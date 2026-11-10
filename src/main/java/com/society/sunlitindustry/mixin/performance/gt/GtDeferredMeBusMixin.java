package com.society.sunlitindustry.mixin.performance.gt;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEOutputBusPartMachine;
import com.society.sunlitindustry.performance.gt.GtAdvancedConfig;
import com.society.sunlitindustry.performance.gt.GtDeferredFlushTarget;
import com.society.sunlitindustry.performance.gt.GtDeferredMeFlush;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MEOutputBusPartMachine.class, remap = false)
public abstract class GtDeferredMeBusMixin implements GtDeferredFlushTarget {
    @Shadow public abstract void autoIO();
    @Unique private boolean sunlitindustry$flushing;

    @Inject(method = "autoIO", at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$defer(CallbackInfo ci) {
        var machine = (MetaMachine) (Object) this;
        if (sunlitindustry$flushing || !GtAdvancedConfig.deferredMeFlush() || machine.isInValid()) return;
        if (machine.getLevel() instanceof ServerLevel level && GtDeferredMeFlush.enqueue(level, this)) ci.cancel();
    }

    @Override
    public void sunlitindustry$flushNow() {
        var machine = (MetaMachine) (Object) this;
        if (machine.isInValid() || !(machine.getLevel() instanceof ServerLevel level)
                || !level.getServer().isSameThread()) return;
        sunlitindustry$flushing = true;
        try { autoIO(); }
        finally { sunlitindustry$flushing = false; }
    }
}
