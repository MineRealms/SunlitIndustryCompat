package com.society.sunlitindustry.performance.ae2.network;

import com.mojang.logging.LogUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;

/** Versioned, self-contained cell key storage. No world database or shared cell identity. */
public final class CompactCellNbt {
    public static final String PAYLOAD = "sunlitindustry:cell_keys";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int VERSION = 1;
    private static final int MAX_RAW_BYTES = 16 * 1024 * 1024;
    private static final int MIN_RAW_BYTES = 1024;

    private CompactCellNbt() {}

    public static boolean compact(CompoundTag cell) {
        if (cell.contains(PAYLOAD)) return false;
        ListTag keys = cell.getList("keys", Tag.TAG_COMPOUND);
        if (keys.isEmpty() || keys.size() != cell.getLongArray("amts").length) return false;
        CompoundTag root = new CompoundTag();
        root.put("keys", keys);
        try {
            ByteArrayOutputStream rawBytes = new ByteArrayOutputStream();
            NbtIo.write(root, new DataOutputStream(rawBytes));
            byte[] raw = rawBytes.toByteArray();
            if (raw.length < MIN_RAW_BYTES || raw.length > MAX_RAW_BYTES) return false;
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            Deflater deflater = new Deflater(Deflater.BEST_SPEED);
            try {
                try (DeflaterOutputStream output = new DeflaterOutputStream(compressed, deflater)) {
                    output.write(raw);
                }
            } finally {
                deflater.end();
            }
            byte[] encoded = compressed.toByteArray();
            if (encoded.length + 96 >= raw.length) return false;
            // Never write a payload that exceeds the reader's depth or accounting limits.
            NbtIo.read(new DataInputStream(new ByteArrayInputStream(raw)), new NbtAccounter(4L * MAX_RAW_BYTES));
            CRC32 checksum = new CRC32();
            checksum.update(raw);
            CompoundTag payload = new CompoundTag();
            payload.putInt("version", VERSION);
            payload.putInt("rawSize", raw.length);
            payload.putInt("keyCount", keys.size());
            payload.putLong("crc32", checksum.getValue());
            payload.putByteArray("data", encoded);
            cell.put(PAYLOAD, payload);
            cell.remove("keys");
            return true;
        } catch (IOException | RuntimeException exception) {
            // Keep the complete native representation if encoding cannot finish.
            return false;
        }
    }

    public static boolean expand(CompoundTag cell) {
        if (!cell.contains(PAYLOAD)) return false;
        try {
            if (cell.contains("keys")) throw new IOException("Ambiguous native and compact keys");
            if (!cell.contains(PAYLOAD, Tag.TAG_COMPOUND)) throw new IOException("Payload is not a compound");
            CompoundTag payload = cell.getCompound(PAYLOAD);
            if (!payload.contains("version", Tag.TAG_INT) || payload.getInt("version") != VERSION) {
                throw new IOException("Unsupported payload version");
            }
            if (!payload.contains("rawSize", Tag.TAG_INT) || !payload.contains("keyCount", Tag.TAG_INT)
                    || !payload.contains("crc32", Tag.TAG_LONG) || !payload.contains("data", Tag.TAG_BYTE_ARRAY)
                    || !cell.contains("amts", Tag.TAG_LONG_ARRAY)) {
                throw new IOException("Missing or invalid payload metadata");
            }
            int size = payload.getInt("rawSize");
            if (size <= 0 || size > MAX_RAW_BYTES) throw new IOException("Invalid payload size");
            int count = payload.getInt("keyCount");
            if (count <= 0 || count != cell.getLongArray("amts").length) {
                throw new IOException("Cell payload key count mismatch");
            }
            byte[] raw;
            try (InflaterInputStream input = new InflaterInputStream(
                    new ByteArrayInputStream(payload.getByteArray("data")))) {
                raw = input.readNBytes(size + 1);
                if (raw.length != size) throw new IOException("Cell payload length mismatch");
            }
            CRC32 checksum = new CRC32();
            checksum.update(raw);
            if (checksum.getValue() != payload.getLong("crc32")) {
                throw new IOException("Cell payload checksum mismatch");
            }
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(raw));
            CompoundTag expanded = NbtIo.read(input, new NbtAccounter(4L * MAX_RAW_BYTES));
            if (expanded == null || input.available() != 0 || !expanded.contains("keys", Tag.TAG_LIST)) {
                throw new IOException("Invalid payload key list");
            }
            ListTag keys = expanded.getList("keys", Tag.TAG_COMPOUND);
            if (keys.size() != count) {
                throw new IOException("Cell payload key count mismatch");
            }
            // Commit only after the full payload is validated. Decode failure must never look like an empty cell.
            cell.put("keys", keys);
            cell.remove(PAYLOAD);
            return true;
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Cannot expand Sunlit AE2 cell keys: {}. Original NBT retained; cell stays read-only until repaired.",
                    exception.toString());
            return false;
        }
    }
}
