package com.society.sunlitindustry.performance.world;

import com.society.sunlitindustry.ServerPerformanceConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class WorldPerformanceConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue SUNLIGHT = BUILDER
            .comment("Reuse one position/biome query per GT sunlight check; preserve native JAVD behavior.")
            .define("gtSunlightQuery", true);
    private static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean sunlight() {
        return ServerPerformanceConfig.enabled() && (!SPEC.isLoaded() || SUNLIGHT.get());
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC,
                "sunlitindustry-world-performance.toml");
    }

    private WorldPerformanceConfig() {}
}
