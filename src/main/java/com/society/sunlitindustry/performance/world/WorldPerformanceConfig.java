package com.society.sunlitindustry.performance.world;

import com.society.sunlitindustry.ServerPerformanceConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class WorldPerformanceConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue SUNLIGHT = BUILDER
            .comment("Reuse one position/biome query per GT sunlight check; preserve native JAVD behavior.")
            .define("gtSunlightQuery", false);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DIMENSIONS = BUILDER
            .comment("Dimensions using the optimized query. An empty list matches none; * matches every dimension.")
            .defineListAllowEmpty("sunlightDimensions", List.of("minecraft:overworld"),
                    value -> value instanceof String id && (id.equals("*") || ResourceLocation.tryParse(id) != null));
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED = BUILDER
            .comment("Dimensions always using native GT logic, even if sunlightDimensions contains *.")
            .defineListAllowEmpty("sunlightExcludedDimensions", List.of(),
                    value -> value instanceof String id && ResourceLocation.tryParse(id) != null);
    private static final ForgeConfigSpec SPEC = BUILDER.build();
    private static volatile Dimensions dimensions = new Dimensions(false,
            Set.of(new ResourceLocation("minecraft", "overworld")), Set.of());

    public static boolean sunlight() {
        return ServerPerformanceConfig.enabled() && SPEC.isLoaded() && SUNLIGHT.get();
    }

    public static boolean sunlight(Level level) {
        return sunlight() && dimensions.allows(level.dimension().location());
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC,
                "sunlitindustry-world-performance.toml");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(WorldPerformanceConfig::changed);
    }

    private static void changed(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) refresh();
    }

    public static void refresh() {
        if (!SPEC.isLoaded()) return;
        var allowed = new HashSet<ResourceLocation>();
        var excluded = new HashSet<ResourceLocation>();
        boolean all = false;
        for (String id : DIMENSIONS.get()) {
            if (id.equals("*")) all = true;
            else allowed.add(new ResourceLocation(id));
        }
        for (String id : EXCLUDED.get()) excluded.add(new ResourceLocation(id));
        dimensions = new Dimensions(all, Set.copyOf(allowed), Set.copyOf(excluded));
    }

    private record Dimensions(boolean all, Set<ResourceLocation> allowed, Set<ResourceLocation> excluded) {
        boolean allows(ResourceLocation dimension) {
            return !excluded.contains(dimension) && (all || allowed.contains(dimension));
        }
    }

    private WorldPerformanceConfig() {}
}
