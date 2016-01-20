package edvisees.edl2015.lucene;

public class IndexItem {
    private String nodeId;
    private String title;

    public static final String ID = "id";
    public static final String TITLE = "title";

    public IndexItem(String nodeId, String title) {
        this.nodeId = nodeId;
        this.title = title;
    }

    public String getId() {
        return this.nodeId;
    }

    public String getTitle() {
        return this.title;
    }

    @Override
    public String toString() {
        return "IndexItem{id=" + this.nodeId + ", title='" + this.title + "'}";
    }
}
