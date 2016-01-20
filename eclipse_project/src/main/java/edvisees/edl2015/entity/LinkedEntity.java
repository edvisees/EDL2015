package edvisees.edl2015.entity;

public class LinkedEntity {
    private String fragment;
    private Entity entity;

    public LinkedEntity(String fragment, Entity entity) {
        this.fragment = fragment;
        this.entity = entity;
    }

    public String getFragment() {
        return fragment;
    }

    public Entity getEntity() {
        return entity;
    }
}
