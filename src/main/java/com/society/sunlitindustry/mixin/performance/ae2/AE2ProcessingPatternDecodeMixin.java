package com.society.sunlitindustry.mixin.performance.ae2;

import appeng.api.stacks.AEItemKey;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.crafting.pattern.ProcessingPatternItem;
import com.society.sunlitindustry.performance.ae2.crafting.PatternDecodeCache;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ProcessingPatternItem.class, remap = false)
public abstract class AE2ProcessingPatternDecodeMixin {
    @Redirect(method = "decode(Lappeng/api/stacks/AEItemKey;Lnet/minecraft/world/level/Level;)Lappeng/crafting/pattern/AEProcessingPattern;",
            at = @At(value = "NEW", target = "(Lappeng/api/stacks/AEItemKey;)Lappeng/crafting/pattern/AEProcessingPattern;"))
    private AEProcessingPattern sunlitindustry$decode(AEItemKey key, AEItemKey ignored, Level level) {
        return PatternDecodeCache.decode(key, level, AEProcessingPattern.class, () -> new AEProcessingPattern(key));
    }
}
