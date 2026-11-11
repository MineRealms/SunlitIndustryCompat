package com.society.sunlitindustry.mixin.performance.tiangong.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.society.sunlitindustry.performance.tiangong.TiangongClientConfig;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "zmod.client.effects.ArcaneGeometry", remap = false)
public abstract class ArcaneRingMixin {
    @Shadow
    public static void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z,
            int color, float alpha) {
        throw new AssertionError();
    }

    @Inject(method = "ring", at = @At("HEAD"), cancellable = true, require = 0)
    private static void sunlitindustry$ring(VertexConsumer buffer, Matrix4f matrix, float radius,
            float width, float phase, int color, float alpha, CallbackInfo ci) {
        if (!TiangongClientConfig.arcaneRings()) return;
        double tau = Math.PI * 2;
        float inner = radius - width;
        float outer = radius + width;
        float previousCos = (float) Math.cos(phase + 0.0);
        float previousSin = (float) Math.sin(phase + 0.0);
        for (int i = 0; i < 96; i++) {
            double angle = phase + tau * (i + 1) / 96;
            float nextCos = (float) Math.cos(angle);
            float nextSin = (float) Math.sin(angle);
            vertex(buffer, matrix, inner * previousCos, 0, inner * previousSin, color, alpha);
            vertex(buffer, matrix, outer * previousCos, 0, outer * previousSin, color, alpha);
            vertex(buffer, matrix, outer * nextCos, 0, outer * nextSin, color, alpha);
            vertex(buffer, matrix, inner * nextCos, 0, inner * nextSin, color, alpha);
            previousCos = nextCos;
            previousSin = nextSin;
        }
        ci.cancel();
    }
}
