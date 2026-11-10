package com.society.sunlitindustry.mixin;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.ActionResult;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.society.sunlitindustry.ServerPerformanceConfig;
import com.society.sunlitindustry.ServerPerformanceStats;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(value = RecipeHelper.class, remap = false)
public abstract class GTRecipeFastPathMixin {
    @Inject(method = "handleRecipe", at = @At("HEAD"), cancellable = true)
    private static void sunlitindustry$emptyIO(IRecipeCapabilityHolder holder, GTRecipe recipe, IO io,
            Map<RecipeCapability<?>, List<Content>> contents,
            Map<RecipeCapability<?>, Object2IntMap<?>> chanceCaches, boolean isTick, boolean simulated,
            CallbackInfoReturnable<ActionResult> cir) {
        if (ServerPerformanceConfig.emptyIO() && contents.isEmpty() && holder.hasCapabilityProxies()
                && io != IO.BOTH) {
            ServerPerformanceStats.record(ServerPerformanceStats.Event.EMPTY_IO);
            cir.setReturnValue(ActionResult.PASS_NO_CONTENTS);
        }
    }

    @Inject(method = "checkConditions", at = @At("HEAD"), cancellable = true)
    private static void sunlitindustry$andOnly(GTRecipe recipe, RecipeLogic logic,
                                               CallbackInfoReturnable<ActionResult> cir) {
        if (!ServerPerformanceConfig.conditions() || recipe.conditions.isEmpty()) return;
        for (var condition : recipe.conditions) if (condition.isOr()) return;
        ServerPerformanceStats.record(ServerPerformanceStats.Event.AND_CONDITIONS);
        for (var condition : recipe.conditions) {
            if (!condition.check(recipe, logic)) {
                cir.setReturnValue(ActionResult.fail(Component.translatable("gtceu.recipe_logic.condition_fails")
                        .append(": ").append(condition.getTooltips()), null, null));
                return;
            }
        }
        cir.setReturnValue(ActionResult.SUCCESS);
    }
}
