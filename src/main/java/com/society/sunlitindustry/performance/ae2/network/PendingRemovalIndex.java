package com.society.sunlitindustry.performance.ae2.network;

import java.util.IdentityHashMap;

/** Mirrors AE2's pending unmount queue without retaining a second operation list. */
public final class PendingRemovalIndex<T> {
    private IdentityHashMap<T, Boolean> removals;

    public void add(T inventory) {
        if (removals == null) removals = new IdentityHashMap<>();
        removals.put(inventory, Boolean.TRUE);
    }

    public boolean contains(T inventory) {
        return removals != null && removals.containsKey(inventory);
    }

    public void clear() {
        if (removals != null) removals.clear();
    }
}
