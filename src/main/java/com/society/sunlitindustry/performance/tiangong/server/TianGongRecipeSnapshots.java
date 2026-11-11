package com.society.sunlitindustry.performance.tiangong.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import zmod.compat.RecipeHolder;
import zmod.machine.materialprocessing.MaterialProcessingRecipe;

/** Owned by one RecipeManager, used only by its server thread, and bounded by retained holder counts. */
public final class TianGongRecipeSnapshots {
    private long epoch = Long.MIN_VALUE;
    private final Map<RecipeType<?>, List<?>> recipes = new IdentityHashMap<>();
    private final Map<RecipeType<?>, Map<String, List<RecipeHolder<MaterialProcessingRecipe>>>> materials =
            new IdentityHashMap<>();
    private int recipeHolders;
    private int materialHolders;
    private int materialGroups;

    private void checkEpoch() {
        long current = TianGongServerRecipeCache.epoch();
        if (epoch == current) return;
        recipes.clear();
        materials.clear();
        recipeHolders = materialHolders = materialGroups = 0;
        epoch = current;
    }

    @SuppressWarnings("unchecked")
    public <T extends Recipe<?>> List<RecipeHolder<T>> recipes(RecipeType<T> type) {
        checkEpoch();
        return (List<RecipeHolder<T>>) recipes.get(type);
    }

    public <T extends Recipe<?>> void remember(RecipeType<T> type, List<RecipeHolder<T>> list) {
        checkEpoch();
        if (recipes.containsKey(type) || recipes.size() >= TianGongServerConfig.maxTypes()
                || list.size() > TianGongServerConfig.maxHolders() - recipeHolders) return;
        recipes.put(type, list);
        recipeHolders += list.size();
    }

    public List<RecipeHolder<MaterialProcessingRecipe>> materialGroup(
            RecipeType<MaterialProcessingRecipe> type, String machine) {
        checkEpoch();
        var groups = materials.get(type);
        return groups == null ? null : groups.get(machine);
    }

    public List<RecipeHolder<MaterialProcessingRecipe>> selectMaterialGroup(
            RecipeType<MaterialProcessingRecipe> type, String machine,
            List<RecipeHolder<MaterialProcessingRecipe>> source) {
        checkEpoch();
        if (materialGroups >= TianGongServerConfig.maxGroups()
                || source.size() > TianGongServerConfig.maxHolders() - materialHolders) return source;
        var selected = new ArrayList<RecipeHolder<MaterialProcessingRecipe>>();
        for (var holder : source) if (holder.value().machine().equals(machine)) selected.add(holder);
        var result = List.copyOf(selected);
        materials.computeIfAbsent(type, ignored -> new HashMap<>()).put(machine, result);
        materialGroups++;
        materialHolders += result.size();
        return result;
    }
}
