package edvisees.edl2015.lucene;

import java.util.List;

public interface LuceneIndexSearcher {
    public List<IndexItem> findByTitle(String queryString, int numOfResults) throws Exception;
}
