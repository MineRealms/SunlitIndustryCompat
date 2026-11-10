package com.society.sunlitindustry;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class ServerPerformanceConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("GTCEu 7.5.3 and AE2 15.4.10 dedicated-server runtime optimizations.")
            .define("enabled", true);
    private static final ForgeConfigSpec.BooleanValue EMPTY_IO = BUILDER.define("emptyRecipeIO", true);
    private static final ForgeConfigSpec.BooleanValue SIMULATION = BUILDER.define("fastSimulation", true);
    private static final ForgeConfigSpec.BooleanValue GROUPS = BUILDER.define("cacheHandlerGroups", true);
    private static final ForgeConfigSpec.BooleanValue CONDITIONS = BUILDER.define("fastAndConditions", true);
    private static final ForgeConfigSpec.BooleanValue AE2_DEFAULT_KEY_CACHE = BUILDER
            .define("ae2DefaultItemKeyCache", true);
    private static final ForgeConfigSpec.BooleanValue AE2_DEFAULT_FLUID_KEY_CACHE = BUILDER
            .define("ae2DefaultFluidKeyCache", true);
    private static final ForgeConfigSpec.BooleanValue AE2_NETWORK = BUILDER.define("ae2NetworkRouting", true);
    private static final ForgeConfigSpec.BooleanValue AE2_FUZZY = BUILDER.define("ae2FuzzyBounds", true);
    private static final ForgeConfigSpec.BooleanValue AE2_CELL_ACCOUNTING = BUILDER.define("ae2CellAccounting", true);
    private static final ForgeConfigSpec.BooleanValue AE2_COMPACT_CELL = BUILDER
            .comment("Opt-in compressed in-item cell keys. Requires this mod on both client and server.")
            .define("ae2CompactCellNbt", false);
    private static final ForgeConfigSpec.IntValue MAX_HANDLERS = BUILDER
            .comment("Maximum handler lists per cached topology; larger inventories use the original path.")
            .defineInRange("maxCachedHandlers", 64, 1, 256);
    private static final ForgeConfigSpec SPEC = BUILDER.build();

    private static boolean read(ForgeConfigSpec.BooleanValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static boolean enabled() {
        return !Boolean.getBoolean("sunlitindustry.disableServerOptimizations") && read(ENABLED);
    }
    public static boolean emptyIO() { return enabled() && read(EMPTY_IO); }
    public static boolean simulation() { return enabled() && read(SIMULATION); }
    public static boolean groups() { return enabled() && read(GROUPS); }
    public static boolean conditions() { return enabled() && read(CONDITIONS); }
    public static boolean ae2DefaultKeyCache() { return enabled() && read(AE2_DEFAULT_KEY_CACHE); }
    public static boolean ae2DefaultFluidKeyCache() { return enabled() && read(AE2_DEFAULT_FLUID_KEY_CACHE); }
    public static boolean ae2NetworkRouting() { return enabled() && read(AE2_NETWORK); }
    public static boolean ae2FuzzyBounds() { return enabled() && read(AE2_FUZZY); }
    public static boolean ae2CellAccounting() { return enabled() && read(AE2_CELL_ACCOUNTING); }
    public static boolean ae2CompactCellNbt() { return enabled() && read(AE2_COMPACT_CELL); }
    public static int maxHandlers() { return SPEC.isLoaded() ? MAX_HANDLERS.get() : 64; }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "sunlitindustry-server-performance.toml");
    }

    private ServerPerformanceConfig() {}
}
