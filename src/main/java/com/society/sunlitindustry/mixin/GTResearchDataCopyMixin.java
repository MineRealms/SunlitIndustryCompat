package com.society.sunlitindustry.mixin;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.utils.ResearchManager;
import com.society.sunlitindustry.NuclearBalanceConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.gregtechceu.gtceu.utils.ResearchManager$DataStickCopyScannerLogic", remap = false)
public abstract class GTResearchDataCopyMixin {
    @Inject(
          method = "createDataRecipe(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Lcom/gregtechceu/gtceu/api/recipe/GTRecipe;",
          at = @At("HEAD"),
          cancellable = true,
          remap = false
    )
    private void sunlitindustry$preventModuleDowngrade(ItemStack destination, ItemStack source,
          CallbackInfoReturnable<GTRecipe> callback) {
        if (NuclearBalanceConfig.preventDataCarrierDowngrade()
              && ResearchManager.isStackDataItem(source, true)
              && !ResearchManager.isStackDataItem(source, false)
              && ResearchManager.isStackDataItem(destination, false)) {
            callback.setReturnValue(null);
        }
    }
}
