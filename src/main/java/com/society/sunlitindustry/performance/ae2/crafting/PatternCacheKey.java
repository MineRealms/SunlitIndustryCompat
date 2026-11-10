package com.society.sunlitindustry.performance.ae2.crafting;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;

public record PatternCacheKey(AEItemKey definition, Class<? extends IPatternDetails> decoderType) {}
