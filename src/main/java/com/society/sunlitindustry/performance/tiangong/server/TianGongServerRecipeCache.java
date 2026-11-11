package com.society.sunlitindustry.performance.tiangong.server;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.server.ServerLifecycleHooks;
import zmod.compat.RecipeHolder;
import zmod.compat.Recipes;
import zmod.machine.materialprocessing.MaterialProcessingRecipe;

public final class TianGongServerRecipeCache {
    private static final AtomicLong EPOCH = new AtomicLong();

    public static void invalidate() {
        EPOCH.incrementAndGet();
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) server.execute(TianGongServerRecipeCache::clearCurrentManager);
    }
    public static long epoch() { return EPOCH.get(); }

    private static void clearCurrentManager() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.getRecipeManager() instanceof TianGongRecipeCacheOwner owner) {
            owner.sunlitindustry$clearTiangongRecipeSnapshots();
        }
    }

    private static TianGongRecipeSnapshots snapshots(RecipeManager manager) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread() || server.getRecipeManager() != manager
                || !(manager instanceof TianGongRecipeCacheOwner owner)) return null;
        return owner.sunlitindustry$tiangongRecipeSnapshots();
    }

    public static <T extends Recipe<?>> List<RecipeHolder<T>> cached(RecipeManager manager, RecipeType<T> type) {
        if (!TianGongServerConfig.recipeLists()) return null;
        var cache = snapshots(manager);
        return cache == null ? null : cache.recipes(type);
    }

    public static <T extends Recipe<?>> void remember(RecipeManager manager, RecipeType<T> type,
                                                       List<RecipeHolder<T>> list) {
        if (!TianGongServerConfig.recipeLists()) return;
        var cache = snapshots(manager);
        if (cache != null) cache.remember(type, list);
    }

    public static List<RecipeHolder<MaterialProcessingRecipe>> materialRecipes(
            RecipeManager manager, RecipeType<MaterialProcessingRecipe> type, String machine) {
        if (!TianGongServerConfig.materialGroups()) return Recipes.all(manager, type);
        var cache = snapshots(manager);
        if (cache == null) return Recipes.all(manager, type);
        var group = cache.materialGroup(type, machine);
        return group != null ? group : cache.selectMaterialGroup(type, machine, Recipes.all(manager, type));
    }

    private TianGongServerRecipeCache() {}
}
