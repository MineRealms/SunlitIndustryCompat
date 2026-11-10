package com.society.sunlitindustry.performance.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveSourceIdentityTest {
    @TempDir Path temporary;

    @Test
    void forgePhysicalSourceAndArchiveRootAreBothRecognized() throws Exception {
        Path archive = temporary.resolve("mod.jar");
        try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {}
        try (var filesystem = FileSystems.newFileSystem(archive, Map.of())) {
            Path root = filesystem.getPath("/");
            assertTrue(ArchiveSourceIdentity.isRoot(archive, archive, root));
            assertTrue(ArchiveSourceIdentity.isRoot(root, archive, root));
            assertFalse(ArchiveSourceIdentity.isRoot(root.resolve("resourcepacks/optional"), archive, root));
            assertFalse(ArchiveSourceIdentity.isRoot(temporary.resolve("other.jar"), archive, root));
        }
    }
}
