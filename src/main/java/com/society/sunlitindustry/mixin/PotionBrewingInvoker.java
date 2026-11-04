package com.society.sunlitindustry.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Typed access to the private vanilla brewing registration used by VintageDelightBrewingMixin. */
@Mixin(PotionBrewing.class)
public interface PotionBrewingInvoker {
    @Invoker("addMix")
    static void sunlitindustry$addMix(Potion input, Item ingredient, Potion output) {
        throw new AssertionError("mixin invoker not applied");
    }
}
