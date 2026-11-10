package com.society.sunlitindustry.performance.resources;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.zip.ZipFile;

/** Immutable source data only: never contains recipes, registry values, or a resource manager. */
public final class ArchiveSnapshotStore {
    private static final int MAGIC = 0x53565243;
    private static final int VERSION = 1;
    private static final int MAX_ENTRIES = 500_000;
    private static final int MAX_FILE = 1 << 20;
    private static final int MAX_DATA = 16 << 20;
    private static final long MAX_DISK = 512L << 20;
    private final Path directory;
    private final long memoryLimit;
    private final LinkedHashMap<Path, String> digests = new LinkedHashMap<>();
    private final LinkedHashMap<String, Snapshot> snapshots = new LinkedHashMap<>(16, .75f, true);
    private long retained;
    private final LongAdder hits = new LongAdder();
    private final LongAdder diskHits = new LongAdder();
    private final LongAdder builds = new LongAdder();
    private final LongAdder failures = new LongAdder();

    public ArchiveSnapshotStore(Path directory, long memoryLimit) {
        this.directory = directory;
        this.memoryLimit = memoryLimit;
    }

    public synchronized String identify(Path archive) throws IOException {
        Path canonical = archive.toRealPath();
        String existing = digests.get(canonical);
        if (existing != null) return existing;
        String digest;
        try (InputStream input = Files.newInputStream(canonical)) {
            digest = hash(input);
        }
        if (digests.size() >= 2048) digests.clear();
        digests.put(canonical, digest);
        return digest;
    }

    public synchronized Snapshot get(Path archive, String digest) throws IOException {
        Snapshot existing = snapshots.get(digest);
        if (existing != null) {
            hits.increment();
            return existing;
        }
        Path cache = directory.resolve(digest + ".bin");
        Snapshot snapshot;
        try {
            snapshot = read(cache, digest);
            diskHits.increment();
        } catch (IOException | RuntimeException invalid) {
            snapshot = build(archive);
            builds.increment();
            try {
                write(cache, digest, snapshot);
            } catch (IOException failure) {
                failures.increment();
            }
        }
        long weight = snapshot.weight();
        if (weight <= memoryLimit) {
            while (!snapshots.isEmpty() && retained + weight > memoryLimit) {
                var iterator = snapshots.entrySet().iterator();
                var eldest = iterator.next();
                retained -= eldest.getValue().weight();
                iterator.remove();
            }
            snapshots.put(digest, snapshot);
            retained += weight;
        }
        return snapshot;
    }

    public synchronized void clearMemory() {
        snapshots.clear();
        retained = 0;
    }

    public synchronized Map<String, Long> statistics() {
        return Map.of("memoryHits", hits.sum(), "diskHits", diskHits.sum(), "builds", builds.sum(),
                "writeFailures", failures.sum(), "retainedBytes", retained, "snapshots", (long) snapshots.size());
    }

