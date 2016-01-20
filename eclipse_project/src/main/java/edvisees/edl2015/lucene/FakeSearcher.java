package edvisees.edl2015.lucene;

import java.util.List;

public class FakeSearcher implements LuceneIndexSearcher {
    @Override
    public List<IndexItem> findByTitle(String queryString, int numOfResults) throws Exception {
        return null;
    }
}
