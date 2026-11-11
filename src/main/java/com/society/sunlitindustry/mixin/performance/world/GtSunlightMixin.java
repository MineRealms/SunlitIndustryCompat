package com.society.sunlitindustry.mixin.performance.world;

import com.gregtechceu.gtceu.utils.GTUtil;
import com.society.sunlitindustry.performance.world.GtSunlightQuery;
import com.society.sunlitindustry.performance.world.WorldPerformanceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GTUtil.class, remap = false)
public abstract class GtSunlightMixin {
    @Inject(method = "canSeeSunClearly", at = @At("HEAD"), cancellable = true)
    private static void sunlitindustry$sunlight(Level level, BlockPos pos,
            CallbackInfoReturnable<Boolean> cir) {
        if (WorldPerformanceConfig.sunlight()) cir.setReturnValue(GtSunlightQuery.canSeeSunClearly(level, pos));
    }
}
