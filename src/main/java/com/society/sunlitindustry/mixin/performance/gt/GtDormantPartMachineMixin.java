package com.society.sunlitindustry.mixin.performance.gt;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.society.sunlitindustry.performance.gt.GtAdvancedConfig;
import com.society.sunlitindustry.performance.gt.GtDormantPart;
import com.society.sunlitindustry.performance.gt.GtDormantTickRegistry;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(value = MetaMachine.class, remap = false)
public abstract class GtDormantPartMachineMixin implements GtDormantPart {
    @Shadow @Final private List<TickableSubscription> serverTicks;
    @Shadow @Final private List<TickableSubscription> waitingToAdd;
    @Unique private boolean sunlitindustry$dormant;

    @Inject(method = "executeTick", at = @At("RETURN"))
    private void sunlitindustry$suspendEmpty(CallbackInfo ci) {
        if (!GtAdvancedConfig.dormantPartTick() || sunlitindustry$dormant
                || !serverTicks.isEmpty() || !waitingToAdd.isEmpty()) return;
        var machine = (MetaMachine) (Object) this;
        if (!(machine instanceof IMultiPart part) || !part.isFormed()
                || machine.isInValid()
                || !machine.getClass().getName().startsWith("com.gregtechceu.gtceu.")) return;
        if (!(machine.getLevel() instanceof ServerLevel level) || !level.getServer().isSameThread()) return;
        var blockEntity = machine.getHolder().getSelf();
        // A custom machine block may add work around MetaMachine.serverTick(). Never suppress it.
        if (!blockEntity.getBlockState().getBlock().getClass().getName()
                .equals("com.gregtechceu.gtceu.api.block.MetaMachineBlock")) return;
        sunlitindustry$dormant = true;
        GtDormantTickRegistry.add(this);
        ((GtChunkTickerAccess) level.getChunkAt(blockEntity.getBlockPos()))
                .sunlitindustry$refreshTicker(blockEntity);
    }

    @Inject(method = "subscribeServerTick", at = @At("RETURN"))
    private void sunlitindustry$resume(Runnable runnable, CallbackInfoReturnable<TickableSubscription> cir) {
        if (cir.getReturnValue() != null) sunlitindustry$wakeTicker();
    }

    @Inject(method = "onUnload", at = @At("HEAD"))
    private void sunlitindustry$unload(CallbackInfo ci) {
        sunlitindustry$dormant = false;
        GtDormantTickRegistry.remove(this);
    }

    @Override
    public boolean sunlitindustry$isDormant() {
        return sunlitindustry$dormant && serverTicks.isEmpty() && waitingToAdd.isEmpty();
    }

    @Override
    public void sunlitindustry$wakeTicker() {
        if (!sunlitindustry$dormant) return;
        sunlitindustry$dormant = false;
        GtDormantTickRegistry.remove(this);
        var machine = (MetaMachine) (Object) this;
        if (machine.isInValid() || !(machine.getLevel() instanceof ServerLevel level)) return;
        var blockEntity = machine.getHolder().getSelf();
        if (level.getChunkSource().getChunkNow(blockEntity.getBlockPos().getX() >> 4,
                blockEntity.getBlockPos().getZ() >> 4) instanceof GtChunkTickerAccess access) {
            access.sunlitindustry$refreshTicker(blockEntity);
        }
    }
}
