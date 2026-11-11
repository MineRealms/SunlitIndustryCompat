package com.society.sunlitindustry.mixin.performance.tiangong.server;

import com.society.sunlitindustry.performance.tiangong.server.TianGongRecipeCacheOwner;
import com.society.sunlitindustry.performance.tiangong.server.TianGongRecipeSnapshots;
import com.society.sunlitindustry.performance.tiangong.server.TianGongServerRecipeCache;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeManager.class)
public abstract class TianGongRecipeManagerMixin implements TianGongRecipeCacheOwner {
    @Unique private TianGongRecipeSnapshots sunlitindustry$tiangongSnapshots;

    @Override
    public TianGongRecipeSnapshots sunlitindustry$tiangongRecipeSnapshots() {
        if (sunlitindustry$tiangongSnapshots == null) sunlitindustry$tiangongSnapshots = new TianGongRecipeSnapshots();
        return sunlitindustry$tiangongSnapshots;
    }

    @Override
    public void sunlitindustry$clearTiangongRecipeSnapshots() { sunlitindustry$tiangongSnapshots = null; }

    @Inject(method = {"apply", "replaceRecipes"}, at = @At("HEAD"))
    private void sunlitindustry$beforeRecipes(CallbackInfo ci) { TianGongServerRecipeCache.invalidate(); }

    @Inject(method = {"apply", "replaceRecipes"}, at = @At("RETURN"))
    private void sunlitindustry$afterRecipes(CallbackInfo ci) { TianGongServerRecipeCache.invalidate(); }
}
