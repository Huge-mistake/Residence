package com.bekvon.bukkit.residence.listenerCache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class EntityInteractCache {

    private static final Cache<BlockKey, Boolean> ENTITY_INTERACT_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(3, TimeUnit.SECONDS)
            .maximumSize(10000)
            .concurrencyLevel(2)
            .build();

    private EntityInteractCache() {
    }

    public static boolean isDenied(BlockKey key) {
        return Boolean.TRUE.equals(ENTITY_INTERACT_CACHE.getIfPresent(key));
    }

    public static void putDenied(BlockKey key) {
        ENTITY_INTERACT_CACHE.put(key, true);
    }

    public static final class BlockKey {

        private final UUID world;
        private final int x;
        private final int y;
        private final int z;
        private final Material material;

        public BlockKey(Block block) {
            this.world = block.getWorld().getUID();
            this.x = block.getX();
            this.y = block.getY();
            this.z = block.getZ();
            this.material = block.getType();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BlockKey)) {
                return false;
            }
            BlockKey other = (BlockKey) obj;
            return x == other.x
                    && y == other.y
                    && z == other.z
                    && world.equals(other.world)
                    && material == other.material;
        }

        @Override
        public int hashCode() {
            int result = world.hashCode();
            result = 31 * result + x;
            result = 31 * result + y;
            result = 31 * result + z;
            result = 31 * result + material.hashCode();
            return result;
        }
    }
}
