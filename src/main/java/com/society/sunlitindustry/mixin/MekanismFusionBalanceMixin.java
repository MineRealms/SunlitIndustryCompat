package com.society.sunlitindustry.mixin;

import com.society.sunlitindustry.NuclearBalanceConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "mekanism.generators.common.content.fusion.FusionReactorMultiblockData", remap = false)
public abstract class MekanismFusionBalanceMixin {
    @ModifyArg(
          method = "burnFuel()J",
          at = @At(
                value = "INVOKE",
                target = "Lmekanism/api/math/MathUtils;clampToLong(D)J",
                remap = false
          ),
          index = 0,
          remap = false
    )
    private double sunlitindustry$capFuelBurn(double requested) {
        return NuclearBalanceConfig.capFusionBurn(requested);
    }

    @ModifyVariable(
          method = "setInjectionRate(I)V",
          at = @At("HEAD"),
          argsOnly = true,
          ordinal = 0,
          remap = false
    )
    private int sunlitindustry$capInjectionRate(int requested) {
        return NuclearBalanceConfig.capFusionInjectionRate(requested);
    }
}
