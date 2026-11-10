package com.society.sunlitindustry.mixin.performance.ae2.planner;

import appeng.crafting.CraftingCalculation;
import appeng.crafting.CraftingTreeNode;
import appeng.crafting.CraftingTreeProcess;
import com.society.sunlitindustry.performance.ae2.planner.Ae2PlannerConfig;
import com.society.sunlitindustry.performance.ae2.planner.PlannerGraphEpoch;
import java.util.ArrayList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftingTreeNode.class, remap = false)
public abstract class AE2PlannerGraphMemoMixin {
    @Shadow @Final private CraftingCalculation job;
    @Shadow private ArrayList<CraftingTreeProcess> nodes;
    @Unique private long sunlitindustry$countGeneration = Long.MIN_VALUE;
    @Unique private long sunlitindustry$pathsGeneration = Long.MIN_VALUE;
    @Unique private long sunlitindustry$nodeCount;
    @Unique private boolean sunlitindustry$multiplePaths;

    @Inject(method = "buildChildPatterns", at = @At("HEAD"))
    private void sunlitindustry$lazyExpansion(CallbackInfo ci) {
        // Advance even while caching is disabled, so live configuration changes cannot revive stale metadata.
        if (nodes == null) ((PlannerGraphEpoch) job).sunlitindustry$graphChanged();
    }

    @Inject(method = "getNodeCount", at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$count(CallbackInfoReturnable<Long> cir) {
        if (Ae2PlannerConfig.graphMetadata()
                && sunlitindustry$countGeneration == ((PlannerGraphEpoch) job).sunlitindustry$graphEpoch()) {
            cir.setReturnValue(sunlitindustry$nodeCount);
        }
    }

    @Inject(method = "getNodeCount", at = @At("RETURN"))
    private void sunlitindustry$rememberCount(CallbackInfoReturnable<Long> cir) {
        if (!Ae2PlannerConfig.graphMetadata()) return;
        sunlitindustry$nodeCount = cir.getReturnValue();
        sunlitindustry$countGeneration = ((PlannerGraphEpoch) job).sunlitindustry$graphEpoch();
    }

    @Inject(method = "hasMultiplePaths", at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$paths(CallbackInfoReturnable<Boolean> cir) {
        if (Ae2PlannerConfig.graphMetadata()
                && sunlitindustry$pathsGeneration == ((PlannerGraphEpoch) job).sunlitindustry$graphEpoch()) {
            cir.setReturnValue(sunlitindustry$multiplePaths);
        }
    }

    @Inject(method = "hasMultiplePaths", at = @At("RETURN"))
    private void sunlitindustry$rememberPaths(CallbackInfoReturnable<Boolean> cir) {
        if (!Ae2PlannerConfig.graphMetadata()) return;
        sunlitindustry$multiplePaths = cir.getReturnValue();
        sunlitindustry$pathsGeneration = ((PlannerGraphEpoch) job).sunlitindustry$graphEpoch();
    }
}
