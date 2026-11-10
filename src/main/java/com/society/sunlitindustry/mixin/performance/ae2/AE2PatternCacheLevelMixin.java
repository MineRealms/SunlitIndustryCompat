package com.society.sunlitindustry.mixin.performance.ae2;

import appeng.api.crafting.IPatternDetails;
import com.society.sunlitindustry.performance.ae2.crafting.EpochLruCache;
import com.society.sunlitindustry.performance.ae2.crafting.PatternCacheOwner;
import com.society.sunlitindustry.performance.ae2.crafting.PatternCacheKey;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerLevel.class)
public abstract class AE2PatternCacheLevelMixin implements PatternCacheOwner {
    @Unique
    private final EpochLruCache<PatternCacheKey, IPatternDetails> sunlitindustry$decodedPatterns = new EpochLruCache<>();

    @Override
    public EpochLruCache<PatternCacheKey, IPatternDetails> sunlitindustry$patternCache() {
        return sunlitindustry$decodedPatterns;
    }
}
