package com.society.sunlitindustry.mixin.performance.gt;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelChunk.class)
public interface GtChunkTickerAccess {
    @Invoker("updateBlockEntityTicker")
    <T extends BlockEntity> void sunlitindustry$refreshTicker(T blockEntity);
}
