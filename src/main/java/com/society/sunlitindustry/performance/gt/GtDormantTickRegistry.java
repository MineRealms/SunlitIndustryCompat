package com.society.sunlitindustry.performance.gt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class GtDormantTickRegistry {
    private static final Set<GtDormantPart> DORMANT = Collections.newSetFromMap(new WeakHashMap<>());

    public static void add(GtDormantPart part) { DORMANT.add(part); }
    public static void remove(GtDormantPart part) { DORMANT.remove(part); }

    public static void wakeAll() {
        var snapshot = new ArrayList<>(DORMANT);
        DORMANT.clear();
        snapshot.forEach(GtDormantPart::sunlitindustry$wakeTicker);
    }

    private GtDormantTickRegistry() {}
}
