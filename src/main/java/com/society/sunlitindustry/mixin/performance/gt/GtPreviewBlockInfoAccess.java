package com.society.sunlitindustry.mixin.performance.gt;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.function.Consumer;

@Mixin(value = BlockInfo.class, remap = false)
public interface GtPreviewBlockInfoAccess {
    @Accessor("tag") CompoundTag sunlitindustry$tag();
    @Accessor("itemStack") ItemStack sunlitindustry$itemStack();
    @Accessor("postCreate") Consumer<BlockEntity> sunlitindustry$postCreate();
}
