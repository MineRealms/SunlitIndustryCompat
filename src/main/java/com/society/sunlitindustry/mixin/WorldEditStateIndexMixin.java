package com.society.sunlitindustry.mixin;

import com.society.sunlitindustry.OrderedStateIndex;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/** Share the state index instead of materialising a neighbour table for every WorldEdit block state. */
@Pseudo
@Mixin(targets = "com.sk89q.worldedit.world.block.BlockState", remap = false)
public abstract class WorldEditStateIndexMixin {
    @Shadow @Final private Map<Object, Object> values;
    @Unique private OrderedStateIndex<Object> sunlitindustry$stateIndex;
    @Unique private static final ThreadLocal<OrderedStateIndex<Object>> sunlitindustry$index = new ThreadLocal<>();
    @Unique private static final ThreadLocal<Map<?, ?>> sunlitindustry$source = new ThreadLocal<>();

    @Inject(method = "populate(Ljava/util/Map;)V", at = @At("HEAD"), cancellable = true, remap = false)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void sunlitindustry$shareStateIndex(Map states, CallbackInfo ci) {
        if (states.size() < 64) return;
        var index = sunlitindustry$index.get();
        if (sunlitindustry$source.get() != states || index == null) {
            index = new OrderedStateIndex<>(states);
            sunlitindustry$source.set(states);
            sunlitindustry$index.set(index);
        }
        sunlitindustry$stateIndex = index;
        ci.cancel();
    }

    @Inject(method = "with(Lcom/sk89q/worldedit/registry/state/Property;Ljava/lang/Object;)Lcom/sk89q/worldedit/world/block/BlockState;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void sunlitindustry$lookupNeighbour(@Coerce Object property, Object value, CallbackInfoReturnable<Object> cir) {
        if (sunlitindustry$stateIndex == null) return;
        Object state = sunlitindustry$stateIndex.with(values, property, value);
        cir.setReturnValue(state == null ? this : state);
    }

    @Inject(method = "generateStateMap", at = @At("RETURN"), remap = false)
    private static void sunlitindustry$releaseIndex(CallbackInfoReturnable<Map<?, ?>> cir) {
        sunlitindustry$source.remove();
        sunlitindustry$index.remove();
    }
}
