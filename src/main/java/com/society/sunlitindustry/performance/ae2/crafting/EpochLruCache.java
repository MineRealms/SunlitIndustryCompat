package com.society.sunlitindustry.performance.ae2.crafting;

import java.util.LinkedHashMap;
import java.util.function.Supplier;

/** An owner-local positive cache. Epoch and source identity are both part of its validity. */
public final class EpochLruCache<K, V> {
    private final LinkedHashMap<K, V> entries = new LinkedHashMap<>(16, 0.75f, true);
    private long epoch = Long.MIN_VALUE;
    private Object source;

    public synchronized V get(K key, long generation, Object identity, int limit, Supplier<V> loader) {
        if (limit < 1) throw new IllegalArgumentException("Cache limit must be positive");
        if (generation != epoch || identity != source) {
            entries.clear();
            epoch = generation;
            source = identity;
        }
        trim(limit);
        V existing = entries.get(key);
        if (existing != null) return existing;
        V loaded = loader.get();
        if (loaded != null) {
            entries.put(key, loaded);
            trim(limit);
        }
        return loaded;
    }

    private void trim(int limit) {
        while (entries.size() > limit) {
            var iterator = entries.entrySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    public synchronized void clear() {
        entries.clear();
        source = null;
        epoch = Long.MIN_VALUE;
    }

    public synchronized int size() { return entries.size(); }
}
