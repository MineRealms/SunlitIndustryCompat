package com.society.sunlitindustry.mixin.performance.ae2.network;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEKey;
import com.society.sunlitindustry.ServerPerformanceConfig;
import com.society.sunlitindustry.performance.ae2.network.FuzzyBoundCache;
import com.society.sunlitindustry.performance.ae2.network.FuzzyRangeViewCache;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.SortedMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "appeng.api.stacks.VariantCounter$FuzzyVariantMap", remap = false)
public abstract class AE2FuzzyRangeViewMixin {
    @Unique private FuzzyRangeViewCache<SortedMap<?, ?>> sunlitindustry$views;

    @WrapOperation(method = "findFuzzy", at = @At(value = "INVOKE", target =
            "Lappeng/api/stacks/FuzzySearch;findFuzzy(Ljava/util/SortedMap;Lappeng/api/stacks/AEKey;Lappeng/api/config/FuzzyMode;)Ljava/util/SortedMap;"))
    private SortedMap<?, ?> sunlitindustry$reuseLiveRange(SortedMap<?, ?> map, AEKey key, FuzzyMode fuzzy,
            Operation<SortedMap<?, ?>> original) {
        int max = key.getFuzzySearchMaxValue();
        if (!ServerPerformanceConfig.ae2FuzzyBounds() || max <= 0) {
            return original.call(map, key, fuzzy);
        }
        int value = key.getFuzzySearchValue();
        boolean ignore = fuzzy == FuzzyMode.IGNORE_ALL;
        int breakpoint = ignore ? 0 : fuzzy.calculateBreakPoint(max);
        int lower = FuzzyBoundCache.lower(max, value, breakpoint, ignore);
        int upper = FuzzyBoundCache.upper(value, breakpoint, ignore);
        if (sunlitindustry$views == null) sunlitindustry$views = new FuzzyRangeViewCache<>();
        SortedMap<?, ?> view = sunlitindustry$views.get(map, lower, upper);
        if (view == null) {
            view = original.call(map, key, fuzzy);
            sunlitindustry$views.put(map, lower, upper, view);
        }
        return view;
    }
}
