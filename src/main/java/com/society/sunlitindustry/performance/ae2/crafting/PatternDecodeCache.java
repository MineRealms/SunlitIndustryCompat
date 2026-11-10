package com.society.sunlitindustry.performance.ae2.crafting;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.crafting.pattern.AECraftingPattern;
import com.society.sunlitindustry.mixin.performance.ae2.AE2PatternEncodingAccess;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;

public final class PatternDecodeCache {
    private static final AtomicLong GENERATION = new AtomicLong();

    public static <T extends IPatternDetails> T decode(AEItemKey key, Level level, Class<T> type,
                                                      Supplier<T> loader) {
        if (!(level instanceof PatternCacheOwner owner)) return loader.get();
        if (!Ae2CraftingConfig.patterns()) {
            owner.sunlitindustry$patternCache().clear();
            return loader.get();
        }
        if (type == AECraftingPattern.class && !isStaticCraftingRecipe(key, level)) return loader.get();
        IPatternDetails decoded = owner.sunlitindustry$patternCache().get(new PatternCacheKey(key, type), GENERATION.get(),
                level.getRecipeManager(), Ae2CraftingConfig.maxPatterns(), loader::get);
        return decoded == null ? null : type.cast(decoded);
    }

    public static void invalidate() { GENERATION.incrementAndGet(); }

    public static void registerEvents() { MinecraftForge.EVENT_BUS.register(PatternDecodeCache.class); }

    private static boolean isStaticCraftingRecipe(AEItemKey key, Level level) {
        var recipeId = AE2PatternEncodingAccess.sunlitindustry$recipeId(key.getTag());
        var recipe = level.getRecipeManager().byType(RecipeType.CRAFTING).get(recipeId);
        if (recipe == null || recipe.getClass() != ShapedRecipe.class && recipe.getClass() != ShapelessRecipe.class) {
            return false;
        }
        // Custom recipes/ingredients may depend on state outside a recipe/tag reload epoch.
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.getClass() != Ingredient.class) return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void tagsUpdated(TagsUpdatedEvent event) { invalidate(); }

    @SubscribeEvent
    public static void levelUnloaded(LevelEvent.Unload event) {
        if (event.getLevel() instanceof PatternCacheOwner owner) owner.sunlitindustry$patternCache().clear();
    }

    private PatternDecodeCache() {}
}
