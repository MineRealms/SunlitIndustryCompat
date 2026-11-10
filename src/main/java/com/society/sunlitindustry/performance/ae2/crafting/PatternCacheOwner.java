package com.society.sunlitindustry.performance.ae2.crafting;

import appeng.api.crafting.IPatternDetails;

public interface PatternCacheOwner {
    EpochLruCache<PatternCacheKey, IPatternDetails> sunlitindustry$patternCache();
}
