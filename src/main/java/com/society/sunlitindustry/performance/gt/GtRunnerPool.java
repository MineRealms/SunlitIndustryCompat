package com.society.sunlitindustry.performance.gt;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeRunner;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Map;

public final class GtRunnerPool {
    private static final ThreadLocal<GtScratchPool<RecipeRunner>> POOL =
            ThreadLocal.withInitial(() -> new GtScratchPool<>(4));

    public static RecipeRunner acquire(GTRecipe recipe, IO io, boolean tick, IRecipeCapabilityHolder holder,
            Map<RecipeCapability<?>, Object2IntMap<?>> chances, boolean simulated) {
        RecipeRunner runner = POOL.get().take();
        if (runner == null) runner = new RecipeRunner(recipe, io, tick, holder, chances, simulated);
        ((GtReusableRunner) runner).sunlitindustry$prepare(recipe, io, tick, holder, chances, simulated);
        return runner;
    }

    public static void release(RecipeRunner runner) {
        if (((GtReusableRunner) runner).sunlitindustry$release()) POOL.get().giveBack(runner);
    }

    public static void clearCurrentThread() { POOL.remove(); }
    private GtRunnerPool() {}
}
