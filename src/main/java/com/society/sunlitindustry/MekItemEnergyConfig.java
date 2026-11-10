package com.society.sunlitindustry;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class MekItemEnergyConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Expose GT electric-item capabilities over the original Mekanism energy containers.")
            .define("enabled", true);
    private static final ForgeConfigSpec.IntValue TABLET = tier("energyTabletTier", 1);
    private static final ForgeConfigSpec.IntValue CUBE_BASE = tier("basicEnergyCubeTier", 1);
    private static final ForgeConfigSpec.IntValue TOOL = tier("energizedToolTier", 3);
    private static final ForgeConfigSpec.IntValue SUIT = tier("mekaSuitTier", 5);
    private static final ForgeConfigSpec.IntValue VOID_SUIT = tier("voidMekaSuitTier", 9);
    private static final ForgeConfigSpec SPEC = BUILDER.build();

    private static ForgeConfigSpec.IntValue tier(String name, int value) {
        return BUILDER.comment("Minimum GT charger tier: ULV=0, LV=1, MV=2, HV=3, IV=5, UHV=9. Restart to apply.")
                .defineInRange(name, value, 0, 14);
    }

    private static int read(ForgeConfigSpec.IntValue value, int fallback) {
        return SPEC.isLoaded() ? value.get() : fallback;
    }

    public static boolean enabled() { return !SPEC.isLoaded() || ENABLED.get(); }
    public static int tabletTier() { return read(TABLET, 1); }
    public static int cubeTier(int ordinal) { return Math.min(14, read(CUBE_BASE, 1) + ordinal); }
    public static int toolTier() { return read(TOOL, 3); }
    public static int suitTier() { return read(SUIT, 5); }
    public static int voidSuitTier() { return read(VOID_SUIT, 9); }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "sunlitindustry-item-energy.toml");
    }

    private MekItemEnergyConfig() {}
}
