package com.society.sunlitindustry.performance.gt;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;

/** Only schedules main-thread work. The authoritative output remains in GT's persisted hatch buffer. */
public final class GtDeferredMeFlush {
    private static final Logger LOGGER = LogUtils.getLogger();
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
        if (queue != null) flushTargets(queue);
    }

    static void flushTargets(Iterable<? extends GtDeferredFlushTarget> targets) {
        for (var target : targets) {
            try {
                target.sunlitindustry$flushNow();
            } catch (RuntimeException failure) {
                LOGGER.error("Sunlit deferred ME flush failed for {}; buffered output remains with the hatch",
                        target.getClass().getName(), failure);
            }
        }
    }

    public static void unload(ServerLevel level) { PENDING.remove(level); }
    public static void clear() { PENDING.clear(); }
    private GtDeferredMeFlush() {}
}
