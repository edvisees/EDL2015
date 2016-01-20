package edvisees.edl2015.lucene;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Indexer {
    private IndexWriter indexWriter;

    public Indexer(String indexDir) throws IOException {
        Directory indexDirectory = FSDirectory.open(Paths.get(indexDir));
        try {
            Analyzer standardIndexAnalyzer = new StandardAnalyzer(CharArraySet.EMPTY_SET);
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(standardIndexAnalyzer);
            indexWriterConfig.setOpenMode(OpenMode.CREATE_OR_APPEND);
            this.indexWriter = new IndexWriter(indexDirectory, indexWriterConfig);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }

    /**
     * add the item to the index
     * @param indexItem
     * @throws IOException
     */
    public void index(IndexItem indexItem) throws IOException {
        Document doc = new Document();
        doc.add(new TextField(IndexItem.ID, indexItem.getId(), Field.Store.YES));
        doc.add(new TextField(IndexItem.TITLE, indexItem.getTitle(), Field.Store.YES));
        this.indexWriter.addDocument(doc);
    }

    public void close() throws IOException {
        this.indexWriter.close();
    }
}