    private static Snapshot build(Path archive) throws IOException {
        TreeMap<String, byte[]> entries = new TreeMap<>();
        int dataBytes = 0;
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            var enumeration = zip.entries();
            while (enumeration.hasMoreElements()) {
                var entry = enumeration.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !validName(name)) continue;
                if (entries.size() >= MAX_ENTRIES) throw new IOException("Archive resource count exceeds limit");
                byte[] bytes = null;
                if (cacheable(name) && entry.getSize() >= 0 && entry.getSize() <= MAX_FILE
                        && dataBytes + entry.getSize() <= MAX_DATA) {
                    try (InputStream input = zip.getInputStream(entry)) {
                        bytes = input.readNBytes(MAX_FILE + 1);
                    }
                    if (bytes.length > MAX_FILE) bytes = null;
                    else dataBytes += bytes.length;
                }
                entries.put(name, bytes);
            }
        }
        return new Snapshot(entries);
    }

    private Snapshot read(Path cache, String digest) throws IOException {
        if (Files.size(cache) > 64L << 20) throw new IOException("Oversized resource snapshot");
        MessageDigest checksum = sha256();
        try (DataInputStream input = new DataInputStream(new DigestInputStream(
                new BufferedInputStream(Files.newInputStream(cache)), checksum))) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION || !input.readUTF().equals(digest)) {
                throw new IOException("Invalid resource snapshot header");
            }
            int count = input.readInt();
            if (count < 0 || count > MAX_ENTRIES) throw new IOException("Invalid resource count");
            TreeMap<String, byte[]> entries = new TreeMap<>();
            int dataBytes = 0;
            for (int i = 0; i < count; i++) {
                String name = input.readUTF();
                int length = input.readInt();
                if (!validName(name) || entries.containsKey(name) || length < -1 || length > MAX_FILE) {
                    throw new IOException("Invalid resource record");
                }
                byte[] bytes = null;
                if (length >= 0) {
                    dataBytes += length;
                    if (dataBytes > MAX_DATA || !cacheable(name)) throw new IOException("Invalid resource data");
                    bytes = input.readNBytes(length);
                    if (bytes.length != length) throw new IOException("Truncated resource data");
                }
                entries.put(name, bytes);
            }
            byte[] actual = checksum.digest();
            byte[] expected = input.readNBytes(32);
            if (!MessageDigest.isEqual(actual, expected) || input.read() != -1) {
                throw new IOException("Resource snapshot checksum mismatch");
            }
            return new Snapshot(entries);
        }
    }

    private void write(Path cache, String digest, Snapshot snapshot) throws IOException {
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "snapshot-", ".tmp");
        try {
            MessageDigest checksum = sha256();
            try (var outputStream = new java.security.DigestOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(temporary)), checksum);
                 var output = new DataOutputStream(outputStream)) {
                output.writeInt(MAGIC);
                output.writeInt(VERSION);
                output.writeUTF(digest);
                output.writeInt(snapshot.entries.size());
                for (var entry : snapshot.entries.entrySet()) {
                    output.writeUTF(entry.getKey());
                    byte[] bytes = entry.getValue();
                    output.writeInt(bytes == null ? -1 : bytes.length);
                    if (bytes != null) output.write(bytes);
                }
                byte[] sum = checksum.digest();
                outputStream.on(false);
                output.write(sum);
            }
            try {
                Files.move(temporary, cache, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, cache, StandardCopyOption.REPLACE_EXISTING);
            }
            prune(cache);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void prune(Path keep) throws IOException {
        List<Path> files;
        try (var stream = Files.list(directory)) {
            files = new ArrayList<>(stream.filter(p -> p.getFileName().toString().matches("[a-f0-9]{64}\\.bin"))
                    .toList());
        }
        files.sort(java.util.Comparator.comparingLong(p -> p.toFile().lastModified()));
        long bytes = 0;
        for (Path file : files) bytes += Files.size(file);
        for (Path file : files) {
            if (bytes <= MAX_DISK) break;
            if (file.equals(keep)) continue;
            long size = Files.size(file);
            Files.deleteIfExists(file);
            bytes -= size;
        }
    }

    private static boolean validName(String name) {
        return (name.startsWith("data/") || name.startsWith("assets/"))
                && name.length() < 8192 && !name.contains("\\") && !name.contains("//")
                && !name.contains("/../") && !name.contains("/./") && !name.endsWith("/..");
    }

    private static boolean cacheable(String name) {
        if (name.startsWith("data/")) return name.endsWith(".json");
        return name.startsWith("assets/") && (name.endsWith(".json") || name.endsWith(".mcmeta"));
    }

    private static String hash(InputStream input) throws IOException {
        MessageDigest digest = sha256();
        byte[] buffer = new byte[64 << 10];
        int read;
        while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public static final class Snapshot {
        private final NavigableMap<String, byte[]> entries;
        private final long weight;

        private Snapshot(Map<String, byte[]> entries) {
            this.entries = Collections.unmodifiableNavigableMap(new TreeMap<>(entries));
            long size = 0;
            for (var entry : entries.entrySet()) {
                size += 96L + entry.getKey().length() * 2L;
                if (entry.getValue() != null) size += entry.getValue().length;
            }
            weight = size;
        }

        public boolean contains(String path) { return entries.containsKey(path); }
        public boolean hasBytes(String path) { return entries.get(path) != null; }
        public InputStream open(String path) {
            byte[] data = entries.get(path);
            return data == null ? null : new ByteArrayInputStream(data);
        }
        public List<String> list(String prefix) {
            return List.copyOf(entries.subMap(prefix, true, prefix + '\uffff', true).keySet());
        }
        public List<String> listTree(String path) {
            return contains(path) ? List.of(path) : list(path + "/");
        }
        public long weight() { return weight; }
    }
}
