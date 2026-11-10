package com.society.sunlitindustry.mixin.performance.gt;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.society.sunlitindustry.performance.gt.GtAdvancedConfig;
import com.society.sunlitindustry.performance.gt.GtShapeCacheOwner;
import com.society.sunlitindustry.performance.gt.GtShapeCaches;
import com.society.sunlitindustry.performance.gt.GtShapeSnapshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
import java.util.function.Supplier;

@Mixin(value = MultiblockMachineDefinition.class, remap = false)
public abstract class GtMatchingShapesMixin implements GtShapeCacheOwner {
    @Unique private volatile GtShapeSnapshot sunlitindustry$shapes;

    @Inject(method = "getMatchingShapes", at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$cached(CallbackInfoReturnable<List<MultiblockShapeInfo>> cir) {
        var snapshot = sunlitindustry$shapes;
        if (GtAdvancedConfig.shapes() && snapshot != null) cir.setReturnValue(snapshot.copy());
    }

    @Inject(method = "getMatchingShapes", at = @At("RETURN"))
    private void sunlitindustry$capture(CallbackInfoReturnable<List<MultiblockShapeInfo>> cir) {
        if (!GtAdvancedConfig.shapes() || sunlitindustry$shapes != null) return;
        var snapshot = GtShapeSnapshot.capture(cir.getReturnValue());
        if (snapshot != null) {
            sunlitindustry$shapes = snapshot;
            GtShapeCaches.track(this);
        }
    }

    @Inject(method = "setPatternFactory", at = @At("HEAD"))
    private void sunlitindustry$pattern(Supplier<BlockPattern> factory, CallbackInfo ci) {
        sunlitindustry$clearShapes();
    }

    @Inject(method = "setShapes", at = @At("HEAD"))
    private void sunlitindustry$supplier(Supplier<List<MultiblockShapeInfo>> shapes, CallbackInfo ci) {
        sunlitindustry$clearShapes();
    }

    @Override
    public void sunlitindustry$clearShapes() { sunlitindustry$shapes = null; }
}
