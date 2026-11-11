package com.society.sunlitindustry.performance.ae2.network;

import java.lang.ref.WeakReference;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

/** The level lookup remains live; only positive, invalidation-aware capability results are reused. */
public final class CapabilityLookupCache<C> {
    private final Entry<C>[] sides;

    @SuppressWarnings("unchecked")
    public CapabilityLookupCache() { sides = (Entry<C>[]) new Entry<?>[7]; }

    public C find(BlockEntity blockEntity, Capability<C> capability, Direction side) {
        if (blockEntity == null || blockEntity.isRemoved()) {
            clear();
            return null;
        }
        int index = side == null ? 6 : side.ordinal();
        Entry<C> cached = sides[index];
        if (cached != null && cached.valid && cached.blockEntity == blockEntity
                && cached.capability == capability) return cached.value;
        sides[index] = null;
        LazyOptional<C> optional = blockEntity.getCapability(capability, side);
        C value = optional.orElse(null);
        if (value == null) return null;
        Entry<C> entry = new Entry<>(blockEntity, capability, value);
        sides[index] = entry;
        WeakReference<Entry<C>> weak = new WeakReference<>(entry);
        optional.addListener(ignored -> {
            Entry<C> current = weak.get();
            if (current != null) current.invalidate();
        });
        return entry.valid ? entry.value : null;
    }

    public void clear() {
        for (int i = 0; i < sides.length; i++) sides[i] = null;
    }

    private static final class Entry<C> {
        private BlockEntity blockEntity;
        private final Capability<C> capability;
        private C value;
        private volatile boolean valid = true;

        private Entry(BlockEntity blockEntity, Capability<C> capability, C value) {
            this.blockEntity = blockEntity;
            this.capability = capability;
            this.value = value;
        }

        private void invalidate() {
            valid = false;
            blockEntity = null;
            value = null;
        }
    }
}
