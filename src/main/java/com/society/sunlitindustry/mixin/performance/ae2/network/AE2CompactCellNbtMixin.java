package com.society.sunlitindustry.mixin.performance.ae2.network;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.cells.IBasicCellItem;
import appeng.api.storage.cells.ISaveProvider;
import appeng.me.cells.BasicCellInventory;
import com.society.sunlitindustry.ServerPerformanceConfig;
import com.society.sunlitindustry.performance.ae2.network.CompactCellNbt;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BasicCellInventory.class, remap = false)
public abstract class AE2CompactCellNbtMixin {
    @Shadow @Final private ItemStack i;
    @Unique private boolean sunlitindustry$unreadableCompactCell;

    @Unique
    private boolean sunlitindustry$quarantined() {
        if (!sunlitindustry$unreadableCompactCell && i.hasTag() && i.getTag().contains(CompactCellNbt.PAYLOAD)) {
            sunlitindustry$unreadableCompactCell = !CompactCellNbt.expand(i.getTag());
        }
        return sunlitindustry$unreadableCompactCell;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void sunlitindustry$readCompactKeys(IBasicCellItem cellType, ItemStack stack,
            ISaveProvider container, CallbackInfo ci) {
        // Always accept previously written cells, including when compact writing has been disabled.
        sunlitindustry$quarantined();
    }

    @Inject(method = {"insert", "extract"}, at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$preserveUnreadableCell(AEKey key, long amount, Actionable mode,
            IActionSource source, CallbackInfoReturnable<Long> cir) {
        if (sunlitindustry$quarantined()) cir.setReturnValue(0L);
    }

    @Inject(method = "persist", at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$expandBeforeNativeWrite(CallbackInfo ci) {
        // Repeated writes on the same live inventory must replace the previous compact payload.
        // Native loading can call saveChanges after failing to resolve keys; keep its writes off the original tag.
        if (sunlitindustry$quarantined()) ci.cancel();
    }

    @Inject(method = "persist", at = @At("RETURN"))
    private void sunlitindustry$writeCompactKeys(CallbackInfo ci) {
        if (ServerPerformanceConfig.ae2CompactCellNbt() && i.hasTag()) CompactCellNbt.compact(i.getTag());
    }
}
