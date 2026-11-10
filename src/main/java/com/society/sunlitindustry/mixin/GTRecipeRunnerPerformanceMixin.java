package com.society.sunlitindustry.mixin;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerGroup;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerList;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeRunner;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.society.sunlitindustry.GTHandlerGroupOwner;
import com.society.sunlitindustry.GTHandlerGroups;
import com.society.sunlitindustry.ServerPerformanceConfig;
import com.society.sunlitindustry.ServerPerformanceStats;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Mixin(value = RecipeRunner.class, remap = false)
public abstract class GTRecipeRunnerPerformanceMixin {
    @Shadow @Final private IO io;
    @Shadow @Final private boolean simulated;
    @Shadow @Final private Map<IO, List<RecipeHandlerList>> capabilityProxies;
    @Shadow @Final private Predicate<RecipeCapability<?>> outputVoid;
    @Shadow private Map<RecipeCapability<?>, List<Object>> recipeContents;
    @Unique private GTHandlerGroupOwner sunlitindustry$owner;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void sunlitindustry$owner(GTRecipe recipe, IO io, boolean isTick, IRecipeCapabilityHolder holder,
            Map<RecipeCapability<?>, Object2IntMap<?>> chances, boolean simulated, CallbackInfo ci) {
        if (holder instanceof GTHandlerGroupOwner owner) sunlitindustry$owner = owner;
    }

    @Inject(method = "fillContentMatchList", at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$simulate(Map<RecipeCapability<?>, List<Content>> entries, CallbackInfo ci) {
        if (!simulated || !ServerPerformanceConfig.simulation()) return;
        ServerPerformanceStats.record(ServerPerformanceStats.Event.SIMULATION);
        // The native simulated path requires every content, including non-consumed and chanced entries.
        for (var entry : entries.entrySet()) {
            var cap = entry.getKey();
            if (!cap.doMatchInRecipe() || (io == IO.OUT && outputVoid.test(cap)) || entry.getValue().isEmpty()) continue;
            var contents = recipeContents.computeIfAbsent(cap, ignored -> new ArrayList<>(entry.getValue().size()));
            for (var content : entry.getValue()) contents.add(content.content);
        }
        ci.cancel();
    }

    @Redirect(method = "handleContents", at = @At(value = "NEW", target = "java/util/HashMap"))
    private HashMap<RecipeHandlerGroup, List<RecipeHandlerList>> sunlitindustry$groups() {
        if (sunlitindustry$owner == null || !ServerPerformanceConfig.groups()) return new HashMap<>();
        return sunlitindustry$owner.sunlitindustry$handlerGroups().get(GTHandlerGroups.key(io),
                capabilityProxies.getOrDefault(io, Collections.emptyList()), null);
    }

    @Redirect(method = "handleContents", at = @At(value = "INVOKE",
            target = "Lcom/gregtechceu/gtceu/api/recipe/RecipeHelper;addToRecipeHandlerMap(Lcom/gregtechceu/gtceu/api/machine/trait/RecipeHandlerGroup;Lcom/gregtechceu/gtceu/api/machine/trait/RecipeHandlerList;Ljava/util/Map;)V"))
    private void sunlitindustry$append(RecipeHandlerGroup group, RecipeHandlerList handler,
                                       Map<RecipeHandlerGroup, List<RecipeHandlerList>> groups) {
        GTHandlerGroups.append(group, handler, groups);
    }
}
