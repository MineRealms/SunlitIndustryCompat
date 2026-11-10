package com.society.sunlitindustry.performance.gt;

import net.minecraft.server.level.ServerLevel;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;

/** Only schedules main-thread work. The authoritative output remains in GT's persisted hatch buffer. */
public final class GtDeferredMeFlush {
    private static final IdentityHashMap<ServerLevel, LinkedHashSet<GtDeferredFlushTarget>> PENDING =
            new IdentityHashMap<>();
    private static final int MAX_PENDING_PER_LEVEL = 4096;

    public static boolean enqueue(ServerLevel level, GtDeferredFlushTarget target) {
        if (!level.getServer().isSameThread()) return false;
        var queue = PENDING.computeIfAbsent(level, ignored -> new LinkedHashSet<>());
        if (queue.contains(target)) return true;
        if (queue.size() >= MAX_PENDING_PER_LEVEL) return false;
        queue.add(target);
        return true;
    }

    public static void flush(ServerLevel level) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("ME flush requires server thread");
        var queue = PENDING.remove(level);
        if (queue != null) for (var target : queue) target.sunlitindustry$flushNow();
    }

    public static void unload(ServerLevel level) { PENDING.remove(level); }
    public static void clear() { PENDING.clear(); }
    private GtDeferredMeFlush() {}
}
