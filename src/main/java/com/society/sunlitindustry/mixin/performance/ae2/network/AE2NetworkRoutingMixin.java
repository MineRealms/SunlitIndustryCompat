package com.society.sunlitindustry.mixin.performance.ae2.network;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.me.storage.NetworkStorage;
import com.society.sunlitindustry.performance.ae2.network.AE2NetworkConfig;
import com.society.sunlitindustry.performance.ae2.network.PendingRemovalIndex;
import com.society.sunlitindustry.performance.ae2.network.PriorityRoutingSnapshot;
import java.util.List;
import java.util.NavigableMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NetworkStorage.class, remap = false)
public abstract class AE2NetworkRoutingMixin {
    @Shadow @Final private NavigableMap<Integer, List<MEStorage>> priorityInventory;
    @Shadow @Final private List<MEStorage> secondPassInventories;
    @Shadow private List<?> queuedOperations;
    @Shadow private boolean mountsInUse;
    @Shadow protected abstract boolean isQueuedForRemoval(MEStorage inventory);
    @Shadow protected abstract void flushQueuedOperations();

    @Unique private PriorityRoutingSnapshot<MEStorage> sunlitindustry$routes;
    @Unique private PendingRemovalIndex<MEStorage> sunlitindustry$removals;
    @Unique private boolean sunlitindustry$indexedOperation;

    @Unique
    private boolean sunlitindustry$enabled() {
        if (AE2NetworkConfig.routing()) return true;
        sunlitindustry$routes = null;
        if (!mountsInUse && sunlitindustry$removals != null) sunlitindustry$removals.clear();
        return false;
    }

    @Unique
    private PriorityRoutingSnapshot<MEStorage> sunlitindustry$routes() {
        if (sunlitindustry$routes == null) {
            sunlitindustry$routes = PriorityRoutingSnapshot.copyOf(priorityInventory);
        }
        return sunlitindustry$routes;
    }

    @Inject(method = "mount", at = @At("HEAD"))
    private void sunlitindustry$invalidateMount(int priority, MEStorage inventory, CallbackInfo ci) {
        sunlitindustry$routes = null;
    }

    @Inject(method = "unmount", at = @At("HEAD"))
    private void sunlitindustry$invalidateUnmount(MEStorage inventory, CallbackInfo ci) {
        sunlitindustry$routes = null;
        if (sunlitindustry$indexedOperation) {
            if (sunlitindustry$removals == null) sunlitindustry$removals = new PendingRemovalIndex<>();
            sunlitindustry$removals.add(inventory);
        }
    }

    @Inject(method = "flushQueuedOperations", at = @At("HEAD"))
    private void sunlitindustry$clearRemovalIndex(CallbackInfo ci) {
        if (sunlitindustry$removals != null) sunlitindustry$removals.clear();
    }

    @Inject(method = "isQueuedForRemoval", at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$indexedRemoval(MEStorage inventory, CallbackInfoReturnable<Boolean> cir) {
        // A native enumeration can leave an older pending queue. Let AE2 inspect that queue itself.
        if (sunlitindustry$indexedOperation) {
            cir.setReturnValue(sunlitindustry$removals != null && sunlitindustry$removals.contains(inventory));
        }
    }

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$routeInsert(AEKey what, long amount, Actionable mode, IActionSource source,
            CallbackInfoReturnable<Long> cir) {
        if (!sunlitindustry$enabled()) return;
        if (mountsInUse) {
            cir.setReturnValue(0L);
            return;
        }
        var routes = sunlitindustry$routes();
        long remaining = amount;
        sunlitindustry$indexedOperation = queuedOperations == null;
        mountsInUse = true;
        try {
            for (int group = 0; group < routes.groupCount(); group++) {
                secondPassInventories.clear();
                for (int slot = 0; slot < routes.groupSize(group) && remaining > 0; slot++) {
                    MEStorage inventory = routes.get(group, slot);
                    if (isQueuedForRemoval(inventory)) continue;
                    if (inventory.isPreferredStorageFor(what, source)) {
                        remaining -= inventory.insert(what, remaining, mode, source);
                    } else {
                        secondPassInventories.add(inventory);
                    }
                }
                for (int slot = 0; slot < secondPassInventories.size() && remaining > 0; slot++) {
                    MEStorage inventory = secondPassInventories.get(slot);
                    if (!isQueuedForRemoval(inventory)) {
                        remaining -= inventory.insert(what, remaining, mode, source);
                    }
                }
            }
        } finally {
            mountsInUse = false;
            sunlitindustry$indexedOperation = false;
            if (sunlitindustry$removals != null) sunlitindustry$removals.clear();
        }
        flushQueuedOperations();
        cir.setReturnValue(amount - remaining);
    }

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$routeExtract(AEKey what, long amount, Actionable mode, IActionSource source,
            CallbackInfoReturnable<Long> cir) {
        if (!sunlitindustry$enabled()) return;
        if (mountsInUse) {
            cir.setReturnValue(0L);
            return;
        }
        var routes = sunlitindustry$routes();
        long extracted = 0;
        sunlitindustry$indexedOperation = queuedOperations == null;
        mountsInUse = true;
        try {
            for (int group = routes.groupCount() - 1; group >= 0; group--) {
                for (int slot = 0; slot < routes.groupSize(group) && extracted < amount; slot++) {
                    MEStorage inventory = routes.get(group, slot);
                    if (!isQueuedForRemoval(inventory)) {
                        extracted += inventory.extract(what, amount - extracted, mode, source);
                    }
                }
            }
        } finally {
            mountsInUse = false;
            sunlitindustry$indexedOperation = false;
            if (sunlitindustry$removals != null) sunlitindustry$removals.clear();
        }
        flushQueuedOperations();
        cir.setReturnValue(extracted);
    }

    @Inject(method = "getAvailableStacks", at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$routeEnumeration(KeyCounter out, CallbackInfo ci) {
        if (!sunlitindustry$enabled()) return;
        if (mountsInUse) {
            ci.cancel();
            return;
        }
        var routes = sunlitindustry$routes();
        mountsInUse = true;
        try {
            for (int group = 0; group < routes.groupCount(); group++) {
                for (int slot = 0; slot < routes.groupSize(group); slot++) {
                    routes.get(group, slot).getAvailableStacks(out);
                }
            }
        } finally {
            mountsInUse = false;
        }
        ci.cancel();
    }
}
