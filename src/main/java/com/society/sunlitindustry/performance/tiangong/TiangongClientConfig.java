package com.society.sunlitindustry.performance.tiangong;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class TiangongClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue DYSON_PRIMITIVES = BUILDER
            .comment("Emit the same Dyson sky primitives without temporary vector/array allocations.")
            .define("dysonPrimitiveAllocation", false);
    private static final ForgeConfigSpec.BooleanValue ARCANE_RINGS = BUILDER
            .comment("Reuse identical adjacent arcane ring endpoints without changing tessellation.")
            .define("arcaneRingTrigReuse", false);
    private static final ForgeConfigSpec.BooleanValue EFFECT_BEZIER = BUILDER
            .comment("Compute effect Bezier points with one result vector instead of three vectors.")
            .define("effectBezierAllocation", false);
    private static final ForgeConfigSpec.BooleanValue DISTANT_EFFECTS = BUILDER
            .comment("Hide optional Tokamak/Industrial/FluidWorks plasma and bubbles beyond the effect distance.")
            .define("distantMachineEffectCulling", false);
    private static final ForgeConfigSpec.IntValue EFFECT_DISTANCE = BUILDER
            .comment("Distance from controller render bounds, in blocks. Machine shells stay visible.")
            .defineInRange("machineEffectDistance", 64, 16, 256);
    private static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean dysonPrimitives() {
        return SPEC.isLoaded() && DYSON_PRIMITIVES.get();
    }

    public static boolean arcaneRings() {
        return SPEC.isLoaded() && ARCANE_RINGS.get();
    }

    public static boolean effectBezier() {
        return SPEC.isLoaded() && EFFECT_BEZIER.get();
    }

    public static boolean distantEffects() {
        return SPEC.isLoaded() && DISTANT_EFFECTS.get();
    }

    public static int effectDistance() {
        return SPEC.isLoaded() ? EFFECT_DISTANCE.get() : 64;
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC,
                "sunlitindustry-tiangong-client.toml");
    }

    private TiangongClientConfig() {}
}
