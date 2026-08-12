package com.bekvon.bukkit.residence.listenersCache;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerCollisionDenyMessageCache {

    private static final Cache<UUID, Boolean> MESSAGE_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .maximumSize(1000)
            .build();

    private PlayerCollisionDenyMessageCache() {
    }

    public static boolean shouldSendDenyMessage(@NotNull Player player) {
        UUID playerUuid = player.getUniqueId();

        if (MESSAGE_CACHE.getIfPresent(playerUuid) != null) {
            return false;
        }

        MESSAGE_CACHE.put(playerUuid, true);
        return true;
    }

}
