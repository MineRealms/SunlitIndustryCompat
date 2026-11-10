package com.society.sunlitindustry.mixin;

import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.society.sunlitindustry.VoidMekaSuitAssembly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = RecipeLogic.class, remap = false)
public abstract class GTVoidMekaSuitAssemblyMixin {
    @ModifyVariable(method = "setupRecipe", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private GTRecipe sunlitindustry$inheritSuitData(GTRecipe recipe) {
        return VoidMekaSuitAssembly.prepare((RecipeLogic) (Object) this, recipe);
    }
}
