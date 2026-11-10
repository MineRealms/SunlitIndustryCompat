package com.society.sunlitindustry.mixin.performance.ae2.network;

import appeng.api.storage.cells.IBasicCellItem;
import appeng.api.storage.cells.ISaveProvider;
import appeng.me.cells.BasicCellInventory;
import com.society.sunlitindustry.ServerPerformanceConfig;
import com.society.sunlitindustry.performance.ae2.network.CompactCellNbt;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BasicCellInventory.class, remap = false)
public abstract class AE2CompactCellNbtMixin {
    @Shadow @Final private ItemStack i;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void sunlitindustry$readCompactKeys(IBasicCellItem cellType, ItemStack stack,
            ISaveProvider container, CallbackInfo ci) {
        // Always accept previously written cells, including when compact writing has been disabled.
        if (stack.hasTag()) CompactCellNbt.expand(stack.getTag());
    }

    @Inject(method = "persist", at = @At("RETURN"))
    private void sunlitindustry$writeCompactKeys(CallbackInfo ci) {
        if (ServerPerformanceConfig.ae2CompactCellNbt() && i.hasTag()) CompactCellNbt.compact(i.getTag());
    }
}
