package com.society.sunlitindustry.mixin.performance.ae2;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AECraftingPattern;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/** Cached crafting patterns have mutable scratch grids; all scratch-grid users share one lock. */
@Mixin(value = AECraftingPattern.class, remap = false)
public abstract class AE2CraftingPatternStateMixin {
    @Shadow @Final public boolean canSubstitute;
    @Shadow @Final private CraftingRecipe recipe;
    @Shadow @Final private CraftingContainer testFrame;
    @Shadow @Final private CraftingContainer specialRecipeTestFrame;
    @Shadow @Final private GenericStack[] sparseInputs;
    @Shadow @Final private ItemStack output;
    @Shadow private Boolean getTestResult(int slot, AEItemKey what) { throw new AssertionError(); }
    @Shadow private void setTestResult(int slot, AEItemKey what, boolean result) { throw new AssertionError(); }
    @Shadow public abstract GenericStack getValidFluid(int slot);

    /**
     * @author Sunlit
     * @reason Preserve native validation while protecting shared scratch state and restoring it on failure.
     */
    @Overwrite
    public synchronized boolean isItemValid(int slot, AEItemKey key, Level level) {
        if (!canSubstitute) {
            return sparseInputs[slot] == null && key == null
                    || sparseInputs[slot] != null && sparseInputs[slot].what().equals(key);
        }
        if (key == null) return sparseInputs[slot] == null;
        Boolean result = getTestResult(slot, key);
        if (result != null) return result;
        ItemStack previous = testFrame.removeItemNoUpdate(slot);
        try {
            testFrame.setItem(slot, key.toStack());
            boolean matches = recipe.matches(testFrame, level)
                    && ItemStack.matches(output, recipe.assemble(testFrame, level.registryAccess()));
            setTestResult(slot, key, matches);
            return matches;
        } finally {
            testFrame.setItem(slot, previous);
        }
    }

    /**
     * @author Sunlit
     * @reason Serialize remainder checks against substitution checks using the same scratch grid.
     */
    @Overwrite
    private synchronized ItemStack getRecipeRemainder(int slot, AEItemKey key) {
        ItemStack previous = testFrame.removeItemNoUpdate(slot);
        try {
            testFrame.setItem(slot, key.toStack());
            return recipe.getRemainingItems(testFrame).get(slot);
        } finally {
            testFrame.setItem(slot, previous);
        }
    }

    /**
     * @author Sunlit
     * @reason Protect the special-recipe scratch grid when a decoded pattern is shared.
     */
    @Overwrite
    public synchronized ItemStack assemble(Container container, Level level) {
        if (canSubstitute && recipe.isSpecial()) {
            specialRecipeTestFrame.clearContent();
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack item = container.getItem(slot);
                GenericStack stack = GenericStack.unwrapItemStack(item);
                GenericStack fluid = getValidFluid(slot);
                if (stack != null && fluid != null && fluid.equals(stack)) {
                    specialRecipeTestFrame.setItem(slot, ((AEItemKey) sparseInputs[slot].what()).toStack());
                } else {
                    specialRecipeTestFrame.setItem(slot, item.copy());
                }
            }
            return recipe.assemble(specialRecipeTestFrame, level.registryAccess());
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack item = container.getItem(slot);
            GenericStack stack = GenericStack.unwrapItemStack(item);
            GenericStack fluid = getValidFluid(slot);
            if (stack != null && fluid != null && fluid.equals(stack)
                    || isItemValid(slot, AEItemKey.of(item), level)) continue;
            return ItemStack.EMPTY;
        }
        return output;
    }
}
