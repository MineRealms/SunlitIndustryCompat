package com.society.sunlitindustry.mixin;

import com.society.sunlitindustry.NuclearBalanceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "zmod.machine.wanxiang.CageStructure", remap = false)
public abstract class WanxiangFormationMixin {
    @Unique
    private static final String SUNLIT_FORM_METHOD =
          "form(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Z";

    @Inject(method = SUNLIT_FORM_METHOD, at = @At("HEAD"), cancellable = true, remap = false)
    private static void sunlitindustry$requireCore(ServerPlayer player, BlockPos clicked,
          ItemStack wand, Direction face, CallbackInfoReturnable<Boolean> callback) {
        if (NuclearBalanceConfig.requireWanxiangCore() && sunlitindustry$core(player).isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.sunlitindustry.wanxiang_core_required"));
            callback.setReturnValue(false);
        }
    }

    @Inject(method = SUNLIT_FORM_METHOD, at = @At("RETURN"), remap = false)
    private static void sunlitindustry$payAfterFormation(ServerPlayer player, BlockPos clicked,
          ItemStack wand, Direction face, CallbackInfoReturnable<Boolean> callback) {
        if (NuclearBalanceConfig.requireWanxiangCore() && Boolean.TRUE.equals(callback.getReturnValue())) {
            sunlitindustry$core(player).shrink(1);
            player.getInventory().setChanged();
        }
    }

    @Unique
    private static ItemStack sunlitindustry$core(ServerPlayer player) {
        var id = new ResourceLocation("society", "wanxiang_formation_core");
        if (!ForgeRegistries.ITEMS.containsKey(id)) return ItemStack.EMPTY;
        var item = ForgeRegistries.ITEMS.getValue(id);
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) return stack;
        }
        return ItemStack.EMPTY;
    }
}
