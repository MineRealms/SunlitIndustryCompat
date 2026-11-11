package com.society.sunlitindustry.performance.tiangong.server;

import com.society.sunlitindustry.ServerPerformanceConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class TianGongServerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER.define("enabled", false);
    private static final ForgeConfigSpec.BooleanValue RECIPE_LISTS = BUILDER
            .comment("Reuse TianGong's immutable RecipeHolder lists within the current server recipe epoch.")
            .define("recipeHolderLists", false);
    private static final ForgeConfigSpec.BooleanValue MATERIAL_GROUPS = BUILDER
            .comment("Preselect material-processing recipes by machine. Ingredient matching remains native.")
            .define("materialMachineGroups", false);
    private static final ForgeConfigSpec.IntValue MAX_TYPES = BUILDER
            .defineInRange("maxCachedRecipeTypes", 64, 1, 256);
    private static final ForgeConfigSpec.IntValue MAX_HOLDERS = BUILDER
            .comment("Maximum retained holders for each cache; larger inputs use the native path.")
            .defineInRange("maxCachedRecipeHolders", 32768, 1, 262144);
    private static final ForgeConfigSpec.IntValue MAX_GROUPS = BUILDER
            .defineInRange("maxMaterialMachineGroups", 16, 1, 64);
    private static final ForgeConfigSpec SPEC = BUILDER.build();
    private static volatile Flags flags = new Flags(false, false, 64, 32768, 16);

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC,
                "sunlitindustry-tiangong-server.toml");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(TianGongServerConfig::configChanged);
        MinecraftForge.EVENT_BUS.addListener(TianGongServerConfig::tagsUpdated);
        MinecraftForge.EVENT_BUS.addListener(TianGongServerConfig::serverStopped);
    }

    private static void configChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) refresh();
        else if (event.getConfig().getModId().equals("sunlitindustry")) TianGongServerRecipeCache.invalidate();
    }

    private static void tagsUpdated(TagsUpdatedEvent event) { TianGongServerRecipeCache.invalidate(); }
    private static void serverStopped(ServerStoppedEvent event) { TianGongServerRecipeCache.invalidate(); }

    public static void refresh() {
        boolean enabled = read(ENABLED);
        flags = new Flags(enabled && read(RECIPE_LISTS), enabled && read(MATERIAL_GROUPS),
                read(MAX_TYPES), read(MAX_HOLDERS), read(MAX_GROUPS));
        TianGongServerRecipeCache.invalidate();
    }

    private static <T> T read(ForgeConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static boolean recipeLists() { return flags.recipeLists && ServerPerformanceConfig.enabled(); }
    public static boolean materialGroups() { return flags.materialGroups && ServerPerformanceConfig.enabled(); }
    public static int maxTypes() { return flags.maxTypes; }
    public static int maxHolders() { return flags.maxHolders; }
    public static int maxGroups() { return flags.maxGroups; }

    private record Flags(boolean recipeLists, boolean materialGroups, int maxTypes, int maxHolders, int maxGroups) {}
    private TianGongServerConfig() {}
}
