package com.society.sunlitindustry.performance.ae2.network;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

class CompactCellNbtTest {
    @Test
    void roundTripPreservesArbitraryKeyTypesAmountsAndUnrelatedTags() {
        CompoundTag cell = cell(63);
        CompoundTag expected = cell.copy();
        assertTrue(CompactCellNbt.compact(cell));
        assertFalse(cell.contains("keys"));
        assertTrue(cell.contains(CompactCellNbt.PAYLOAD));
        assertTrue(CompactCellNbt.expand(cell));
        assertEquals(expected, cell);
    }

    @Test
    void copiedCellsOwnTheirPayloadAndContents() {
        CompoundTag original = cell(63);
        assertTrue(CompactCellNbt.compact(original));
        CompoundTag copied = original.copy();
        assertTrue(CompactCellNbt.expand(copied));
        copied.getList("keys", 10).getCompound(0).putString("custom", "edited");
        assertTrue(CompactCellNbt.expand(original));
        assertNotEquals(copied, original);
    }

    @Test
    void corruptedPayloadReturnsFalseBeforeModifyingCell() {
        CompoundTag cell = cell(63);
        assertTrue(CompactCellNbt.compact(cell));
        cell.getCompound(CompactCellNbt.PAYLOAD).putLong("crc32", -1);
        CompoundTag before = cell.copy();
        assertFalse(CompactCellNbt.expand(cell));
        assertEquals(before, cell);
        assertFalse(CompactCellNbt.compact(cell));
        assertEquals(before, cell);
    }

    @Test
    void rejectsUnknownVersionAndOversizedPayloadWithoutMigration() {
        CompoundTag cell = cell(63);
        assertTrue(CompactCellNbt.compact(cell));
        cell.getCompound(CompactCellNbt.PAYLOAD).putInt("version", 2);
        CompoundTag unknown = cell.copy();
        assertFalse(CompactCellNbt.expand(cell));
        assertEquals(unknown, cell);
        cell.getCompound(CompactCellNbt.PAYLOAD).putInt("version", 1);
        cell.getCompound(CompactCellNbt.PAYLOAD).putInt("rawSize", Integer.MAX_VALUE);
        CompoundTag oversized = cell.copy();
        assertFalse(CompactCellNbt.expand(cell));
        assertEquals(oversized, cell);
    }

    @Test
    void rejectsMismatchedCountsWithoutDiscardingAnything() {
        CompoundTag cell = cell(63);
        assertTrue(CompactCellNbt.compact(cell));
        cell.putLongArray("amts", new long[] { 1 });
        CompoundTag before = cell.copy();
        assertFalse(CompactCellNbt.expand(cell));
        assertEquals(before, cell);
    }

    @Test
    void refusesToOverwriteAnAmbiguousNativeKeyList() {
        CompoundTag cell = cell(63);
        assertTrue(CompactCellNbt.compact(cell));
        cell.put("keys", new ListTag());
        CompoundTag before = cell.copy();
        assertFalse(CompactCellNbt.expand(cell));
        assertEquals(before, cell);
    }

    @Test
    void malformedMetadataAndCompressedBytesRemainUntouched() {
        CompoundTag compact = cell(63);
        assertTrue(CompactCellNbt.compact(compact));
        for (String field : new String[] { "version", "rawSize", "keyCount", "crc32", "data" }) {
            CompoundTag invalid = compact.copy();
            invalid.getCompound(CompactCellNbt.PAYLOAD).remove(field);
            CompoundTag before = invalid.copy();
            assertFalse(CompactCellNbt.expand(invalid), field);
            assertEquals(before, invalid, field);
        }
        CompoundTag invalid = compact.copy();
        invalid.getCompound(CompactCellNbt.PAYLOAD).putByteArray("data", new byte[] { 1, 2, 3 });
        CompoundTag before = invalid.copy();
        assertFalse(CompactCellNbt.expand(invalid));
        assertEquals(before, invalid);
    }

    @Test
    void wrongPayloadOrAmountTagTypesRemainUntouched() {
        CompoundTag compact = cell(63);
        assertTrue(CompactCellNbt.compact(compact));
        compact.putString(CompactCellNbt.PAYLOAD, "unrecognized payload");
        CompoundTag before = compact.copy();
        assertFalse(CompactCellNbt.expand(compact));
        assertEquals(before, compact);

        compact = cell(63);
        assertTrue(CompactCellNbt.compact(compact));
        compact.put("amts", new ListTag());
        before = compact.copy();
        assertFalse(CompactCellNbt.expand(compact));
        assertEquals(before, compact);
    }

    @Test
    void legacyEmptyAndSmallCellsStayNative() {
        assertFalse(CompactCellNbt.compact(new CompoundTag()));
        CompoundTag legacy = cell(1);
        assertFalse(CompactCellNbt.expand(legacy));
        CompoundTag expected = legacy.copy();
        assertFalse(CompactCellNbt.compact(legacy));
        assertEquals(expected, legacy);
    }

    private static CompoundTag cell(int types) {
        CompoundTag result = new CompoundTag();
        ListTag keys = new ListTag();
        long[] amounts = new long[types];
        for (int i = 0; i < types; i++) {
            CompoundTag key = new CompoundTag();
            key.putString("#type", i % 2 == 0 ? "ae2:i" : "custom:chemical");
            key.putString("id", "example:variant_" + i);
            key.putString("custom", "Repeated custom data preserved for arbitrary addon keys ".repeat(5));
            CompoundTag tag = new CompoundTag();
            tag.putLong("energy", i * 10000L);
            key.put("tag", tag);
            keys.add(key);
            amounts[i] = Integer.MAX_VALUE + (long) i;
        }
        result.put("keys", keys);
        result.putLongArray("amts", amounts);
        result.putLong("ic", 123456789L);
        result.putString("display", "Untouched display and upgrade data");
        return result;
    }
}
