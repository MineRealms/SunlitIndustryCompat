package com.society.sunlitindustry.mixin;

import com.society.sunlitindustry.NuclearBalanceConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "mekanism.generators.common.content.fission.FissionReactorMultiblockData", remap = false)
public abstract class MekanismFissionBalanceMixin {
    @Inject(method = "getMaxBurnRate()J", at = @At("RETURN"), cancellable = true, remap = false)
    private void sunlitindustry$capAdvertisedRate(CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(NuclearBalanceConfig.maxFissionBurnRate(cir.getReturnValue()));
    }

    @ModifyVariable(
          method = "setRateLimit(D)V",
          at = @At("HEAD"),
          argsOnly = true,
          ordinal = 0,
          remap = false
    )
    private double sunlitindustry$capConfiguredRate(double requested) {
        return NuclearBalanceConfig.capFissionRate(requested);
    }

    @ModifyArg(
          method = "burnFuel(Lnet/minecraft/world/level/Level;)V",
          at = @At(
                value = "INVOKE",
                target = "Ljava/lang/Math;min(DD)D",
                ordinal = 1
          ),
          index = 1
    )
    private double sunlitindustry$capActualBurn(double requested) {
        return NuclearBalanceConfig.capFissionBurn(requested);
    }
}
