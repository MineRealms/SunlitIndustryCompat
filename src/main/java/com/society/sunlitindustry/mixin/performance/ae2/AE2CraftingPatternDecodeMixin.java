package com.society.sunlitindustry.mixin.performance.ae2;

import appeng.api.stacks.AEItemKey;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.crafting.pattern.CraftingPatternItem;
import com.society.sunlitindustry.performance.ae2.crafting.PatternDecodeCache;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CraftingPatternItem.class, remap = false)
public abstract class AE2CraftingPatternDecodeMixin {
    @Redirect(method = "decode(Lappeng/api/stacks/AEItemKey;Lnet/minecraft/world/level/Level;)Lappeng/crafting/pattern/AECraftingPattern;",
            at = @At(value = "NEW", target = "(Lappeng/api/stacks/AEItemKey;Lnet/minecraft/world/level/Level;)Lappeng/crafting/pattern/AECraftingPattern;"))
    private AECraftingPattern sunlitindustry$decode(AEItemKey key, Level level) {
        return PatternDecodeCache.decode(key, level, AECraftingPattern.class, () -> new AECraftingPattern(key, level));
    }
}
