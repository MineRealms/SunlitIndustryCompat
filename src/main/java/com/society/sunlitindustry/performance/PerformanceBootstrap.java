package com.society.sunlitindustry.performance;

import com.society.sunlitindustry.ServerPerformanceConfig;
import com.society.sunlitindustry.performance.ae2.crafting.Ae2CraftingConfig;
import com.society.sunlitindustry.performance.ae2.crafting.PatternDecodeCache;
import com.society.sunlitindustry.performance.ae2.planner.Ae2PlannerConfig;
import com.society.sunlitindustry.performance.gt.GtAdvancedConfig;
import com.society.sunlitindustry.performance.gt.GtAdvancedLifecycle;
import com.society.sunlitindustry.performance.resources.ResourceCacheConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Module registration and lifecycle wiring stay separate from the compatibility mod entry point. */
public final class PerformanceBootstrap {
    public static void register() {
        ServerPerformanceConfig.register();
        GtAdvancedConfig.register();
        Ae2CraftingConfig.register();
        Ae2PlannerConfig.register();
        ResourceCacheConfig.register();
        if (FMLLoader.getDist().isDedicatedServer() && ModList.get().isLoaded("ae2")) {
            PatternDecodeCache.registerEvents();
        }
        if (FMLLoader.getDist().isDedicatedServer() && ModList.get().isLoaded("gtceu")) {
            var bus = MinecraftForge.EVENT_BUS;
            bus.addListener(GtAdvancedLifecycle::levelTick);
            bus.addListener(GtAdvancedLifecycle::levelUnload);
            bus.addListener(GtAdvancedLifecycle::serverStopped);
            bus.addListener(PerformanceBootstrap::tagsUpdated);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(PerformanceBootstrap::configReloaded);
        }
    }

    private static void tagsUpdated(TagsUpdatedEvent event) { invalidateGtOnServerThread(); }

    private static void configReloaded(ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals("sunlitindustry")) invalidateGtOnServerThread();
    }

    private static void invalidateGtOnServerThread() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) server.execute(GtAdvancedLifecycle::resourcesReloaded);
    }

    private PerformanceBootstrap() {}
}
