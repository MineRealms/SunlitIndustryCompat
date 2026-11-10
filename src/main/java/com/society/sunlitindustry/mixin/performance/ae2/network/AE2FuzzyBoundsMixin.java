package com.society.sunlitindustry.mixin.performance.ae2.network;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEKey;
import com.society.sunlitindustry.ServerPerformanceConfig;
import com.society.sunlitindustry.performance.ae2.network.FuzzyBoundCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "appeng.api.stacks.FuzzySearch", remap = false)
public abstract class AE2FuzzyBoundsMixin {
    @Unique
    private static int sunlitindustry$damage(AEKey key, FuzzyMode fuzzy, boolean lower) {
        int max = key.getFuzzySearchMaxValue();
        int breakpoint = fuzzy == FuzzyMode.IGNORE_ALL ? 0 : fuzzy.calculateBreakPoint(max);
        return lower ? FuzzyBoundCache.lower(max, key.getFuzzySearchValue(), breakpoint,
                fuzzy == FuzzyMode.IGNORE_ALL) : FuzzyBoundCache.upper(key.getFuzzySearchValue(), breakpoint,
                fuzzy == FuzzyMode.IGNORE_ALL);
    }

    @Inject(method = "makeLowerBound", at = @At("HEAD"), cancellable = true)
    private static void sunlitindustry$reuseLower(AEKey key, FuzzyMode fuzzy, CallbackInfoReturnable<Object> cir) {
        if (!ServerPerformanceConfig.ae2FuzzyBounds() || key.getFuzzySearchMaxValue() <= 0) return;
        Object bound = FuzzyBoundCache.local().get(sunlitindustry$damage(key, fuzzy, true));
        if (bound != null) cir.setReturnValue(bound);
    }

    @Inject(method = "makeLowerBound", at = @At("RETURN"))
    private static void sunlitindustry$rememberLower(AEKey key, FuzzyMode fuzzy, CallbackInfoReturnable<Object> cir) {
        if (ServerPerformanceConfig.ae2FuzzyBounds() && key.getFuzzySearchMaxValue() > 0) {
            FuzzyBoundCache.local().put(sunlitindustry$damage(key, fuzzy, true), cir.getReturnValue());
        }
    }

    @Inject(method = "makeUpperBound", at = @At("HEAD"), cancellable = true)
    private static void sunlitindustry$reuseUpper(AEKey key, FuzzyMode fuzzy, CallbackInfoReturnable<Object> cir) {
        if (!ServerPerformanceConfig.ae2FuzzyBounds() || key.getFuzzySearchMaxValue() <= 0) return;
        Object bound = FuzzyBoundCache.local().get(sunlitindustry$damage(key, fuzzy, false));
        if (bound != null) cir.setReturnValue(bound);
    }

    @Inject(method = "makeUpperBound", at = @At("RETURN"))
    private static void sunlitindustry$rememberUpper(AEKey key, FuzzyMode fuzzy, CallbackInfoReturnable<Object> cir) {
        if (ServerPerformanceConfig.ae2FuzzyBounds() && key.getFuzzySearchMaxValue() > 0) {
            FuzzyBoundCache.local().put(sunlitindustry$damage(key, fuzzy, false), cir.getReturnValue());
        }
    }
}
