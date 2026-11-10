package com.society.sunlitindustry.mixin.performance.ae2.network;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEKey;
import java.util.SortedMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "appeng.api.stacks.FuzzySearch", remap = false)
public interface AE2FuzzySearchInvoker {
    @Invoker("findFuzzy")
    static SortedMap<?, ?> sunlitindustry$findFuzzy(SortedMap<?, ?> map, AEKey key, FuzzyMode fuzzy) {
        throw new AssertionError("Mixin invoker not applied");
    }
}
