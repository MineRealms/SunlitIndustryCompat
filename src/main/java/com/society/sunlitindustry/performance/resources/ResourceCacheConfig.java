package com.society.sunlitindustry.performance.resources;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class ResourceCacheConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Content-addressed snapshots of immutable mod archives. Dynamic packs and directories stay native.")
            .define("immutableArchiveCache", true);
    private static final ForgeConfigSpec.BooleanValue CLIENT = BUILDER
            .comment("Cache model JSON/metadata source bytes, never baked models. Disabled with ModernFix or Fusion.")
            .define("clientModelSourceCache", false);
    private static final ForgeConfigSpec.IntValue MEMORY = BUILDER
            .comment("Maximum retained snapshot bytes; transient construction may require additional memory.")
            .defineInRange("memoryMiB", 32, 4, 256);
    private static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enabled() {
        return !Boolean.getBoolean("sunlitindustry.disableServerOptimizations")
                && (!SPEC.isLoaded() || ENABLED.get());
    }

    public static boolean clientModels() { return SPEC.isLoaded() && CLIENT.get(); }
    public static int memoryMiB() { return SPEC.isLoaded() ? MEMORY.get() : 32; }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "sunlitindustry-resources.toml");
    }

    private ResourceCacheConfig() {}
}
