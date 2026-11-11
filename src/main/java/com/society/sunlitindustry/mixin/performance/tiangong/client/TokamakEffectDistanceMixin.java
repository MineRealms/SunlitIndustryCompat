package com.society.sunlitindustry.mixin.performance.tiangong.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.society.sunlitindustry.performance.tiangong.client.TiangongEffectDistance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Mixin(targets = "zmod.machine.tokamak.client.TokamakRenderer", remap = false)
public abstract class TokamakEffectDistanceMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target =
            "Lzmod/machine/tokamak/TokamakController;isPreviewPlasmaVisible()Z"), require = 0)
    private boolean sunlitindustry$plasma(@Coerce Object controller, Operation<Boolean> original) {
        return original.call(controller) && TiangongEffectDistance.visible(controller);
    }
}
