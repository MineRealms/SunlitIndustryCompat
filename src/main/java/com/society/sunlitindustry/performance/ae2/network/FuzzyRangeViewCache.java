package com.society.sunlitindustry.performance.ae2.network;

import java.util.Arrays;

/** Caches live native submap views, so map mutations need no result invalidation. */
public final class FuzzyRangeViewCache<V> {
    private static final int CAPACITY = 4;
    private final int[] lower = new int[CAPACITY];
    private final int[] upper = new int[CAPACITY];
    private final Object[] views = new Object[CAPACITY];
    private Object owner;
    private int next;

    @SuppressWarnings("unchecked")
    public V get(Object map, int low, int high) {
        if (owner != map) {
            Arrays.fill(views, null);
            owner = map;
            next = 0;
            return null;
        }
        for (int slot = 0; slot < CAPACITY; slot++) {
            if (views[slot] != null && lower[slot] == low && upper[slot] == high) return (V) views[slot];
        }
        return null;
    }

    public void put(Object map, int low, int high, V view) {
        if (owner != map) get(map, low, high);
        for (int slot = 0; slot < CAPACITY; slot++) {
            if (views[slot] != null && lower[slot] == low && upper[slot] == high) {
                views[slot] = view;
                return;
            }
        }
        lower[next] = low;
        upper[next] = high;
        views[next] = view;
        next = (next + 1) % CAPACITY;
    }
}
