package com.society.sunlitindustry.mixin;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.society.sunlitindustry.GTHandlerGroupOwner;
import com.society.sunlitindustry.GTHandlerGroups;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MetaMachine.class, remap = false)
public abstract class GTMachineGroupCacheMixin implements GTHandlerGroupOwner {
    @Unique private GTHandlerGroups sunlitindustry$groups;

    @Override
    public synchronized GTHandlerGroups sunlitindustry$handlerGroups() {
        if (sunlitindustry$groups == null) sunlitindustry$groups = new GTHandlerGroups();
        return sunlitindustry$groups;
    }

    @Override
    public synchronized void sunlitindustry$clearHandlerGroups() { sunlitindustry$groups = null; }

    @Inject(method = "onUnload", at = @At("HEAD"))
    private void sunlitindustry$releaseGroups(CallbackInfo ci) { sunlitindustry$clearHandlerGroups(); }
}
