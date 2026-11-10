package com.society.sunlitindustry.mixin.performance.ae2;

import com.society.sunlitindustry.performance.ae2.crafting.PatternDecodeCache;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeManager.class)
public abstract class AE2PatternRecipeEpochMixin {
    @Inject(method = {"apply", "replaceRecipes"}, at = @At("RETURN"))
    private void sunlitindustry$newRecipeEpoch(CallbackInfo ci) { PatternDecodeCache.invalidate(); }
}
