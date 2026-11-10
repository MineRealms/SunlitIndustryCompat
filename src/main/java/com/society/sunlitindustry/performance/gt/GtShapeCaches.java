package com.society.sunlitindustry.performance.gt;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class GtShapeCaches {
    private static final Set<GtShapeCacheOwner> OWNERS = Collections.newSetFromMap(new WeakHashMap<>());

    public static synchronized void track(GtShapeCacheOwner owner) { OWNERS.add(owner); }
    public static synchronized void invalidate() {
        OWNERS.forEach(GtShapeCacheOwner::sunlitindustry$clearShapes);
        OWNERS.clear();
    }

    private GtShapeCaches() {}
}
