package com.society.sunlitindustry.mixin.performance.gt;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.ActionResult;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.RecipeRunner;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.society.sunlitindustry.performance.gt.GtAdvancedConfig;
import com.society.sunlitindustry.performance.gt.GtRunnerPool;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
import java.util.Map;

@Mixin(value = RecipeHelper.class, remap = false)
public abstract class GtRecipePipelineMixin {
    @Inject(method = "handleRecipe", at = @At("HEAD"), cancellable = true)
    private static void sunlitindustry$run(IRecipeCapabilityHolder holder, GTRecipe recipe, IO io,
            Map<RecipeCapability<?>, List<Content>> contents,
            Map<RecipeCapability<?>, Object2IntMap<?>> chances, boolean tick, boolean simulated,
            CallbackInfoReturnable<ActionResult> cir) {
        if (!GtAdvancedConfig.runners() || contents.isEmpty()) return;
        RecipeRunner runner = GtRunnerPool.acquire(recipe, io, tick, holder, chances, simulated);
        ActionResult result;
        try {
            result = runner.handle(contents);
            if (result.isSuccess() || result.capability() == null) {
                recipe.groupColor = runner.getGroupColor();
            } else {
                if (!simulated && ConfigHolder.INSTANCE.dev.debug) {
                    GTCEu.LOGGER.warn("IO {} Error while handling recipe {} outputs for {}",
                            Component.translatable(io.tooltip).getString(), recipe, holder);
                }
                String key = "gtceu.recipe_logic.insufficient_" + (io == IO.IN ? "in" : "out");
                result = ActionResult.fail(Component.translatable(key).append(": ")
                        .append(result.capability().getName()), result.capability(), io);
            }
        } finally {
            GtRunnerPool.release(runner);
        }
        cir.setReturnValue(result);
    }
}
