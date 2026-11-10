package com.society.sunlitindustry;

import com.gregtechceu.gtceu.api.capability.IElectricItem;
import mekanism.api.Action;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.math.FloatingLong;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class MekEuEnergyAdapter implements IElectricItem {
    private final IStrictEnergyHandler storage;
    private final Supplier<FloatingLong> joulesPerEu;
    private final BooleanSupplier usable;
    private final int tier;
    private final boolean battery;

    public MekEuEnergyAdapter(IStrictEnergyHandler storage, Supplier<FloatingLong> joulesPerEu,
                              int tier, boolean battery, BooleanSupplier usable) {
        this.storage = storage;
        this.joulesPerEu = joulesPerEu;
        this.tier = Math.max(0, Math.min(14, tier));
        this.battery = battery;
        this.usable = usable;
    }

    @Override
    public boolean canProvideChargeExternally() { return usable.getAsBoolean() && battery; }

    @Override
    public boolean chargeable() { return usable.getAsBoolean() && getMaxCharge() > 0; }

    @Override
    public long charge(long amount, int chargerTier, boolean ignoreTransferLimit, boolean simulate) {
        if (!usable.getAsBoolean() || amount <= 0 || chargerTier < tier) return 0;
        FloatingLong unit = joulesPerEu.get();
        if (unit.isZero()) return 0;
        long requested = Math.min(amount, sum(true).subtract(sum(false)).divideToLong(unit));
        if (!ignoreTransferLimit) requested = Math.min(requested, getTransferLimit());
        if (requested <= 0) return 0;
        FloatingLong energy = unit.multiply(requested);
        // Transfer only whole EU; probing a nearly-full container must not create fractional energy.
        long accepted = Math.min(requested,
                energy.subtract(storage.insertEnergy(energy, Action.SIMULATE)).divideToLong(unit));
        if (simulate || accepted <= 0) return accepted;
        energy = unit.multiply(accepted);
        return energy.subtract(storage.insertEnergy(energy, Action.EXECUTE)).divideToLong(unit);
    }

    @Override
    public long discharge(long amount, int dischargerTier, boolean ignoreTransferLimit,
                          boolean externally, boolean simulate) {
        if (!usable.getAsBoolean() || amount <= 0 || dischargerTier < tier || (externally && !battery)) return 0;
        FloatingLong unit = joulesPerEu.get();
        if (unit.isZero()) return 0;
        long requested = Math.min(amount, sum(false).divideToLong(unit));
        if (!ignoreTransferLimit) requested = Math.min(requested, getTransferLimit());
        if (requested <= 0) return 0;
        long available = Math.min(requested,
                storage.extractEnergy(unit.multiply(requested), Action.SIMULATE).divideToLong(unit));
        if (simulate || available <= 0) return available;
        return storage.extractEnergy(unit.multiply(available), Action.EXECUTE).divideToLong(unit);
    }

    private FloatingLong sum(boolean maximum) {
        FloatingLong total = FloatingLong.create(0);
        for (int i = 0; i < storage.getEnergyContainerCount(); i++) {
            total = total.plusEqual(maximum ? storage.getMaxEnergy(i) : storage.getEnergy(i));
        }
        return total;
    }

    private long inEu(boolean maximum) {
        if (!usable.getAsBoolean()) return 0;
        FloatingLong unit = joulesPerEu.get();
        return unit.isZero() ? 0 : sum(maximum).divideToLong(unit);
    }

    @Override
    public long getTransferLimit() { return 8L << (tier * 2); }
    @Override
    public long getMaxCharge() { return inEu(true); }
    @Override
    public long getCharge() { return inEu(false); }
    @Override
    public int getTier() { return tier; }
}
