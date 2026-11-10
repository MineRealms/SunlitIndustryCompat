package com.society.sunlitindustry.mixin.performance.ae2;

import appeng.util.BlockApiCache;
import com.society.sunlitindustry.performance.ae2.capability.CapabilityLookupCache;
import com.society.sunlitindustry.performance.ae2.crafting.Ae2CraftingConfig;
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
    @Unique private final CapabilityLookupCache<C> sunlitindustry$capabilities = new CapabilityLookupCache<>();

    @Inject(method = "find", at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$find(Direction side, CallbackInfoReturnable<C> cir) {
        if (!Ae2CraftingConfig.adjacency()) {
            sunlitindustry$capabilities.clear();
            return;
        }
        cir.setReturnValue(sunlitindustry$capabilities.find(level.getBlockEntity(fromPos), capability, side));
    }
}
