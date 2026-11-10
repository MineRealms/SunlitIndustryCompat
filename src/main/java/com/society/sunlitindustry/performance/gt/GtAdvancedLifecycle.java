package com.society.sunlitindustry.performance.gt;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

/** Called by the central performance lifecycle; no independent event subscriber or worker thread. */
public final class GtAdvancedLifecycle {
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) {
            GtDeferredMeFlush.flush(level);
        }
    }

    public static void levelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) GtDeferredMeFlush.unload(level);
    }

    public static void serverStopped(ServerStoppedEvent event) {
        GtDeferredMeFlush.clear();
        GtShapeCaches.invalidate();
        GtRunnerPool.clearCurrentThread();
        GtParallelScratch.clearCurrentThread();
        GtDormantTickRegistry.wakeAll();
    }

    public static void resourcesReloaded() {
        GtShapeCaches.invalidate();
        GtRunnerPool.clearCurrentThread();
        GtParallelScratch.clearCurrentThread();
        GtDormantTickRegistry.wakeAll();
    }

    private GtAdvancedLifecycle() {}
}
