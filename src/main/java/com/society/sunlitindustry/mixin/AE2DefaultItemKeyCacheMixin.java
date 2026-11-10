package com.society.sunlitindustry.mixin;

import appeng.api.stacks.AEItemKey;
import com.society.sunlitindustry.AE2DefaultItemKeyHolder;
import com.society.sunlitindustry.ServerPerformanceConfig;
import com.society.sunlitindustry.ServerPerformanceStats;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AEItemKey.class, remap = false)
public abstract class AE2DefaultItemKeyCacheMixin {
    @Inject(method = "of(Lnet/minecraft/world/level/ItemLike;)Lappeng/api/stacks/AEItemKey;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void sunlitindustry$getCachedDefaultKey(ItemLike itemLike,
            CallbackInfoReturnable<AEItemKey> cir) {
        if (!ServerPerformanceConfig.ae2DefaultKeyCache() || itemLike == null) return;
        Item item = itemLike.asItem();
        if (!(item instanceof AE2DefaultItemKeyHolder holder)) return;
        AEItemKey key = holder.sunlitindustry$getDefaultAEItemKey();
        if (key == null) {
            ServerPerformanceStats.record(ServerPerformanceStats.Event.AE2_DEFAULT_KEY_MISS);
            return;
        }
        ServerPerformanceStats.record(ServerPerformanceStats.Event.AE2_DEFAULT_KEY_HIT);
        cir.setReturnValue(key);
    }

    @Inject(method = "of(Lnet/minecraft/world/level/ItemLike;)Lappeng/api/stacks/AEItemKey;",
            at = @At("RETURN"), remap = false)
    private static void sunlitindustry$storeDefaultKey(ItemLike itemLike,
            CallbackInfoReturnable<AEItemKey> cir) {
        if (!ServerPerformanceConfig.ae2DefaultKeyCache() || itemLike == null) return;
        Item item = itemLike.asItem();
        if (!(item instanceof AE2DefaultItemKeyHolder holder)) return;
        AEItemKey key = cir.getReturnValue();
        if (key == null) return;
        holder.sunlitindustry$setDefaultAEItemKey(key);
        ServerPerformanceStats.record(ServerPerformanceStats.Event.AE2_DEFAULT_KEY_STORED);
    }
}
