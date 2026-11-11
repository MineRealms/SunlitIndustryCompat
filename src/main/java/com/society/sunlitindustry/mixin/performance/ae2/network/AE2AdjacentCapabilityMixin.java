package com.society.sunlitindustry.mixin.performance.ae2.network;

import appeng.util.BlockApiCache;
import com.society.sunlitindustry.performance.ae2.network.CapabilityLookupCache;
import com.society.sunlitindustry.performance.ae2.network.AE2NetworkConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.Capability;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockApiCache.class, remap = false)
public abstract class AE2AdjacentCapabilityMixin<C> {
    @Shadow @Final private ServerLevel level;
    @Shadow @Final private BlockPos fromPos;
    @Shadow @Final private Capability<C> capability;
    @Unique private CapabilityLookupCache<C> sunlitindustry$capabilities;

    @Inject(method = "find", at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$find(Direction side, CallbackInfoReturnable<C> cir) {
        if (!AE2NetworkConfig.adjacency()) {
            if (sunlitindustry$capabilities != null) {
                sunlitindustry$capabilities.clear();
                sunlitindustry$capabilities = null;
            }
            return;
        }
        if (sunlitindustry$capabilities == null) sunlitindustry$capabilities = new CapabilityLookupCache<>();
        cir.setReturnValue(sunlitindustry$capabilities.find(level.getBlockEntity(fromPos), capability, side));
    }
}
