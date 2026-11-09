package com.society.sunlitindustry.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** FTBBackups2 1.0.23 assumes every registered block has a default map colour. */
@Pseudo
@Mixin(targets = "net.creeperhost.ftbbackups.BackupHandler", remap = false)
public abstract class FTBBackupsPreviewMixin {
    @Redirect(
            method = "initPreview()V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;defaultMapColor()Lnet/minecraft/world/level/material/MapColor;",
                    remap = true),
            remap = false
    )
    private static MapColor sunlitindustry$previewMapColor(Block block) {
        MapColor color = block.defaultMapColor();
        if (color != null) return color;
        LogUtils.getLogger().warn("Using an empty backup preview colour for block {} with null defaultMapColor",
                BuiltInRegistries.BLOCK.getKey(block));
        return MapColor.NONE;
    }
}
