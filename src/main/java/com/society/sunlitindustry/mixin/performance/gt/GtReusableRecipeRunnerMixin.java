package com.society.sunlitindustry.mixin.performance.gt;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.IVoidable;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerList;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeRunner;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.society.sunlitindustry.GTHandlerGroupOwner;
import com.society.sunlitindustry.performance.gt.GtAdvancedConfig;
import com.society.sunlitindustry.performance.gt.GtReusableRunner;
import com.society.sunlitindustry.performance.gt.GtRunnerGroupOwnerAccess;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Mixin(value = RecipeRunner.class, remap = false, priority = 1100)
public abstract class GtReusableRecipeRunnerMixin implements GtReusableRunner {
    @Shadow @Final @Mutable private GTRecipe recipe;
    @Shadow @Final @Mutable private IO io;
    @Shadow @Final @Mutable private boolean isTick;
    @Shadow @Final @Mutable private Map<RecipeCapability<?>, Object2IntMap<?>> chanceCaches;
    @Shadow @Final @Mutable private Map<IO, List<RecipeHandlerList>> capabilityProxies;
    @Shadow @Final @Mutable private boolean simulated;
    @Shadow private Map<RecipeCapability<?>, List<Object>> recipeContents;
    @Shadow @Final @Mutable private Map<RecipeCapability<?>, List<Object>> searchRecipeContents;
    @Shadow @Final @Mutable private Predicate<RecipeCapability<?>> outputVoid;
    @Shadow private int groupColor;

    @Unique private final Map<RecipeCapability<?>, List<Object>> sunlitindustry$actual =
            new Reference2ObjectOpenHashMap<>();
    @Unique private final Map<RecipeCapability<?>, List<Object>> sunlitindustry$search =
            new Reference2ObjectOpenHashMap<>();
    @Unique private final Map<RecipeCapability<?>, ArrayList<Object>> sunlitindustry$actualLists =
            new Reference2ObjectOpenHashMap<>();
    @Unique private final Map<RecipeCapability<?>, ArrayList<Object>> sunlitindustry$searchLists =
            new Reference2ObjectOpenHashMap<>();
    @Unique private final ArrayList<Content> sunlitindustry$chanced = new ArrayList<>();
    @Unique private IRecipeCapabilityHolder sunlitindustry$holder;
    @Unique private final Predicate<RecipeCapability<?>> sunlitindustry$voidPredicate = cap ->
            sunlitindustry$holder instanceof IVoidable voidable && voidable.canVoidRecipeOutputs(cap);
    @Unique private boolean sunlitindustry$borrowed;
    @Unique private boolean sunlitindustry$oversized;

    @Override
    public void sunlitindustry$prepare(GTRecipe nextRecipe, IO nextIo, boolean tick,
            IRecipeCapabilityHolder holder, Map<RecipeCapability<?>, Object2IntMap<?>> chances,
            boolean simulate) {
        recipe = nextRecipe;
        io = nextIo;
        isTick = tick;
        chanceCaches = chances;
        capabilityProxies = holder.getCapabilitiesProxy();
        simulated = simulate;
        sunlitindustry$holder = holder;
        ((GtRunnerGroupOwnerAccess) this).sunlitindustry$setGroupOwner(
                holder instanceof GTHandlerGroupOwner owner ? owner : null);
        outputVoid = sunlitindustry$voidPredicate;
        groupColor = nextRecipe.groupColor;
        recipeContents = sunlitindustry$actual;
        searchRecipeContents = simulate ? recipeContents : sunlitindustry$search;
        sunlitindustry$borrowed = true;
        sunlitindustry$oversized = false;
    }

    @Inject(method = "fillContentMatchList", at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$fill(Map<RecipeCapability<?>, List<Content>> entries, CallbackInfo ci) {
        if (!sunlitindustry$borrowed || !GtAdvancedConfig.runners()) return;
        int recipeTier = 0;
        int chanceTier = 0;
        boolean tierReady = false;
        for (var entry : entries.entrySet()) {
            var cap = entry.getKey();
            var source = entry.getValue();
            if (!cap.doMatchInRecipe() || source.isEmpty()
                    || (simulated && io == IO.OUT && outputVoid.test(cap))) continue;
            if (source.size() > 256) sunlitindustry$oversized = true;
            var actual = sunlitindustry$actualLists.computeIfAbsent(cap, ignored -> new ArrayList<>());
            recipeContents.put(cap, actual);
            var search = simulated ? actual :
                    sunlitindustry$searchLists.computeIfAbsent(cap, ignored -> new ArrayList<>());
            searchRecipeContents.put(cap, search);
            sunlitindustry$chanced.clear();
            for (Content content : source) {
                search.add(content.content);
                if (simulated) continue;
                if (content.chance >= content.maxChance) actual.add(content.content);
                else if (content.chance > 0 || content.tierChanceBoost > 0) sunlitindustry$chanced.add(content);
            }
            if (!sunlitindustry$chanced.isEmpty()) {
                if (!tierReady) {
                    recipeTier = RecipeHelper.getPreOCRecipeEuTier(recipe);
                    chanceTier = recipeTier + recipe.ocLevel;
                    tierReady = true;
                }
                // Native roll owns RNG order and persistent chance debt; only its temporary input list is reused.
                var rolled = recipe.getChanceLogicForCapability(cap, io, isTick).roll(cap,
                        sunlitindustry$chanced, recipe.getType().getChanceFunction(), recipeTier,
                        chanceTier, chanceCaches.get(cap), recipe.getTotalRuns());
                for (Content content : rolled) actual.add(content.content);
            }
            if (actual.isEmpty()) recipeContents.remove(cap);
        }
        ci.cancel();
    }

    @Override
    public boolean sunlitindustry$release() {
        sunlitindustry$borrowed = false;
        boolean bounded = !sunlitindustry$oversized && sunlitindustry$actualLists.size() <= 16
                && sunlitindustry$searchLists.size() <= 16;
        for (var list : sunlitindustry$actualLists.values()) bounded &= list.size() <= 512;
        for (var list : sunlitindustry$searchLists.values()) bounded &= list.size() <= 512;
        sunlitindustry$actualLists.values().forEach(List::clear);
        sunlitindustry$searchLists.values().forEach(List::clear);
        sunlitindustry$actual.clear();
        sunlitindustry$search.clear();
        sunlitindustry$chanced.clear();
        recipe = null;
        chanceCaches = Collections.emptyMap();
        capabilityProxies = Collections.emptyMap();
        sunlitindustry$holder = null;
        ((GtRunnerGroupOwnerAccess) this).sunlitindustry$setGroupOwner(null);
        outputVoid = sunlitindustry$voidPredicate;
        recipeContents = sunlitindustry$actual;
        searchRecipeContents = sunlitindustry$search;
        return bounded;
    }
}
