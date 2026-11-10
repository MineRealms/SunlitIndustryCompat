package com.society.sunlitindustry.performance.ae2.network;

/** Per-thread direct-mapped cache of AE2's immutable integer range bounds. */
public final class FuzzyBoundCache {
    private static final int CAPACITY = 128;
    private static final ThreadLocal<FuzzyBoundCache> LOCAL = ThreadLocal.withInitial(FuzzyBoundCache::new);
    private final int[] damages = new int[CAPACITY];
    private final Object[] bounds = new Object[CAPACITY];

    public static FuzzyBoundCache local() { return LOCAL.get(); }

    private static int slot(int damage) {
        int mixed = damage ^ (damage >>> 16);
        return (mixed * 0x9e3779b9) >>> 25;
    }

    public Object get(int damage) {
        int slot = slot(damage);
        return damages[slot] == damage ? bounds[slot] : null;
    }

    public void put(int damage, Object bound) {
        int slot = slot(damage);
        damages[slot] = damage;
        bounds[slot] = bound;
    }

    public static int lower(int max, int value, int breakpoint, boolean ignoreAll) {
        return ignoreAll || value > breakpoint ? max : breakpoint;
    }

    public static int upper(int value, int breakpoint, boolean ignoreAll) {
        return ignoreAll || value <= breakpoint ? -1 : breakpoint;
    }
}
