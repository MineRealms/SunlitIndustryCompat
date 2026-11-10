package com.society.sunlitindustry.performance.gt;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;

public final class GtShapeCaches {
    private static final ArrayDeque<WeakReference<GtShapeCacheOwner>> OWNERS = new ArrayDeque<>();

    public static synchronized void track(GtShapeCacheOwner owner) {
        OWNERS.removeIf(reference -> reference.get() == null || reference.get() == owner);
        OWNERS.addLast(new WeakReference<>(owner));
        while (OWNERS.size() > 16) {
            var evicted = OWNERS.removeFirst().get();
            if (evicted != null) evicted.sunlitindustry$clearShapes();
        }
    }
    public static synchronized void invalidate() {
        for (var reference : OWNERS) {
            var owner = reference.get();
            if (owner != null) owner.sunlitindustry$clearShapes();
        }
        OWNERS.clear();
    }

    private GtShapeCaches() {}
}
