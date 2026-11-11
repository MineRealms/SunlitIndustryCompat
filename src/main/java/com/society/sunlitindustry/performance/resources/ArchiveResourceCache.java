package com.society.sunlitindustry.performance.resources;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ArchiveResourceCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<Path> FAILED = ConcurrentHashMap.newKeySet();
    private static volatile ArchiveSnapshotStore store;

    public static boolean enabled() {
        if (!FMLLoader.isProduction() || !ResourceCacheConfig.enabled()) return false;
        return FMLLoader.getDist().isDedicatedServer() || ResourceCacheConfig.clientModels();
    }

    public static Binding bind(Path source) {
        if (!enabled()) return null;
        var list = FMLLoader.getLoadingModList();
        if (list == null) return null;
        for (var info : list.getModFiles()) {
            var file = info.getFile();
            Path archive = file.getFilePath();
            if (!archive.getFileSystem().provider().getScheme().equals("file")
                    || !archive.toString().endsWith(".jar") || !Files.isRegularFile(archive)) continue;
            Path root = file.findResource("");
            if (root == null) continue;
            // Forge's ResourcePackLoader stores the physical jar as source and overrides resolve.
            // Subdirectory packs need a separate index; do not answer absence from the root index.
            if (!ArchiveSourceIdentity.isRoot(source, archive, root)) continue;
            if (FAILED.contains(archive)) return null;
            try {
                return new Binding(archive, getStore().identify(archive), "");
            } catch (IOException | RuntimeException failure) {
                fail(archive, failure);
                return null;
            }
        }
        return null;
    }

    public static ArchiveSnapshotStore.Snapshot snapshot(Binding binding) {
        if (binding == null || FAILED.contains(binding.archive())) return null;
        try {
            var result = getStore().get(binding.archive(), binding.digest());
            return result;
        } catch (IOException | RuntimeException failure) {
            fail(binding.archive(), failure);
            return null;
        }
    }

    public static Map<String, Long> statistics() { return getStore().statistics(); }

    private static ArchiveSnapshotStore getStore() {
        long limit = (long) ResourceCacheConfig.memoryMiB() << 20;
        var current = store;
        if (current == null || current.memoryLimit() != limit) {
            synchronized (ArchiveResourceCache.class) {
                current = store;
                if (current == null || current.memoryLimit() != limit) store = current = new ArchiveSnapshotStore(
                        FMLPaths.GAMEDIR.get().resolve(FMLLoader.getDist().isClient()
                                ? "cache/sunlitindustry/archives-v2-client" : "cache/sunlitindustry/archives-v2-server"),
                        limit, FMLLoader.getDist().isClient());
            }
        }
        return current;
    }

    private static void fail(Path archive, Exception failure) {
        if (FAILED.add(archive)) LOGGER.warn("Sunlit archive cache bypass for {}: {}", archive, failure.toString());
    }

    public record Binding(Path archive, String digest, String prefix) {}
    private ArchiveResourceCache() {}
}
