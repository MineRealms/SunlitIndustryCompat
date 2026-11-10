package com.society.sunlitindustry.performance.resources;

import java.nio.file.Path;

final class ArchiveSourceIdentity {
    static boolean isRoot(Path source, Path archive, Path resourceRoot) {
        if (source.getFileSystem().equals(archive.getFileSystem())
                && source.toAbsolutePath().normalize().equals(archive.toAbsolutePath().normalize())) return true;
        return source.getFileSystem().equals(resourceRoot.getFileSystem())
                && source.normalize().equals(resourceRoot.normalize());
    }

    private ArchiveSourceIdentity() {}
}
