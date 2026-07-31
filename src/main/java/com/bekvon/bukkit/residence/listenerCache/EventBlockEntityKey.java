package com.bekvon.bukkit.residence.listenerCache;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;

public final class EventBlockEntityKey {

    private final Class<? extends Event> eventType;
    private final UUID world;
    private final int x;
    private final int y;
    private final int z;
    private final Material material;
    private final UUID entityUuid;

    public EventBlockEntityKey(Event event, Block block, Entity entity) {
        this.eventType = event.getClass();
        this.world = block.getWorld().getUID();
        this.x = block.getX();
        this.y = block.getY();
        this.z = block.getZ();
        this.material = block.getType();
        this.entityUuid = entity.getUniqueId();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EventBlockEntityKey)) {
            return false;
        }
        EventBlockEntityKey other = (EventBlockEntityKey) obj;
        return x == other.x
                && y == other.y
                && z == other.z
                && material == other.material
                && eventType == other.eventType
                && world.equals(other.world)
                && entityUuid.equals(other.entityUuid);
    }

    @Override
    public int hashCode() {
        int result = eventType.hashCode();
        result = 31 * result + world.hashCode();
        result = 31 * result + x;
        result = 31 * result + y;
        result = 31 * result + z;
        result = 31 * result + material.hashCode();
        result = 31 * result + entityUuid.hashCode();
        return result;
    }
}
