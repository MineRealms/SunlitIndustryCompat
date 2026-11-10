package com.society.sunlitindustry.performance.gt;

import java.util.ArrayDeque;

/** A borrowed value is absent from the pool, so recursive handlers receive a different workspace. */
public final class GtScratchPool<T> {
    private final ArrayDeque<T> available = new ArrayDeque<>();
    private final int capacity;

    public GtScratchPool(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("Negative capacity");
        this.capacity = capacity;
    }

    public T take() { return available.pollFirst(); }

    public void giveBack(T value) {
        if (available.size() < capacity) available.addFirst(value);
    }

    public int size() { return available.size(); }
    public void clear() { available.clear(); }
}
