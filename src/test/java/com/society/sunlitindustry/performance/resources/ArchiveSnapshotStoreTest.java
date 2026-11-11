package com.society.sunlitindustry.performance.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveSnapshotStoreTest {
    @TempDir Path temporary;

    @Test
    void diskRoundTripKeepsExactBytesAndResourceBoundary() throws IOException {
        Path archive = archive(Map.of("data/test/recipes/a.json", "{\"x\":1}",
                "data/test/recipes_extra/a.json", "{}", "assets/test/models/a.json", "{\"parent\":\"x\"}"));
        var cold = store();
        String digest = cold.identify(archive);
        var built = cold.get(archive, digest);
        assertEquals(1, built.list("data/test/recipes/").size());
        assertEquals(java.util.List.of("data/test/recipes/a.json"), built.listTree("data/test/recipes/a.json"));
        assertEquals(java.util.List.of("data/test/recipes/a.json"), built.listTree("data/test/recipes"));
        assertArrayEquals("{\"x\":1}".getBytes(StandardCharsets.UTF_8), built.open("data/test/recipes/a.json").readAllBytes());
        var warm = store();
        var loaded = warm.get(archive, warm.identify(archive));
        assertEquals(built.list("assets/"), loaded.list("assets/"));
        assertEquals(1L, warm.statistics().get("diskHits"));
        assertEquals(0L, warm.statistics().get("builds"));
    }

    @Test
    void sameSizeAndTimestampReplacementStillInvalidatesAcrossProcesses() throws IOException {
        Path archive = archive(Map.of("data/test/tags/items/a.json", "{\"values\":[\"a\"]}"));
        FileTime time = Files.getLastModifiedTime(archive);
        String first = store().identify(archive);
        archive(Map.of("data/test/tags/items/a.json", "{\"values\":[\"b\"]}"));
        Files.setLastModifiedTime(archive, time);
        var second = store();
        assertNotEquals(first, second.identify(archive));
        assertTrue(new String(second.get(archive, second.identify(archive))
                .open("data/test/tags/items/a.json").readAllBytes(), StandardCharsets.UTF_8).contains("b"));
    }

    @Test
    void corruptDiskFallsBackAndDeletedResourcesDoNotReappear() throws IOException {
        Path archive = archive(Map.of("data/test/recipes/a.json", "{}"));
        var cache = store();
        String digest = cache.identify(archive);
        cache.get(archive, digest);
        Path file = temporary.resolve("cache").resolve(digest + ".bin");
        byte[] corrupt = Files.readAllBytes(file);
        corrupt[corrupt.length - 1] ^= 1;
        Files.write(file, corrupt);
        var repaired = store();
        assertTrue(repaired.get(archive, digest).contains("data/test/recipes/a.json"));
        assertEquals(1L, repaired.statistics().get("builds"));
        archive(Map.of("data/test/recipes/b.json", "{}"));
        var changed = store();
        var snapshot = changed.get(archive, changed.identify(archive));
        assertFalse(snapshot.contains("data/test/recipes/a.json"));
        assertTrue(snapshot.contains("data/test/recipes/b.json"));
    }

    @Test
    void streamConsumersCannotMutateSharedBytes() throws IOException {
        Path archive = archive(Map.of("data/test/recipes/a.json", "{}"));
        var store = store();
        var snapshot = store.get(archive, store.identify(archive));
        byte[] bytes = snapshot.open("data/test/recipes/a.json").readAllBytes();
        bytes[0] = 0;
        assertEquals('{', snapshot.open("data/test/recipes/a.json").read());
        assertSame(snapshot, store.get(archive, store.identify(archive)));
    }

    @Test
    void sharedSuppliersKeepIndependentCursorsAndPrivateSourceBytes() throws IOException {
        Path archive = archive(Map.of("data/test/recipes/a.json", "{\"x\":1}"));
        var store = store();
        var snapshot = store.get(archive, store.identify(archive));
        var resource = snapshot.resource("data/test/recipes/a.json");
        assertSame(resource, snapshot.resource("data/test/recipes/a.json"));
        try (var first = resource.get(); var second = resource.get()) {
            assertEquals('{', first.read());
            assertEquals('{', second.read());
            first.readAllBytes()[0] = 0;
            assertEquals('"', second.read());
        }
        try (var fresh = resource.get()) { assertEquals('{', fresh.read()); }
        assertNull(snapshot.resource("data/test/recipes/missing.json"));
    }

    @Test
    void indexedTraversalMatchesFileAndDirectoryBoundaries() throws IOException {
        Path archive = archive(Map.of("data/test/recipes/a.json", "{}",
                "data/test/recipes/nested/b.json", "{}", "data/test/recipes_extra/c.json", "{}",
                "assets/test/textures/a.png", "image"));
        var store = store();
        var snapshot = store.get(archive, store.identify(archive));
        var listed = new java.util.ArrayList<String>();
        snapshot.forEachTree("data/test/recipes", (name, resource) -> listed.add(name));
        assertEquals(snapshot.listTree("data/test/recipes"), listed);
        assertEquals(2, listed.size());
        listed.clear();
        snapshot.forEachTree("data/test/recipes/a.json", (name, resource) -> listed.add(name));
        assertEquals(java.util.List.of("data/test/recipes/a.json"), listed);
        var image = snapshot.resource("assets/test/textures/a.png");
        assertNotNull(image);
        assertFalse(image.hasBytes());
        assertThrows(IOException.class, image::get);
        assertNull(snapshot.open("assets/test/textures/a.png"));
    }

    @Test
    void admissionLimitDoesNotRepeatedlyReadAnUncacheableArchive() throws IOException {
        Path archive = archive(Map.of("data/test/recipes/a.json", "{}"));
        var store = new ArchiveSnapshotStore(temporary.resolve("small-cache"), 1);
        String digest = store.identify(archive);
        assertNull(store.get(archive, digest));
        assertNull(store.get(archive, digest));
        assertEquals(1L, store.statistics().get("builds"));
        assertEquals(1L, store.statistics().get("bypassed"));
        assertEquals(0L, store.statistics().get("retainedBytes"));
    }

    @Test
    void fullCacheEvictsLeastRecentSnapshotAndAllowsItToReturn() throws IOException {
        Path first = archive("first.jar", Map.of("data/test/recipes/a.json", "{\"x\":1}"));
        Path second = archive("second.jar", Map.of("data/test/recipes/a.json", "{\"x\":2}"));
        Path third = archive("third.jar", Map.of("data/test/recipes/a.json", "{\"x\":3}"));
        var sizing = store();
        long weight = sizing.get(first, sizing.identify(first)).weight();
        var store = new ArchiveSnapshotStore(temporary.resolve("lru-cache"), weight * 2);
        String firstDigest = store.identify(first);
        String secondDigest = store.identify(second);
        String thirdDigest = store.identify(third);
        var firstSnapshot = store.get(first, firstDigest);
        var secondSnapshot = store.get(second, secondDigest);
        assertSame(firstSnapshot, store.get(first, firstDigest));
        assertNotNull(store.get(third, thirdDigest));
        assertSame(firstSnapshot, store.get(first, firstDigest));
        assertNotSame(secondSnapshot, store.get(second, secondDigest));
        assertEquals(1L, store.statistics().get("diskHits"));
        assertEquals(2L, store.statistics().get("snapshots"));
        assertEquals(weight * 2, store.statistics().get("retainedBytes"));
        assertEquals(0L, store.statistics().get("bypassed"));
        store.clearMemory();
        assertEquals(0L, store.statistics().get("retainedBytes"));
        assertNotNull(store.get(first, firstDigest));
    }

    @Test
    void serverSnapshotIndexesModelSourcesButDoesNotRetainTheirBytes() throws IOException {
        Path archive = archive(Map.of("data/test/recipes/a.json", "{}", "assets/test/models/a.json", "{}",
                "assets/test/lang/en_us.json", "{}"));
        var store = new ArchiveSnapshotStore(temporary.resolve("server-cache"), 4 << 20, false);
        var snapshot = store.get(archive, store.identify(archive));
        assertTrue(snapshot.contains("assets/test/models/a.json"));
        assertFalse(snapshot.hasBytes("assets/test/models/a.json"));
        assertTrue(snapshot.hasBytes("assets/test/lang/en_us.json"));
        assertTrue(snapshot.hasBytes("data/test/recipes/a.json"));
    }

    private ArchiveSnapshotStore store() { return new ArchiveSnapshotStore(temporary.resolve("cache"), 4 << 20); }

    private Path archive(Map<String, String> entries) throws IOException {
        return archive("mod.jar", entries);
    }

    private Path archive(String name, Map<String, String> entries) throws IOException {
        Path path = temporary.resolve(name);
        try (var zip = new ZipOutputStream(Files.newOutputStream(path))) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return path;
    }
}
