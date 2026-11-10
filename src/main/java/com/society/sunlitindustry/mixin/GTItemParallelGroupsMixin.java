package com.society.sunlitindustry.mixin;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerGroup;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerList;
import com.society.sunlitindustry.GTHandlerGroupOwner;
import com.society.sunlitindustry.GTHandlerGroups;
import com.society.sunlitindustry.ServerPerformanceConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = ItemRecipeCapability.class, remap = false)
public abstract class GTItemParallelGroupsMixin {
    @Redirect(method = "getInputContents", at = @At(value = "NEW", target = "java/util/HashMap"))
    private static HashMap<RecipeHandlerGroup, List<RecipeHandlerList>> sunlitindustry$groups(IRecipeCapabilityHolder holder) {
        if (!(holder instanceof GTHandlerGroupOwner owner) || !ServerPerformanceConfig.groups()) return new HashMap<>();
        return owner.sunlitindustry$handlerGroups().get(GTHandlerGroups.ITEM_INPUT,
                holder.getCapabilitiesForIO(IO.IN), ItemRecipeCapability.CAP);
    }

    @Redirect(method = "getInputContents", at = @At(value = "INVOKE",
            target = "Lcom/gregtechceu/gtceu/api/recipe/RecipeHelper;addToRecipeHandlerMap(Lcom/gregtechceu/gtceu/api/machine/trait/RecipeHandlerGroup;Lcom/gregtechceu/gtceu/api/machine/trait/RecipeHandlerList;Ljava/util/Map;)V"))
    private static void sunlitindustry$append(RecipeHandlerGroup group, RecipeHandlerList handler,
                                              Map<RecipeHandlerGroup, List<RecipeHandlerList>> groups) {
        GTHandlerGroups.append(group, handler, groups);
    }
}
