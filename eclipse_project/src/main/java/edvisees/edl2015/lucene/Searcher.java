package edvisees.edl2015.lucene;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Searcher implements LuceneIndexSearcher {
    private IndexSearcher indexSearcher;
    private QueryParser titleQueryParser, idQueryParser;

    public Searcher(String indexDir) throws IOException {
        // open the index directory to search
        Directory indexDirectory = FSDirectory.open(Paths.get(indexDir));
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        this.indexSearcher = new IndexSearcher(indexReader);
        this.titleQueryParser = new QueryParser(IndexItem.TITLE, new StandardAnalyzer(CharArraySet.EMPTY_SET));
        this.titleQueryParser.setDefaultOperator(QueryParser.Operator.AND);
        this.idQueryParser = new QueryParser(IndexItem.ID, new StandardAnalyzer(CharArraySet.EMPTY_SET));
        this.idQueryParser.setDefaultOperator(QueryParser.Operator.AND);
    }

    /**
      * This method is used to find the indexed items by the title.
      * @param queryString - the query string to search for
      */
    public List<IndexItem> findByTitle(String queryString, int numOfResults) throws Exception {
        return this.queryUsingParser(queryString, this.titleQueryParser, numOfResults);
    }

    public IndexItem findByTitleUnique(String queryString) throws Exception {
        return this.queryUsingParserUnique(queryString, this.titleQueryParser);
    }

    public List<IndexItem> findById(String queryString, int numOfResults) throws Exception {
        return this.queryUsingParser(queryString, this.idQueryParser, numOfResults);
    }

    public IndexItem findByIdUnique(String queryString) throws Exception {
        return this.queryUsingParserUnique(queryString, this.idQueryParser);
    }

    private List<IndexItem> queryUsingParser(String queryString, QueryParser queryParser, int numOfResults) throws ParseException, IOException {
        queryString = '"' + QueryParser.escape(queryString) + '"';
        List<IndexItem> results = new ArrayList<IndexItem>();
        if (queryString.trim().length() > 0) {
            Query query = queryParser.parse(queryString);
            ScoreDoc[] queryResults = this.indexSearcher.search(query, numOfResults).scoreDocs;
            for (ScoreDoc scoreDoc : queryResults) {
                Document doc = indexSearcher.doc(scoreDoc.doc);
                results.add(new IndexItem(doc.get(IndexItem.ID), doc.get(IndexItem.TITLE)));
            }
        }
        return results;
    }

    private IndexItem queryUsingParserUnique(String queryString, QueryParser queryParser) throws Exception {
        List<IndexItem> results = this.queryUsingParser(queryString, queryParser, 2); // if more than 1, throw error
        if (results.size() > 1) {
            throw new Exception("More than one result found.");
        }
        if (results.size() == 0) {
            return null;
        }
        return results.get(0);
    }
}
