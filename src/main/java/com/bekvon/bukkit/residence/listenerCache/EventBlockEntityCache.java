package com.bekvon.bukkit.residence.listenerCache;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Intended for high-frequency events to reduce repeated calculations.
 * Example: EntityInteractEvent with physical block interactions.
 */
public final class EventBlockEntityCache {

    private static final Cache<EventBlockEntityKey, Boolean> EVENT_DECISION_CACHE =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(1000L, TimeUnit.MILLISECONDS)
                    .maximumSize(10000)
                    .concurrencyLevel(2)
                    .build();

    private EventBlockEntityCache() {
    }

    public static boolean get(EventBlockEntityKey key, BooleanSupplier decisionLoader) {
        return EVENT_DECISION_CACHE.asMap().computeIfAbsent(key, k -> decisionLoader.getAsBoolean());
    }
}
