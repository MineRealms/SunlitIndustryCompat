package com.society.sunlitindustry.mixin.performance.tiangong.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.society.sunlitindustry.performance.tiangong.TiangongClientConfig;
import com.society.sunlitindustry.performance.tiangong.client.DysonPrimitiveEmitter;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "zmod.machine.dyson.client.DysonSkyRenderer", remap = false)
public abstract class DysonSkyPrimitiveMixin {
    @Inject(method = "quad", at = @At("HEAD"), cancellable = true, require = 0)
    private static void sunlitindustry$quad(BufferBuilder buffer, Matrix4f matrix, Vector3f p,
            Vector3f q, Vector3f r, Vector3f s, int red, int green, int blue, int alpha,
            CallbackInfo ci) {
        if (!TiangongClientConfig.dysonPrimitives()) return;
        DysonPrimitiveEmitter.quad(buffer, matrix, p, q, r, s, red, green, blue, alpha);
        ci.cancel();
    }

    @Inject(method = "diamond", at = @At("HEAD"), cancellable = true, require = 0)
    private static void sunlitindustry$diamond(BufferBuilder buffer, Matrix4f matrix, Vector3f p,
            float radius, int red, int green, int blue, int alpha, CallbackInfo ci) {
        if (!TiangongClientConfig.dysonPrimitives()) return;
        DysonPrimitiveEmitter.diamond(buffer, matrix, p, radius, red, green, blue, alpha);
        ci.cancel();
    }

    @Inject(method = "line", at = @At("HEAD"), cancellable = true, require = 0)
    private static void sunlitindustry$line(BufferBuilder buffer, Matrix4f matrix, Vector3f p,
            Vector3f q, float width, int red, int green, int blue, int alpha, CallbackInfo ci) {
        if (!TiangongClientConfig.dysonPrimitives()) return;
        DysonPrimitiveEmitter.line(buffer, matrix, p, q, width, red, green, blue, alpha);
        ci.cancel();
    }
}
