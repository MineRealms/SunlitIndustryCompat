package com.society.sunlitindustry.mixin;

import net.minecraft.world.item.crafting.Recipe;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** KubeJS build 16 creates a Throwable before checking this cache. */
@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.recipe.RecipeJS", remap = false)
public abstract class KubeJSRecipeCacheMixin {
    @Shadow private MutableObject<Recipe<?>> originalRecipe;

    @Inject(method = "getOriginalRecipe()Lnet/minecraft/world/item/crafting/Recipe;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void sunlitindustry$cachedOriginal(CallbackInfoReturnable<Recipe<?>> cir) {
        if (originalRecipe != null) cir.setReturnValue(originalRecipe.getValue());
    }
}
