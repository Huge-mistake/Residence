package com.bekvon.bukkit.residence.listenerCache;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Intended for high-frequency events to reduce repeated calculations.
 * Example: EntityInteractEvent with physical block interactions.
 */
public final class EventBlockEntityCache {

    private static final Cache<EventBlockEntityKey, Boolean> EVENT_DECISION_CACHE = Caffeine.newBuilder()
            .expireAfterWrite(1000, TimeUnit.MILLISECONDS)
            .maximumSize(10000)
            .build();

    private EventBlockEntityCache() {
    }

    public static boolean get(EventBlockEntityKey key,  BooleanSupplier decisionLoader) {
        // Loader runs only on cache miss and caches the decision
        return Boolean.TRUE.equals(EVENT_DECISION_CACHE.get(key, k -> decisionLoader.getAsBoolean()));
    }
}
