package com.society.sunlitindustry.performance;

import com.society.sunlitindustry.ServerPerformanceConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/** Switches for optimizations which predate the modular performance package. */
public final class LegacyPerformanceConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue WORLD_EDIT = BUILDER
            .comment("Share large WorldEdit state indexes. Restart after changing: existing states keep their representation.")
            .worldRestart().define("worldEditStateIndex", false);
    private static final ForgeConfigSpec.BooleanValue KUBEJS = BUILDER
            .comment("Return KubeJS's existing original recipe before it constructs a diagnostic Throwable.")
            .define("kubejsOriginalRecipeFastPath", true);
    private static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean worldEdit() {
        return ServerPerformanceConfig.enabled() && SPEC.isLoaded() && WORLD_EDIT.get();
    }

    public static boolean kubejs() {
        return ServerPerformanceConfig.enabled() && (!SPEC.isLoaded() || KUBEJS.get());
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC,
                "sunlitindustry-legacy-performance.toml");
    }

    private LegacyPerformanceConfig() {}
}
