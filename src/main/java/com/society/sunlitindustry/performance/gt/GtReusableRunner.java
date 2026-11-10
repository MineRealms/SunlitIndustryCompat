package com.society.sunlitindustry.performance.gt;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Map;

public interface GtReusableRunner {
    void sunlitindustry$prepare(GTRecipe recipe, IO io, boolean tick, IRecipeCapabilityHolder holder,
            Map<RecipeCapability<?>, Object2IntMap<?>> chances, boolean simulated);

    boolean sunlitindustry$release();
}
