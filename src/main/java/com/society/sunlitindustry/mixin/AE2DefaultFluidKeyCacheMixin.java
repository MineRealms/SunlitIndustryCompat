package com.society.sunlitindustry.mixin;

import appeng.api.stacks.AEFluidKey;
import com.society.sunlitindustry.AE2DefaultFluidKeyHolder;
import com.society.sunlitindustry.ServerPerformanceConfig;
import com.society.sunlitindustry.ServerPerformanceStats;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AEFluidKey.class, remap = false)
public abstract class AE2DefaultFluidKeyCacheMixin {
    @Inject(method = "of(Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/nbt/CompoundTag;)Lappeng/api/stacks/AEFluidKey;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void sunlitindustry$getCachedDefaultKey(Fluid fluid, CompoundTag tag,
            CallbackInfoReturnable<AEFluidKey> cir) {
        if (!ServerPerformanceConfig.ae2DefaultFluidKeyCache() || tag != null || fluid == null
                || !(fluid instanceof AE2DefaultFluidKeyHolder holder)) return;
        AEFluidKey key = holder.sunlitindustry$getDefaultAEFluidKey();
        if (key == null) {
            ServerPerformanceStats.record(ServerPerformanceStats.Event.AE2_DEFAULT_FLUID_KEY_MISS);
            return;
        }
        ServerPerformanceStats.record(ServerPerformanceStats.Event.AE2_DEFAULT_FLUID_KEY_HIT);
        cir.setReturnValue(key);
    }

    @Inject(method = "of(Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/nbt/CompoundTag;)Lappeng/api/stacks/AEFluidKey;",
            at = @At("RETURN"), remap = false)
    private static void sunlitindustry$storeDefaultKey(Fluid fluid, CompoundTag tag,
            CallbackInfoReturnable<AEFluidKey> cir) {
        if (!ServerPerformanceConfig.ae2DefaultFluidKeyCache() || tag != null || fluid == null
                || !(fluid instanceof AE2DefaultFluidKeyHolder holder)) return;
        AEFluidKey key = cir.getReturnValue();
        if (key == null) return;
        holder.sunlitindustry$setDefaultAEFluidKey(key);
        ServerPerformanceStats.record(ServerPerformanceStats.Event.AE2_DEFAULT_FLUID_KEY_STORED);
    }
}
