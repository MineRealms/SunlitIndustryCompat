package com.society.sunlitindustry.mixin.performance.tiangong.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.society.sunlitindustry.performance.tiangong.client.TokamakPlasmaLod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import zmod.machine.tokamak.TokamakController;
import zmod.machine.tokamak.TokamakGeometry;

@Pseudo
@Mixin(targets = "zmod.machine.tokamak.client.TokamakRenderer", remap = false)
public abstract class TokamakPlasmaLodMixin {
    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", ordinal = 0, target =
            "Lzmod/machine/tokamak/client/TokamakRenderer;CORE:Lzmod/machine/tokamak/TokamakGeometry$Mesh;"),
            require = 0)
    private TokamakGeometry.Mesh sunlitindustry$core(TokamakGeometry.Mesh original,
            @Local(argsOnly = true) TokamakController controller) {
        return TokamakPlasmaLod.select(original, 0, controller);
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", ordinal = 0, target =
            "Lzmod/machine/tokamak/client/TokamakRenderer;PLASMA:Lzmod/machine/tokamak/TokamakGeometry$Mesh;"),
            require = 0)
    private TokamakGeometry.Mesh sunlitindustry$plasma(TokamakGeometry.Mesh original,
            @Local(argsOnly = true) TokamakController controller) {
        return TokamakPlasmaLod.select(original, 1, controller);
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", ordinal = 0, target =
            "Lzmod/machine/tokamak/client/TokamakRenderer;HALO:Lzmod/machine/tokamak/TokamakGeometry$Mesh;"),
            require = 0)
    private TokamakGeometry.Mesh sunlitindustry$halo(TokamakGeometry.Mesh original,
            @Local(argsOnly = true) TokamakController controller) {
        return TokamakPlasmaLod.select(original, 2, controller);
    }
}
