package com.society.sunlitindustry.mixin.performance.tiangong.server;

import com.society.sunlitindustry.performance.tiangong.server.TianGongServerRecipeCache;
import java.util.List;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zmod.compat.RecipeHolder;

@Pseudo
@Mixin(targets = "zmod.compat.Recipes", remap = false)
public abstract class TianGongRecipeListsMixin {
    @Inject(method = "all", at = @At("HEAD"), cancellable = true, remap = false)
    private static <T extends Recipe<?>> void sunlitindustry$cachedLists(RecipeManager manager, RecipeType<T> type,
            CallbackInfoReturnable<List<RecipeHolder<T>>> cir) {
        var cached = TianGongServerRecipeCache.cached(manager, type);
        if (cached != null) cir.setReturnValue(cached);
    }

    @Inject(method = "all", at = @At("RETURN"), remap = false)
    private static <T extends Recipe<?>> void sunlitindustry$rememberLists(RecipeManager manager, RecipeType<T> type,
            CallbackInfoReturnable<List<RecipeHolder<T>>> cir) {
        TianGongServerRecipeCache.remember(manager, type, cir.getReturnValue());
    }
}
