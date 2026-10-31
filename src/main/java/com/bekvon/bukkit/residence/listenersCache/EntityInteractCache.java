package com.bekvon.bukkit.residence.listenersCache;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/*
Preventing Entity pressure-plate contact triggers 20 events/sec (1 per tick)
Using cache is more lightweight
*/
public class EntityInteractCache {

    private static final Cache<EntityInteractKey, Boolean> ENTITY_INTERACT_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .build();

    private EntityInteractCache() {
    }

    public static boolean getOrCompute(EntityInteractKey key, BooleanSupplier loader) {
        try {
            // On cache miss or expiry, compute via loader and store the result
            return ENTITY_INTERACT_CACHE.get(key, loader::getAsBoolean);
        } catch (ExecutionException e) {
            return true;
        }
    }

    public static final class EntityInteractKey {
        private final UUID worldUuid;
        private final int x, y, z;

        public EntityInteractKey(@NotNull Block block) {
            this.worldUuid = block.getWorld().getUID();
            this.x = block.getX();
            this.y = block.getY();
            this.z = block.getZ();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof EntityInteractKey)) {
                return false;
            }
            EntityInteractKey other = (EntityInteractKey) obj;
            return worldUuid.equals(other.worldUuid)
                    && x == other.x
                    && y == other.y
                    && z == other.z;
        }

        @Override
        public int hashCode() {
            int result = worldUuid.hashCode();
            result = 31 * result + x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }
    }
}
