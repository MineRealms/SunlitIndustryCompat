package com.society.sunlitindustry.mixin.performance.tiangong.client;

import com.society.sunlitindustry.performance.tiangong.TiangongClientConfig;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "zmod.client.effects.MachineEffectGeometry", remap = false)
public abstract class MachineEffectBezierMixin {
    @Inject(method = "bezier", at = @At("HEAD"), cancellable = true, require = 0)
    private static void sunlitindustry$bezier(Vector3f start, Vector3f control, Vector3f end,
            float t, CallbackInfoReturnable<Vector3f> cir) {
        if (!TiangongClientConfig.effectBezier()) return;
        float u = 1 - t;
        float startWeight = u * u;
        float controlWeight = 2 * u * t;
        float endWeight = t * t;
        cir.setReturnValue(new Vector3f(
                (start.x * startWeight + control.x * controlWeight) + end.x * endWeight,
                (start.y * startWeight + control.y * controlWeight) + end.y * endWeight,
                (start.z * startWeight + control.z * controlWeight) + end.z * endWeight));
    }
}
