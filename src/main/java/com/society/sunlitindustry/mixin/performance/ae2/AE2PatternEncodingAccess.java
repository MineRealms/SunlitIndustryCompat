package com.society.sunlitindustry.mixin.performance.ae2;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "appeng.crafting.pattern.CraftingPatternEncoding", remap = false)
public interface AE2PatternEncodingAccess {
    @Invoker("getRecipeId")
    static ResourceLocation sunlitindustry$recipeId(CompoundTag tag) { throw new AssertionError(); }
}
