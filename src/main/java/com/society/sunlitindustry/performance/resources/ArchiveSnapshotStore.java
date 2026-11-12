package com.society.sunlitindustry.performance.resources;

import net.minecraft.server.packs.resources.IoSupplier;

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
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
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
    private static final int MAX_LOADS_PER_RELOAD = 2048;
    private final Path directory;
    private final long memoryLimit;
    private final boolean clientSources;
    private final LinkedHashMap<Path, String> digests = new LinkedHashMap<>(16, .75f, true);
    private final LinkedHashMap<String, Snapshot> snapshots = new LinkedHashMap<>(16, .75f, true);
    private final LinkedHashMap<String, Boolean> oversized = new LinkedHashMap<>(16, .75f, true);
    private final Set<String> loadedThisReload = new HashSet<>();
    private long retained;
    private final LongAdder hits = new LongAdder();
    private final LongAdder diskHits = new LongAdder();
    private final LongAdder builds = new LongAdder();
    private final LongAdder failures = new LongAdder();
    private final LongAdder nativeFallbacks = new LongAdder();

    public ArchiveSnapshotStore(Path directory, long memoryLimit) {
        this(directory, memoryLimit, true);
    }

    public ArchiveSnapshotStore(Path directory, long memoryLimit, boolean clientSources) {
        this.directory = directory;
        this.memoryLimit = memoryLimit;
        this.clientSources = clientSources;
    }

    public synchronized String identify(Path archive) throws IOException {
        Path canonical = archive.toRealPath();
        String existing = digests.get(canonical);
        if (existing != null) return existing;
        String digest;
        try (InputStream input = Files.newInputStream(canonical)) {
            digest = hash(input);
        }
        if (digests.size() >= 2048) digests.remove(digests.keySet().iterator().next());
        digests.put(canonical, digest);
        return digest;
    }

    public synchronized Snapshot get(Path archive, String digest) throws IOException {
        if (oversized.get(digest) != null) return null;
        Snapshot existing = snapshots.get(digest);
        if (existing != null) {
            hits.increment();
            return existing;
        }
        // Rehydrating evicted packs on every resource query causes cyclic working sets
        // to serialize on disk I/O and allocate a whole snapshot per missing resource.
        if (loadedThisReload.contains(digest) || loadedThisReload.size() >= MAX_LOADS_PER_RELOAD) {
            nativeFallbacks.increment();
            return null;
        }
        loadedThisReload.add(digest);
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
        if (weight > memoryLimit) {
            // Intrinsically oversized snapshots cannot be admitted at this capacity.
            if (oversized.size() >= 2048) oversized.remove(oversized.keySet().iterator().next());
            oversized.put(digest, Boolean.TRUE);
            return null;
        }
        var entries = snapshots.entrySet().iterator();
        while (retained + weight > memoryLimit && entries.hasNext()) {
            retained -= entries.next().getValue().weight();
            entries.remove();
        }
        snapshots.put(digest, snapshot);
        retained += weight;
        return snapshot;
    }

    long memoryLimit() { return memoryLimit; }

    public synchronized void beginReload() {
        loadedThisReload.clear();
    }

    public synchronized void clearMemory() {
        snapshots.clear();
        oversized.clear();
        loadedThisReload.clear();
        retained = 0;
    }

    public synchronized Map<String, Long> statistics() {
        return Map.of("memoryHits", hits.sum(), "diskHits", diskHits.sum(), "builds", builds.sum(),
                "writeFailures", failures.sum(), "retainedBytes", retained, "snapshots", (long) snapshots.size(),
                "bypassed", (long) oversized.size(), "nativeFallbacks", nativeFallbacks.sum());
    }

    private Snapshot build(Path archive) throws IOException {
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
                if (cacheable(name) && (clientSources || name.startsWith("data/") || name.contains("/lang/"))
                        && entry.getSize() >= 0 && entry.getSize() <= MAX_FILE
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
                    byte[] bytes = entry.getValue().bytes;
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
        private final NavigableMap<String, Resource> entries;
        private final long weight;

        private Snapshot(Map<String, byte[]> entries) {
            TreeMap<String, Resource> indexed = new TreeMap<>();
            long size = 0;
            for (var entry : entries.entrySet()) {
                size += 96L + entry.getKey().length() * 2L;
                byte[] bytes = entry.getValue();
                indexed.put(entry.getKey(), bytes == null ? Resource.UNCACHED : new Resource(bytes));
                if (bytes != null) size += 24L + bytes.length;
            }
            this.entries = Collections.unmodifiableNavigableMap(indexed);
            weight = size;
        }

        public boolean contains(String path) { return entries.containsKey(path); }
        public boolean hasBytes(String path) {
            Resource resource = entries.get(path);
            return resource != null && resource.hasBytes();
        }
        public Resource resource(String path) { return entries.get(path); }
        public InputStream open(String path) {
            Resource resource = entries.get(path);
            return resource == null || !resource.hasBytes() ? null : resource.open();
        }
        public List<String> list(String prefix) {
            return List.copyOf(entries.subMap(prefix, true, prefix + '\uffff', true).keySet());
        }
        public List<String> listTree(String path) {
            return contains(path) ? List.of(path) : list(path + "/");
        }
        public void forEachTree(String path, BiConsumer<String, Resource> output) {
            Resource resource = entries.get(path);
            if (resource != null) {
                output.accept(path, resource);
                return;
            }
            String prefix = path + "/";
            entries.subMap(prefix, true, prefix + '\uffff', true).forEach(output);
        }
        public long weight() { return weight; }
    }

    /** Shared immutable source only. Every get returns an independent read cursor. */
    public static final class Resource implements IoSupplier<InputStream> {
        private static final Resource UNCACHED = new Resource(null);
        private final byte[] bytes;

        private Resource(byte[] bytes) { this.bytes = bytes; }
        public boolean hasBytes() { return bytes != null; }
        private InputStream open() { return new ByteArrayInputStream(bytes); }

        @Override
        public InputStream get() throws IOException {
            if (bytes == null) throw new IOException("Resource source bytes are not cached");
            return open();
        }
    }
}
