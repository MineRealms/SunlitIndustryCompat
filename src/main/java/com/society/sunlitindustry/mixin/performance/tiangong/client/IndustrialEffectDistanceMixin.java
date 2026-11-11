package com.society.sunlitindustry.mixin.performance.tiangong.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.society.sunlitindustry.performance.tiangong.client.TiangongEffectDistance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Slice;

@Pseudo
@Mixin(targets = "zmod.machine.industrial.client.IndustrialRenderer", remap = false)
public abstract class IndustrialEffectDistanceMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target =
            "Lzmod/machine/industrial/IndustrialController;active()Z"), slice = @Slice(from =
            @At(value = "INVOKE", target = "Lzmod/machine/industrial/client/IndustrialPorts;render("
                    + "Lzmod/machine/industrial/IndustrialController;Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;II)V")), require = 0)
    private boolean sunlitindustry$effects(@Coerce Object controller, Operation<Boolean> original) {
        return original.call(controller) && TiangongEffectDistance.visible(controller);
    }
}
