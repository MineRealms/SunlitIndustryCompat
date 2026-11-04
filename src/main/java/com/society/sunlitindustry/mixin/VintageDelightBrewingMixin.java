package com.society.sunlitindustry.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * VintageDelight 0.1.6 registers its brewing mixes by reflecting on PotionBrewing.addMix under the
 * Mojang name. Production Forge runs SRG names (m_43513_), so every call throws NoSuchMethodException,
 * gets printed to STDERR and the mix is silently dropped. Route the call through a remapped invoker.
 */
@Pseudo
@Mixin(targets = "net.ribs.vintagedelight.mobEffects.CustomPotionBrewing", remap = false)
public abstract class VintageDelightBrewingMixin {
    @Inject(method = "addMix(Lnet/minecraft/world/item/alchemy/Potion;Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/alchemy/Potion;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void sunlitindustry$addMix(Potion input, Item ingredient, Potion output, CallbackInfo ci) {
        PotionBrewingInvoker.sunlitindustry$addMix(input, ingredient, output);
        ci.cancel();
    }
}
