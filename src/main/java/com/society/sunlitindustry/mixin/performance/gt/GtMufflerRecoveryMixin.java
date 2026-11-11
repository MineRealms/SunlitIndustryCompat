package com.society.sunlitindustry.mixin.performance.gt;

import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.machine.multiblock.part.MufflerPartMachine;
import com.society.sunlitindustry.performance.gt.GtMufflerRecoveryConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MufflerPartMachine.class, remap = false)
public abstract class GtMufflerRecoveryMixin {
    @Shadow @Final private CustomItemStackHandler inventory;
    @Shadow private boolean calculateChance() { throw new AssertionError(); }

    @Inject(method = "recoverItemsTable", at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$recover(ItemStack[] recoveryItems, CallbackInfo ci) {
        if (!GtMufflerRecoveryConfig.enabled()) return;
        int limit = Math.min(recoveryItems.length, inventory.getSlots());
        for (int slot = 0; slot < limit; slot++) {
            if (calculateChance()) {
                ItemHandlerHelper.insertItemStacked(inventory, recoveryItems[slot].copy(), false);
            }
        }
        ci.cancel();
    }
}
