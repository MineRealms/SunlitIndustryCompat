package com.society.sunlitindustry.mixin.performance.gt;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.society.sunlitindustry.performance.gt.GtDormantPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MultiblockPartMachine.class, remap = false)
public abstract class GtDormantPartLifecycleMixin {
    @Inject(method = "removedFromController", at = @At("RETURN"))
    private void sunlitindustry$unformed(IMultiController controller, CallbackInfo ci) {
        ((GtDormantPart) this).sunlitindustry$wakeTicker();
    }
}
