package com.society.sunlitindustry.mixin.performance.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.crafting.CraftingTreeNode;
import appeng.crafting.execution.InputTemplate;
import appeng.crafting.inv.ICraftingInventory;
import com.society.sunlitindustry.performance.ae2.crafting.Ae2CraftingConfig;
import java.util.List;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftingTreeNode.class, remap = false)
public abstract class AE2CraftingRootTemplatesMixin {
    @Shadow @Final private AEKey what;
    @Shadow @Final IPatternDetails.IInput parentInput;
    @Unique private List<InputTemplate> sunlitindustry$rootTemplate;

    @Inject(method = "getValidItemTemplates", at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$rootTemplate(ICraftingInventory inventory,
                                           CallbackInfoReturnable<Iterable<InputTemplate>> cir) {
        if (parentInput != null || !Ae2CraftingConfig.sorting()) return;
        if (sunlitindustry$rootTemplate == null) {
            sunlitindustry$rootTemplate = List.of(new InputTemplate(what, 1L));
        }
        cir.setReturnValue(sunlitindustry$rootTemplate);
    }
}
