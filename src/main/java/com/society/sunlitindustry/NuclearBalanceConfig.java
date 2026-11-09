package com.society.sunlitindustry;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class NuclearBalanceConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.DoubleValue FUSION_BURN_CAP;
    private static final ForgeConfigSpec.IntValue FUSION_INJECTION_CAP;
    private static final ForgeConfigSpec.DoubleValue FISSION_BURN_CAP;
    private static final ForgeConfigSpec.BooleanValue PREVENT_DATA_CARRIER_DOWNGRADE;
    private static final ForgeConfigSpec.BooleanValue REQUIRE_WANXIANG_CORE;
    private static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("mekanism_generators");
        ENABLED = BUILDER.define("enableNuclearRateLimits", true);
        FUSION_BURN_CAP = BUILDER
              .comment("Maximum fusion fuel burned per reactor tick, including direct gas and Hohlraum input.")
              .defineInRange("maxFusionFuelBurnPerTick", 8.0D, 0.001D, 1000D);
        FUSION_INJECTION_CAP = BUILDER
              .comment("Maximum GUI/computer deuterium-tritium injection rate; the effective value is rounded down to an even number.")
              .defineInRange("maxFusionInjectionRate", 2, 0, 98);
        FISSION_BURN_CAP = BUILDER
              .comment("Maximum fission fuel burned per reactor tick, regardless of assembly count or saved rate limit.")
              .defineInRange("maxFissionBurnPerTick", 1.0D, 0.001D, 1_000_000D);
        BUILDER.pop();
        BUILDER.push("gregtech");
        PREVENT_DATA_CARRIER_DOWNGRADE = BUILDER
              .comment("Prevent copying Data Bank-only research modules into data orbs/sticks. Same-module copies and upgrades remain allowed.")
              .define("preventDataCarrierDowngrade", true);
        BUILDER.pop();
        BUILDER.push("tiangong");
        REQUIRE_WANXIANG_CORE = BUILDER
              .comment("Require one GT-assembled society:wanxiang_formation_core for each successful new Wanxiang Cage formation. Existing formed cages are unchanged.")
              .define("requireWanxiangFormationCore", true);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "sunlitindustry-common.toml");
    }

    public static int capFusionInjectionRate(int requested) {
        if (!ENABLED.get()) {
            return requested;
        }
        int maximum = FUSION_INJECTION_CAP.get();
        maximum -= maximum % 2;
        int bounded = Math.max(0, Math.min(requested, maximum));
        return bounded - bounded % 2;
    }

    public static boolean preventDataCarrierDowngrade() {
        return PREVENT_DATA_CARRIER_DOWNGRADE.get();
    }

    public static boolean requireWanxiangCore() {
        return REQUIRE_WANXIANG_CORE.get();
    }

    public static double capFusionBurn(double requested) {
        if (Double.isNaN(requested) || requested <= 0) {
            return 0;
        }
        if (!ENABLED.get()) {
            return requested;
        }
        return Math.min(requested, FUSION_BURN_CAP.get());
    }

    public static double capFissionBurn(double requested) {
        if (Double.isNaN(requested) || requested <= 0) {
            return 0;
        }
        if (!ENABLED.get()) {
            return requested;
        }
        return Math.min(requested, FISSION_BURN_CAP.get());
    }

    public static long maxFissionBurnRate(long nativeMaximum) {
        if (!ENABLED.get()) {
            return nativeMaximum;
        }
        long configuredMaximum = Math.max(1, (long) Math.ceil(FISSION_BURN_CAP.get()));
        return Math.min(nativeMaximum, configuredMaximum);
    }

    public static double capFissionRate(double requested) {
        if (Double.isNaN(requested) || requested <= 0) {
            return 0;
        }
        if (!ENABLED.get()) {
            return requested;
        }
        return Math.min(requested, FISSION_BURN_CAP.get());
    }

    private NuclearBalanceConfig() {}
}
