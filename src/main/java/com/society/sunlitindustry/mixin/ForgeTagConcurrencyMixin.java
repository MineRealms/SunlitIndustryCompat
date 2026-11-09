package com.society.sunlitindustry.mixin;

import org.spongepowered.asm.mixin.Mixin;

/** The plugin synchronizes Forge's copy-on-write tag creation and binding methods. */
@Mixin(targets = "net.minecraftforge.registries.ForgeRegistryTagManager", remap = false)
public abstract class ForgeTagConcurrencyMixin {}
