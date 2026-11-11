package com.society.sunlitindustry.performance.world;

import com.gregtechceu.gtceu.GTCEu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;

/** Per-call reuse only: world time, weather, biome tags and sky access are never cached. */
public final class GtSunlightQuery {
    private static final ResourceLocation JAVD_VOID_BIOME = new ResourceLocation("javd", "void");

    public static boolean canSeeSunClearly(Level level, BlockPos pos) {
        BlockPos above = pos.above();
        if (!level.canSeeSky(above)) return false;

        boolean javd = GTCEu.Mods.isJAVDLoaded();
        boolean day = level.isDay();
        // Native GT can only invert the day test for JAVD's registered void biome.
        if (!day && !javd) return false;

        var biomeHolder = level.getBiome(above);
        var biome = biomeHolder.value();
        if (level.isRaining() && (biome.warmEnoughToRain(above) || biome.coldEnoughToSnow(above))) {
            return false;
        }
        if (biomeHolder.is(BiomeTags.IS_END)) return false;
        if (javd && level.registryAccess().registryOrThrow(Registries.BIOME).getKey(biome)
                .equals(JAVD_VOID_BIOME)) return !day;
        return day;
    }

    private GtSunlightQuery() {}
}
