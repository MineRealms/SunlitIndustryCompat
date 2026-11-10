package com.society.sunlitindustry;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.AssemblyLineMachine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;

public final class VoidMekaSuitAssembly {
    public static final String SOURCE = "sunlitindustry.armor_source";
    private static final Set<String> PARTS = Set.of("helmet", "bodyarmor", "pants", "boots");

    public static GTRecipe prepare(RecipeLogic logic, GTRecipe recipe) {
        if (!(logic.getMachine() instanceof AssemblyLineMachine machine)
                || recipe.recipeType != GTRecipeTypes.ASSEMBLY_LINE_RECIPES) return recipe;
        String source = recipe.data.getString(SOURCE);
        String part = source.replace("mekanism:mekasuit_", "");
        if (!PARTS.contains(part) || !source.equals("mekanism:mekasuit_" + part)) return recipe;
        var outputs = recipe.outputs.get(ItemRecipeCapability.CAP);
        if (outputs == null || outputs.size() != 1) return recipe;
        Content old = outputs.get(0);
        var samples = ItemRecipeCapability.CAP.of(old.content).getItems();
        String target = "zmod:void_mekasuit_" + part;
        if (samples.length != 1 || !target.equals(String.valueOf(ForgeRegistries.ITEMS.getKey(samples[0].getItem())))) {
            return recipe;
        }
        for (var handler : machine.getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP)) {
            if (!handler.shouldSearchContent()) continue;
            for (var content : handler.getContents()) {
                if (!(content instanceof ItemStack stack) || stack.isEmpty()
                        || !source.equals(String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())))) continue;
                CompoundTag serialized = stack.save(new CompoundTag());
                serialized.putString("id", target);
                serialized.putByte("Count", (byte) 1);
                ItemStack upgraded = ItemStack.of(serialized);
                // The per-run recipe is persisted by GT RecipeLogic, including the copied output NBT.
                GTRecipe copy = recipe.copy();
                copy.outputs.put(ItemRecipeCapability.CAP, List.of(new Content(SizedIngredient.create(upgraded),
                        old.chance, old.maxChance, old.tierChanceBoost)));
                return copy;
            }
        }
        return recipe;
    }

    private VoidMekaSuitAssembly() {}
}
