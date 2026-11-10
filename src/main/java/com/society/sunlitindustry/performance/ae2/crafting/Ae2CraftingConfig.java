package com.society.sunlitindustry.performance.ae2.crafting;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class Ae2CraftingConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER.define("enabled", true);
    private static final ForgeConfigSpec.BooleanValue PATTERNS = BUILDER.define("decodedPatternCache", true);
    private static final ForgeConfigSpec.BooleanValue SORTING = BUILDER.define("craftingProviderSortCache", true);
    private static final ForgeConfigSpec.IntValue MAX_PATTERNS = BUILDER
            .comment("Maximum successfully decoded native AE2 patterns retained by each server level.")
            .defineInRange("maxDecodedPatternsPerLevel", 2048, 1, 16384);
    private static final ForgeConfigSpec SPEC = BUILDER.build();

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC,
                "sunlitindustry-ae2-crafting.toml");
    }

    private static boolean read(ForgeConfigSpec.BooleanValue value) {
        return !SPEC.isLoaded() || value.get();
    }

    private static boolean enabled() {
        return !Boolean.getBoolean("sunlitindustry.disableServerOptimizations") && read(ENABLED);
    }

    public static boolean patterns() { return enabled() && read(PATTERNS); }
    public static boolean sorting() { return enabled() && read(SORTING); }
    public static int maxPatterns() { return SPEC.isLoaded() ? MAX_PATTERNS.get() : 2048; }

    private Ae2CraftingConfig() {}
}
