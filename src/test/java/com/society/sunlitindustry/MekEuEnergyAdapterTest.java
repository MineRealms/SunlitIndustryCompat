package com.society.sunlitindustry;

import mekanism.api.Action;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.math.FloatingLong;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MekEuEnergyAdapterTest {
    private static final class Storage implements IStrictEnergyHandler {
        private FloatingLong energy = FloatingLong.ZERO;
        private final FloatingLong capacity;
        private final FloatingLong rate;

        private Storage(long capacity, long rate) {
            this.capacity = FloatingLong.create(capacity);
            this.rate = FloatingLong.create(rate);
        }

        public int getEnergyContainerCount() { return 1; }
        public FloatingLong getEnergy(int i) { return energy; }
        public void setEnergy(int i, FloatingLong value) { energy = value.copy(); }
        public FloatingLong getMaxEnergy(int i) { return capacity; }
        public FloatingLong getNeededEnergy(int i) { return capacity.subtract(energy); }
        public FloatingLong insertEnergy(int i, FloatingLong amount, Action action) {
            FloatingLong inserted = amount.min(rate).min(getNeededEnergy(i));
            if (action.execute()) energy = energy.add(inserted);
            return amount.subtract(inserted);
        }
        public FloatingLong extractEnergy(int i, FloatingLong amount, Action action) {
            FloatingLong extracted = amount.min(rate).min(energy);
            if (action.execute()) energy = energy.subtract(extracted);
            return extracted;
        }
    }

    private static MekEuEnergyAdapter adapter(Storage storage, int tier, boolean battery) {
        return new MekEuEnergyAdapter(storage, () -> FloatingLong.create(10), tier, battery, () -> true);
    }

    @Test
    void chargeAndDischargeShareNativeStorageAndSimulationDoesNotWrite() {
        Storage storage = new Storage(10000, 10000);
        var item = adapter(storage, 1, true);
        assertEquals(100, item.charge(100, 1, true, true));
        assertTrue(storage.energy.isZero());
        assertEquals(100, item.charge(100, 1, true, false));
        assertEquals(1000, storage.energy.longValue());
        assertEquals(100, item.getCharge());
        assertEquals(80, item.discharge(80, 1, true, true, true));
        assertEquals(1000, storage.energy.longValue());
        assertEquals(80, item.discharge(80, 1, true, true, false));
        assertEquals(200, storage.energy.longValue());
    }

    @Test
    void fractionalCapacityAndRateCannotCreateOrDestroyEnergy() {
        Storage storage = new Storage(103, 27);
        var item = adapter(storage, 1, true);
        assertEquals(2, item.charge(100, 1, true, false));
        assertEquals(20, storage.energy.longValue());
        storage.setEnergy(0, FloatingLong.create(98));
        assertEquals(0, item.charge(100, 1, true, false));
        assertEquals(98, storage.energy.longValue());
        assertEquals(2, item.discharge(100, 1, true, true, false));
        assertEquals(78, storage.energy.longValue());
    }

    @Test
    void tierAndGtTransferLimitAreEnforcedAlongsideNativeLimit() {
        Storage storage = new Storage(10000, 10000);
        var item = adapter(storage, 1, true);
        assertEquals(0, item.charge(100, 0, true, false));
        assertEquals(32, item.charge(100, 1, false, false));
        assertEquals(0, item.discharge(32, 0, true, true, false));
        assertEquals(32, item.discharge(100, 1, false, true, false));
        assertEquals(0, item.charge(-1, 1, true, false));
    }

    @Test
    void toolsCannotDischargeAsBatteriesEvenWithUnlimitedRequest() {
        Storage storage = new Storage(10000, 10000);
        storage.setEnergy(0, FloatingLong.create(1000));
        var item = adapter(storage, 5, false);
        assertFalse(item.canProvideChargeExternally());
        assertEquals(0, item.discharge(Long.MAX_VALUE, 14, true, true, false));
        assertEquals(1000, storage.energy.longValue());
    }

    @Test
    void chargeQueriesAndTransfersAreNotTruncatedToFeIntegerRange() {
        Storage storage = new Storage(50_000_000_000L, 50_000_000_000L);
        var item = adapter(storage, 9, true);
        assertEquals(5_000_000_000L, item.getMaxCharge());
        assertEquals(5_000_000_000L, item.charge(Long.MAX_VALUE, 14, true, false));
        assertEquals(5_000_000_000L, item.getCharge());
        assertEquals(5_000_000_000L, item.discharge(Long.MAX_VALUE, 14, true, true, false));
        assertTrue(storage.energy.isZero());
    }

    @Test
    void disabledOrStackedItemDoesNotTransfer() {
        Storage storage = new Storage(10000, 10000);
        var item = new MekEuEnergyAdapter(storage, () -> FloatingLong.create(10), 1, true, () -> false);
        assertEquals(0, item.charge(32, 1, true, false));
        assertEquals(0, item.discharge(32, 1, true, true, false));
        assertFalse(item.canProvideChargeExternally());
    }
}
