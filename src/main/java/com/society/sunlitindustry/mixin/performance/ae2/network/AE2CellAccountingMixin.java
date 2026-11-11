package com.society.sunlitindustry.mixin.performance.ae2.network;

import appeng.api.stacks.AEKey;
import appeng.api.storage.cells.ISaveProvider;
import appeng.me.cells.BasicCellInventory;
import com.society.sunlitindustry.performance.ae2.network.AE2NetworkConfig;
import com.society.sunlitindustry.performance.ae2.network.CellAccounting;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BasicCellInventory.class, remap = false)
public abstract class AE2CellAccountingMixin {
    @Shadow private Object2LongMap<AEKey> storedAmounts;
    @Shadow private short storedItems;
    @Shadow private long storedItemCount;
    @Shadow private boolean isPersisted;
    @Shadow @Final private ISaveProvider container;
    @Shadow public abstract void persist();

    @Inject(method = "saveChanges", at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$primitiveCellAccounting(CallbackInfo ci) {
        if (!AE2NetworkConfig.cellAccounting()) return;
        storedItems = (short) storedAmounts.size();
        storedItemCount = CellAccounting.sum(storedAmounts);
        isPersisted = false;
        if (container != null) container.saveChanges();
        else persist();
        ci.cancel();
    }
}
