package com.society.sunlitindustry.mixin.performance.ae2.planner;

import appeng.api.crafting.IPatternDetails;
import appeng.crafting.inv.CraftingSimulationState;
import com.society.sunlitindustry.performance.ae2.planner.Ae2PlannerConfig;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.Map;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftingSimulationState.class, remap = false)
public abstract class AE2PlannerSimulationStateMixin {
    @Shadow @Final @Mutable private Map<IPatternDetails, Long> crafts;
    @SuppressWarnings("unchecked")
    @Inject(method = "addCrafting", at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$primitiveCraftCount(IPatternDetails details, long count, CallbackInfo ci) {
        if (!Ae2PlannerConfig.primitiveCounts()) return;
        Object2LongOpenHashMap<IPatternDetails> primitive;
        if (crafts instanceof Object2LongOpenHashMap<?> map) {
            primitive = (Object2LongOpenHashMap<IPatternDetails>) map;
        } else {
            primitive = new Object2LongOpenHashMap<>(crafts);
            crafts = primitive;
        }
        primitive.addTo(details, count);
        ci.cancel();
    }
}
