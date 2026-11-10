package com.society.sunlitindustry.mixin.performance.gt;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.society.sunlitindustry.performance.gt.GtAdvancedConfig;
import com.society.sunlitindustry.performance.gt.GtDormantPart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelChunk.class)
public abstract class GtDormantChunkTickerMixin {
    @Redirect(method = "updateBlockEntityTicker", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getTicker(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/BlockEntityType;)Lnet/minecraft/world/level/block/entity/BlockEntityTicker;"))
    private <T extends BlockEntity> BlockEntityTicker<T> sunlitindustry$ticker(BlockState state, Level level,
            BlockEntityType<T> type, T blockEntity) {
        if (!level.isClientSide && GtAdvancedConfig.dormantPartTick()
                && blockEntity instanceof MetaMachineBlockEntity holder
                && holder.getMetaMachine() instanceof GtDormantPart dormant
                && dormant.sunlitindustry$isDormant()) return null;
        return state.getTicker(level, type);
    }
}
