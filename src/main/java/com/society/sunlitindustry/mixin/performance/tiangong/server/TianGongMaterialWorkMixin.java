package com.society.sunlitindustry.mixin.performance.tiangong.server;

import com.society.sunlitindustry.performance.tiangong.server.TianGongServerRecipeCache;
import java.util.List;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import zmod.compat.RecipeHolder;
import zmod.machine.industrial.IndustrialController;
import zmod.machine.materialprocessing.MaterialProcessingRecipe;

@Pseudo
@Mixin(targets = "zmod.machine.materialprocessing.MaterialProcessingWork", remap = false)
public abstract class TianGongMaterialWorkMixin {
    @Shadow @Final private IndustrialController owner;

    @Redirect(method = "recipes", at = @At(value = "INVOKE", target =
            "Lzmod/compat/Recipes;all(Lnet/minecraft/world/item/crafting/RecipeManager;Lnet/minecraft/world/item/crafting/RecipeType;)Ljava/util/List;"),
            remap = false)
    private List<RecipeHolder<MaterialProcessingRecipe>> sunlitindustry$machineRecipes(
            RecipeManager manager, RecipeType<MaterialProcessingRecipe> type) {
        return TianGongServerRecipeCache.materialRecipes(manager, type, owner.kind().id());
    }
}
