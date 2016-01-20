package edvisees.edl2015.entity;

public class Entity {
    private String freebaseID;
    // TODO: entity type enum?

    public Entity(String freebaseID) {
        this.freebaseID = freebaseID;
    }

    public String getId() {
        return this.freebaseID;
    }
}
