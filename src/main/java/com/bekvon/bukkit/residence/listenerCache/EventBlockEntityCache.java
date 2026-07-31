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
                    .expireAfterWrite(1, TimeUnit.SECONDS)
                    .maximumSize(10000)
                    .concurrencyLevel(2)
                    .build();

    private EventBlockEntityCache() {
    }

    public static boolean get(EventBlockEntityKey key, BooleanSupplier loader) {
        Boolean cached = EVENT_DECISION_CACHE.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        boolean result = loader.getAsBoolean();
        EVENT_DECISION_CACHE.put(key, result);
        return result;
    }
}
