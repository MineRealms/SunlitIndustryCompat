package com.society.sunlitindustry.mixin.performance.ae2;

import com.society.sunlitindustry.performance.ae2.crafting.Ae2CraftingConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "appeng.me.service.helpers.NetworkCraftingProviders$PatternsForKey", remap = false)
public abstract class AE2ProviderSortCacheMixin {
    @Shadow private boolean needsSorting;

    @Inject(method = "sortPatterns", at = @At("RETURN"))
    private void sunlitindustry$sorted(CallbackInfo ci) {
        if (Ae2CraftingConfig.sorting()) needsSorting = false;
    }
}
