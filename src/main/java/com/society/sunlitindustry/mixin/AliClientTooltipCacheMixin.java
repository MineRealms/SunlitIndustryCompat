package com.society.sunlitindustry.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** ALI empty tooltip nodes belong to the client cache during EMI registration. */
@Pseudo
@Mixin(targets = "com.yanny.aci.tooltip.TooltipNode", remap = false)
public abstract class AliClientTooltipCacheMixin {}
