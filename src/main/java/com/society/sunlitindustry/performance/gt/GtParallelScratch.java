package com.society.sunlitindustry.performance.gt;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2LongOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;

/** Leases are nested rather than shared: an addon may ask for another parallel calculation in a handler. */
public final class GtParallelScratch implements AutoCloseable {
    private static final ThreadLocal<GtScratchPool<GtParallelScratch>> POOL =
            ThreadLocal.withInitial(() -> new GtScratchPool<>(4));
    private static final ThreadLocal<ArrayDeque<GtParallelScratch>> ACTIVE =
            ThreadLocal.withInitial(ArrayDeque::new);
    private final ArrayList<Object2LongOpenHashMap<Object>> plain = new ArrayList<>();
    private final ArrayList<Object2LongOpenCustomHashMap<Object>> custom = new ArrayList<>();
    private final ArrayList<Hash.Strategy<?>> strategies = new ArrayList<>();
    private int plainUsed;
    private int customUsed;

    public static GtParallelScratch open() {
        var scratch = POOL.get().take();
        if (scratch == null) scratch = new GtParallelScratch();
        ACTIVE.get().push(scratch);
        return scratch;
    }

    @SuppressWarnings("unchecked")
    public static <T> Object2LongOpenHashMap<T> plainMap() {
        var scratch = ACTIVE.get().peek();
        if (scratch == null) return new Object2LongOpenHashMap<>();
        int index = scratch.plainUsed++;
        if (index == scratch.plain.size()) scratch.plain.add(new Object2LongOpenHashMap<>());
        return (Object2LongOpenHashMap<T>) (Object) scratch.plain.get(index);
    }

    @SuppressWarnings("unchecked")
    public static <T> Object2LongOpenCustomHashMap<T> customMap(Hash.Strategy<T> strategy) {
        var scratch = ACTIVE.get().peek();
        if (scratch == null) return new Object2LongOpenCustomHashMap<>(strategy);
        int index = scratch.customUsed++;
        if (index == scratch.custom.size()) {
            scratch.custom.add(new Object2LongOpenCustomHashMap<>((Hash.Strategy<Object>) strategy));
            scratch.strategies.add(strategy);
        } else if (scratch.strategies.get(index) != strategy) {
            scratch.custom.set(index, new Object2LongOpenCustomHashMap<>((Hash.Strategy<Object>) strategy));
            scratch.strategies.set(index, strategy);
        }
        return (Object2LongOpenCustomHashMap<T>) (Object) scratch.custom.get(index);
    }

    @Override
    public void close() {
        var active = ACTIVE.get();
        if (active.peek() != this) throw new IllegalStateException("Parallel scratch lease order");
        active.pop();
        boolean bounded = plain.size() <= 64 && custom.size() <= 32;
        for (var map : plain) {
            bounded &= map.size() <= 512;
            map.clear();
        }
        for (var map : custom) {
            bounded &= map.size() <= 512;
            map.clear();
        }
        plainUsed = customUsed = 0;
        if (bounded) POOL.get().giveBack(this);
    }

    public static void clearCurrentThread() {
        POOL.remove();
        if (ACTIVE.get().isEmpty()) ACTIVE.remove();
    }

    private GtParallelScratch() {}
}
