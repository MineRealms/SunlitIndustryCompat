package com.society.sunlitindustry.performance.gt;

import com.society.sunlitindustry.ServerPerformanceConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class GtAdvancedConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue RUNNER = BUILDER
            .comment("Reuse bounded RecipeRunner working containers. Chance rolls and handler calls stay native.")
            .define("reuseRecipeRunner", true);
    private static final ForgeConfigSpec.BooleanValue PARALLEL = BUILDER
            .comment("Reuse parallel-calculation scratch maps; inventory amounts are rebuilt for every invocation.")
            .define("reuseParallelScratch", true);
    private static final ForgeConfigSpec.BooleanValue TICK = BUILDER
            .comment("Experimental: formed GT parts without tick subscriptions omit their empty machine ticker.")
            .define("experimentalDormantPartTick", false);
    private static final ForgeConfigSpec.BooleanValue SHAPES = BUILDER
            .comment("Experimental: cache native GT preview shape pages until reload or supplier replacement.")
            .define("experimentalShapeCache", false);
    private static final ForgeConfigSpec.BooleanValue ME = BUILDER
            .comment("Experimental: coalesce GT ME item/fluid hatch flushes at the end of the same server tick.")
            .define("experimentalDeferredMeFlush", false);
    private static final ForgeConfigSpec SPEC = BUILDER.build();

    private static boolean enabled(ForgeConfigSpec.BooleanValue value, boolean defaultValue) {
        return ServerPerformanceConfig.enabled() && (SPEC.isLoaded() ? value.get() : defaultValue);
    }

    public static boolean runners() { return enabled(RUNNER, true); }
    public static boolean parallel() { return enabled(PARALLEL, true); }
    public static boolean dormantPartTick() { return enabled(TICK, false); }
    public static boolean shapes() { return enabled(SHAPES, false); }
    public static boolean deferredMeFlush() { return enabled(ME, false); }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC,
                "sunlitindustry-gt-advanced.toml");
    }

    private GtAdvancedConfig() {}
}
