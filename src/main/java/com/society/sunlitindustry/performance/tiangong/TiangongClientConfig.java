package com.society.sunlitindustry.performance.tiangong;

import com.society.sunlitindustry.ServerPerformanceConfig;
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
    private static final ForgeConfigSpec.BooleanValue TOKAMAK_PLASMA_LOD = BUILDER
            .comment("Reduce only Tokamak core/plasma/halo mesh tessellation at a distance.")
            .define("tokamakPlasmaLod", false);
    private static final ForgeConfigSpec.IntValue TOKAMAK_PLASMA_MID_DISTANCE = BUILDER
            .comment("Nearest render-bounds distance for medium plasma detail, in blocks.")
            .defineInRange("tokamakPlasmaMidDistance", 48, 16, 256);
    private static final ForgeConfigSpec.IntValue TOKAMAK_PLASMA_FAR_DISTANCE = BUILDER
            .comment("Nearest render-bounds distance for low plasma detail; kept above the medium distance.")
            .defineInRange("tokamakPlasmaFarDistance", 96, 32, 512);
    private static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean dysonPrimitives() {
        return ServerPerformanceConfig.enabled() && SPEC.isLoaded() && DYSON_PRIMITIVES.get();
    }

    public static boolean arcaneRings() {
        return ServerPerformanceConfig.enabled() && SPEC.isLoaded() && ARCANE_RINGS.get();
    }

    public static boolean effectBezier() {
        return ServerPerformanceConfig.enabled() && SPEC.isLoaded() && EFFECT_BEZIER.get();
    }

    public static boolean distantEffects() {
        return ServerPerformanceConfig.enabled() && SPEC.isLoaded() && DISTANT_EFFECTS.get();
    }

    public static int effectDistance() {
        return SPEC.isLoaded() ? EFFECT_DISTANCE.get() : 64;
    }

    public static boolean tokamakPlasmaLod() {
        return ServerPerformanceConfig.enabled() && SPEC.isLoaded() && TOKAMAK_PLASMA_LOD.get();
    }

    public static int tokamakPlasmaMidDistance() {
        return SPEC.isLoaded() ? TOKAMAK_PLASMA_MID_DISTANCE.get() : 48;
    }

    public static int tokamakPlasmaFarDistance() {
        int configured = SPEC.isLoaded() ? TOKAMAK_PLASMA_FAR_DISTANCE.get() : 96;
        return Math.max(tokamakPlasmaMidDistance() + 1, configured);
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC,
                "sunlitindustry-tiangong-client.toml");
    }

    private TiangongClientConfig() {}
}
