package com.society.sunlitindustry;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class ServerPerformanceConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("GTCEu 7.5.3 dedicated-server optimizations. No recipe, tag or disk resource cache.")
            .define("enabled", true);
    private static final ForgeConfigSpec.BooleanValue EMPTY_IO = BUILDER.define("emptyRecipeIO", true);
    private static final ForgeConfigSpec.BooleanValue SIMULATION = BUILDER.define("fastSimulation", true);
    private static final ForgeConfigSpec.BooleanValue GROUPS = BUILDER.define("cacheHandlerGroups", true);
    private static final ForgeConfigSpec.BooleanValue CONDITIONS = BUILDER.define("fastAndConditions", true);
    private static final ForgeConfigSpec.BooleanValue AE2_DEFAULT_KEY_CACHE = BUILDER
            .define("ae2DefaultItemKeyCache", true);
    private static final ForgeConfigSpec.BooleanValue AE2_DEFAULT_FLUID_KEY_CACHE = BUILDER
            .define("ae2DefaultFluidKeyCache", true);
    private static final ForgeConfigSpec.IntValue MAX_HANDLERS = BUILDER
            .comment("Maximum handler lists per cached topology; larger inventories use the original path.")
            .defineInRange("maxCachedHandlers", 64, 1, 256);
    private static final ForgeConfigSpec SPEC = BUILDER.build();

    private static boolean read(ForgeConfigSpec.BooleanValue value) {
        return !SPEC.isLoaded() || value.get();
    }

    public static boolean enabled() { return read(ENABLED); }
    public static boolean emptyIO() { return enabled() && read(EMPTY_IO); }
    public static boolean simulation() { return enabled() && read(SIMULATION); }
    public static boolean groups() { return enabled() && read(GROUPS); }
    public static boolean conditions() { return enabled() && read(CONDITIONS); }
    public static boolean ae2DefaultKeyCache() { return enabled() && read(AE2_DEFAULT_KEY_CACHE); }
    public static boolean ae2DefaultFluidKeyCache() { return enabled() && read(AE2_DEFAULT_FLUID_KEY_CACHE); }
    public static int maxHandlers() { return SPEC.isLoaded() ? MAX_HANDLERS.get() : 64; }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "sunlitindustry-server-performance.toml");
    }

    private ServerPerformanceConfig() {}
}
