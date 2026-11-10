package com.society.sunlitindustry;

import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.capability.compat.FeCompat;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.item.ItemEnergized;
import mekanism.common.item.block.ItemBlockEnergyCube;
import mekanism.common.item.gear.ItemFreeRunners;
import mekanism.common.item.gear.ItemMekaSuitArmor;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MekEuItemCapabilities {
    private static final ResourceLocation KEY = new ResourceLocation("sunlitindustry", "mek_electric_item");

    public static void register() {
        MinecraftForge.EVENT_BUS.addGenericListener(ItemStack.class, MekEuItemCapabilities::attach);
    }

    private static void attach(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        var item = stack.getItem();
        if (!(item instanceof ItemEnergized || item instanceof ItemMekaSuitArmor
                || item instanceof ItemFreeRunners || item instanceof ItemBlockEnergyCube)) return;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null || id.getPath().equals("creative_energy_cube")) return;
        int tier;
        boolean battery = id.equals(new ResourceLocation("mekanism", "energy_tablet"));
        if (item instanceof ItemBlockEnergyCube cube) {
            battery = true;
            tier = MekItemEnergyConfig.cubeTier(cube.getTier().ordinal());
        } else if (item instanceof ItemMekaSuitArmor) {
            tier = id.getNamespace().equals("zmod") ? MekItemEnergyConfig.voidSuitTier() : MekItemEnergyConfig.suitTier();
        } else {
            tier = battery ? MekItemEnergyConfig.tabletTier() : MekItemEnergyConfig.toolTier();
        }
        var provider = new Provider(stack, tier, battery);
        event.addCapability(KEY, provider);
        event.addListener(provider::invalidate);
    }

    private static final class Provider implements ICapabilityProvider {
        private final ItemStack stack;
        private final int tier;
        private final boolean battery;
        private LazyOptional<IElectricItem> electric = LazyOptional.empty();
        private boolean valid = true;

        private Provider(ItemStack stack, int tier, boolean battery) {
            this.stack = stack;
            this.tier = tier;
            this.battery = battery;
        }

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
            if (!valid || capability != GTCapability.CAPABILITY_ELECTRIC_ITEM || !MekItemEnergyConfig.enabled()) {
                return LazyOptional.empty();
            }
            if (!electric.isPresent()) {
                // Resolve after attachment: querying the stack while gathering providers is recursive.
                var nativeStorage = stack.getCapability(Capabilities.STRICT_ENERGY).resolve().orElse(null);
                if (nativeStorage == null || nativeStorage.getEnergyContainerCount() == 0) return LazyOptional.empty();
                electric = LazyOptional.of(() -> new MekEuEnergyAdapter(nativeStorage,
                        () -> EnergyUnit.FORGE_ENERGY.convertFrom(Math.max(1, FeCompat.ratio(false))),
                        tier, battery, () -> valid && stack.getCount() == 1 && MekItemEnergyConfig.enabled()));
            }
            return electric.cast();
        }

        private void invalidate() {
            valid = false;
            electric.invalidate();
        }
    }

    private MekEuItemCapabilities() {}
}
