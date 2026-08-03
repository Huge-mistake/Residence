package com.bekvon.bukkit.residence.listenerCache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Use case: when the 'EntityInteractEvent' and the
 * 'PlayerInteractEvent-Action.PHYSICAL' are cancelled.
 * Cancelled events can fire up to 20 times per 20 ticks; this cache avoids repeated checks.
 * Allowed events are not cached (low-frequency, typically once).
 */
public class EventBlockEntityCache {

    private static final Cache<EventBlockEntityKey, Boolean> EVENT_BLOCK_ENTITY_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(3, TimeUnit.SECONDS)
            .maximumSize(10000)
            .concurrencyLevel(2)
            .build();

    private EventBlockEntityCache() {
    }

    public static boolean isDenied(EventBlockEntityKey key) {
        return Boolean.TRUE.equals(EVENT_BLOCK_ENTITY_CACHE.getIfPresent(key));
    }

    public static void putDenied(EventBlockEntityKey key) {
        EVENT_BLOCK_ENTITY_CACHE.put(key, true);
    }

    public static final class EventBlockEntityKey {

        private final Class<? extends Event> eventType;
        private final UUID world;
        private final int x;
        private final int y;
        private final int z;
        private final Material material;
        private final UUID entityUuid;
        private final int hash;

        public EventBlockEntityKey(@NotNull Event event, @NotNull Block block, @NotNull Entity entity) {
            this.eventType = event.getClass();
            this.world = block.getWorld().getUID();
            this.x = block.getX();
            this.y = block.getY();
            this.z = block.getZ();
            this.material = block.getType();
            this.entityUuid = entity.getUniqueId();

            int result = eventType.hashCode();
            result = 31 * result + world.hashCode();
            result = 31 * result + x;
            result = 31 * result + y;
            result = 31 * result + z;
            result = 31 * result + material.hashCode();
            result = 31 * result + entityUuid.hashCode();
            this.hash = result;
        }

        @Override
        public int hashCode() {
            return this.hash;
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
            if (this.hash != other.hash) {
                return false;
            }
            if (this.x != other.x) return false;
            if (this.y != other.y) return false;
            if (this.z != other.z) return false;
            if (this.material != other.material) return false;
            if (this.eventType != other.eventType) return false;
            if (!this.world.equals(other.world)) return false;

            return this.entityUuid.equals(other.entityUuid);

//            return x == other.x
//                    && y == other.y
//                    && z == other.z
//                    && material == other.material
//                    && eventType == other.eventType
//                    && world.equals(other.world)
//                    && entityUuid.equals(other.entityUuid);
        }

//        @Override
//        public int hashCode() {
//            int result = eventType.hashCode();
//            result = 31 * result + world.hashCode();
//            result = 31 * result + x;
//            result = 31 * result + y;
//            result = 31 * result + z;
//            result = 31 * result + material.hashCode();
//            result = 31 * result + entityUuid.hashCode();
//            return result;
//        }
    }
}
