package com.society.sunlitindustry.performance.gt;

import com.society.sunlitindustry.ServerPerformanceConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class GtMufflerRecoveryConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue RECOVERY = BUILDER
            .comment("Replace native muffler recovery streams with an ordered loop. RNG and inventory handling stay native.")
            .define("mufflerRecoveryLoop", false);
    private static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enabled() {
        return ServerPerformanceConfig.enabled() && SPEC.isLoaded() && RECOVERY.get();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC,
                "sunlitindustry-gt-muffler.toml");
    }

    private GtMufflerRecoveryConfig() {}
}
