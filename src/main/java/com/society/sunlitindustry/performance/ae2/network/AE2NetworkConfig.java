package com.society.sunlitindustry.performance.ae2.network;

import com.society.sunlitindustry.ServerPerformanceConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/** Previously regressing candidates kept for explicit workload experiments only. */
public final class AE2NetworkConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue ROUTING = BUILDER
            .comment("Experimental priority routing snapshots. Previous 256-store benchmark was slower than native.")
            .define("experimentalPriorityRouting", false);
    private static final ForgeConfigSpec.BooleanValue ADJACENCY = BUILDER
            .comment("Experimental positive LazyOptional cache. Previous chest benchmark was slower than native.")
            .define("experimentalAdjacentCapabilityCache", false);
    private static final ForgeConfigSpec.BooleanValue CELL_ACCOUNTING = BUILDER
            .comment("Experimental primitive cell accounting. Previous benchmark had no allocation benefit.")
            .define("experimentalPrimitiveCellAccounting", false);
    private static final ForgeConfigSpec SPEC = BUILDER.build();

    private static boolean enabled(ForgeConfigSpec.BooleanValue option) {
        return ServerPerformanceConfig.enabled() && SPEC.isLoaded() && option.get();
    }

    public static boolean routing() { return enabled(ROUTING); }
    public static boolean adjacency() { return enabled(ADJACENCY); }
    public static boolean cellAccounting() { return enabled(CELL_ACCOUNTING); }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC,
                "sunlitindustry-ae2-network-experiments.toml");
    }

    private AE2NetworkConfig() {}
}
