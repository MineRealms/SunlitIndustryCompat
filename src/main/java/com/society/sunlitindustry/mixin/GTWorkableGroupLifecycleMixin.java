package com.society.sunlitindustry.mixin;

import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.society.sunlitindustry.GTHandlerGroupOwner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WorkableMultiblockMachine.class, remap = false)
public abstract class GTWorkableGroupLifecycleMixin {
    @Inject(method = {"onStructureFormed", "onStructureInvalid", "onPartUnload"}, at = @At("HEAD"))
    private void sunlitindustry$releaseGroups(CallbackInfo ci) {
        ((GTHandlerGroupOwner) this).sunlitindustry$clearHandlerGroups();
    }
}
