package com.society.sunlitindustry.mixin.performance.gt;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.society.sunlitindustry.performance.gt.GtAdvancedConfig;
import com.society.sunlitindustry.performance.gt.GtParallelScratch;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FluidRecipeCapability.class, remap = false)
public abstract class GtFluidParallelScratchMixin {
    @WrapMethod(method = "getMaxParallelByInput")
    private int sunlitindustry$scope(IRecipeCapabilityHolder holder, GTRecipe recipe, int limit,
            boolean tick, Operation<Integer> original) {
        if (!GtAdvancedConfig.parallel()) return original.call(holder, recipe, limit, tick);
        try (var ignored = GtParallelScratch.open()) {
            return original.call(holder, recipe, limit, tick);
        }
    }

    @Redirect(method = {"getMaxParallelByInput", "getInputContents"}, at = @At(value = "NEW",
            target = "it/unimi/dsi/fastutil/objects/Object2LongOpenHashMap"), require = 4)
    private static Object2LongOpenHashMap<?> sunlitindustry$map() { return GtParallelScratch.plainMap(); }
}
