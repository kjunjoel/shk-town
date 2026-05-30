package kr.shkworld.shktown.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Town {
    private final long id;
    private final UUID uuid;
    private String name;
    private UUID mayorUUID;
    private long nationID;

    private List<UUID> members = new ArrayList<>();

    public Town(long id, UUID uuid, String name, UUID mayorUUID, long nationID) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
        this.mayorUUID = mayorUUID;
        this.nationID = nationID;
    }

    public Town withID(long newID) {
        return new Town(newID, this.uuid, this.name, this.mayorUUID, this.nationID);
    }

    public long getID() { return id; }
    public UUID getUUID() { return uuid; }
    public String getName() { return name; }
    public UUID getMayorUUID() { return mayorUUID; }
    public long getNationID() { return nationID; }
    public List<UUID> getMembers() { return Collections.unmodifiableList(members); }

    public void setName(String name) { this.name = name; }
    public void setMayorUUID(UUID mayorUUID) { this.mayorUUID = mayorUUID; }
    public void setNationID(long nationID) { this.nationID = nationID; }
    public void setMembers(List<UUID> members) { this.members = members; }
}
