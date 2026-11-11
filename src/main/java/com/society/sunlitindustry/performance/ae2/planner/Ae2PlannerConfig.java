package com.society.sunlitindustry.performance.ae2.planner;

import com.society.sunlitindustry.ServerPerformanceConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class Ae2PlannerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER.define("enabled", true);
    private static final ForgeConfigSpec.BooleanValue PRIMARIES = BUILDER
            .comment("Experimental: two-entry initialized-primary shortcut. Previously slower than native in the local probe.")
            .define("hotSimulationPrimaries", false);
    private static final ForgeConfigSpec.BooleanValue COUNTS = BUILDER
            .comment("Accumulate per-pattern craft counts without allocating boxed Long values on every merge.")
            .define("primitiveCraftCounts", true);
    private static final ForgeConfigSpec.BooleanValue GRAPH = BUILDER
            .comment("Reuse tree metadata between planning attempts until lazy child-pattern expansion changes the tree.")
            .define("graphMetadataCache", true);
    private static final ForgeConfigSpec SPEC = BUILDER.build();
    private static volatile Flags flags = new Flags(false, true, true);

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "sunlitindustry-ae2-planner.toml");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(Ae2PlannerConfig::changed);
    }

    private static void changed(ModConfigEvent event) {
        if (event.getConfig().getModId().equals("sunlitindustry")) refresh();
    }

    public static void refresh() {
        boolean enabled = ServerPerformanceConfig.enabled() && read(ENABLED);
        flags = new Flags(enabled && read(PRIMARIES), enabled && read(COUNTS), enabled && read(GRAPH));
    }

    private static boolean read(ForgeConfigSpec.BooleanValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static boolean hotPrimaries() { return ServerPerformanceConfig.enabled() && flags.primaries; }
    public static boolean primitiveCounts() { return ServerPerformanceConfig.enabled() && flags.counts; }
    public static boolean graphMetadata() { return ServerPerformanceConfig.enabled() && flags.graph; }

    private record Flags(boolean primaries, boolean counts, boolean graph) {}
    private Ae2PlannerConfig() {}
}
