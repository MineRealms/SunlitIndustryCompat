package com.society.sunlitindustry.mixin;

import appeng.api.stacks.AEFluidKey;
import com.society.sunlitindustry.AE2DefaultFluidKeyHolder;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Fluid.class)
public abstract class AE2FluidDefaultKeyMixin implements AE2DefaultFluidKeyHolder {
    @Unique
    private volatile AEFluidKey sunlitindustry$defaultAEFluidKey;

    @Override
    public AEFluidKey sunlitindustry$getDefaultAEFluidKey() {
        return sunlitindustry$defaultAEFluidKey;
    }

    @Override
    public void sunlitindustry$setDefaultAEFluidKey(AEFluidKey key) {
        sunlitindustry$defaultAEFluidKey = key;
    }
}
