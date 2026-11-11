# Immutable Archive Source Index

## Exact Source Path

Reviewed Forge 47.4.23 `net.minecraftforge.resource.PathPackResources` and
`ResourcePackLoader.createPackForMod` from the installed Forge sources jar.
The latter exposes the physical mod jar as `getSource()` but overrides
`resolve()` with `IModFile.findResource`. `getRootResource` checks existence and
returns an `IoSupplier`; `listResources` delegates to vanilla
`net.minecraft.server.packs.PathPackResources.listPath`.

The installed mapped Minecraft 1.20.1 bytecode confirms that `listPath` uses
`Files.find`, accepts regular files, constructs resource IDs, and emits source
suppliers. File targets are also enumerated. The resource precedence and JSON,
tag, recipe, and KubeJS consumers live above this source layer.

## Change

The existing archive path searched the snapshot for existence, searched again
for cached bytes, allocated a supplier, then reacquired the synchronized store
and searched again when opening the supplier. Directory enumeration first made
a copied path list, then created one supplier per resource per enumeration.

Each cached byte array now has one immutable `ArchiveSnapshotStore.Resource`
supplier. It returns a fresh `ByteArrayInputStream` for every caller and never
exposes the source array. Lookup selects it once; enumeration passes it directly
to the native resource consumer. `Snapshot.forEachTree` visits the ordered index
without copying its keys into an intermediate list.

An indexed file without retained bytes still uses the original pack's source
supplier. An absent file still returns absent. No parsed JSON, built tags,
registries, Recipe objects, resource managers, or Level objects are retained.
No KubeJS event is skipped. Mod jar replacement still selects a new SHA-256
snapshot on the next process; this is the same immutable-archive lifetime as the
existing source cache.

The supplier has a small retained-object cost. Snapshot admission includes an
additional conservative 24 bytes for each cached file, and the existing total
memory limit still applies. Already issued resource suppliers retain their own
immutable bytes for their consumer's lifetime, just as already opened cached
streams did; clearing the store does not invalidate those issued sources.

Disk snapshot encoding remains version 1 because it still stores the exact same
names, raw bytes, and checksum. No resource-result cache or recipe replay was
added, avoiding the WarpLoad invalidation paths.

## Integration

No new config, bootstrap hook, or mixin registration is required. The existing
`ImmutableArchiveResourcesMixin` uses the new index directly. Existing
dedicated-server, production-mode, content identity, dynamic-pack bypass, and
client conflict gates continue to apply.

The unit tests added to `ArchiveSnapshotStoreTest` cover supplier identity,
independent cursors, private byte storage, exact-file enumeration, directory
boundaries, missing files, and resources whose bytes were not retained.

## Runtime Probes

- `tools/server_perf/resource_index_correctness.snippet` compares intercepted
  Forge output and the indexed tree against vanilla `listPath`, verifies byte
  equality and fallback sources, and checks fresh cursors and missing files.
- `tools/server_perf/resource_index_benchmark.snippet` compares native source
  I/O, the previous supplier path, and the indexed supplier path. A second
  workload compares native file traversal, copied-key enumeration, and direct
  index enumeration. Five warmup rounds precede four measured rounds; each
  capture includes checksums, nanoseconds, and thread-allocated bytes.

These probes require the minimal server's `zmod` archive to be admitted. They
have not been executed by the resource subagent. No speedup or whole-server
effect is claimed before the root agent runs the build and runtime captures.

Parsed JSON AST caching was not added in this pass. It would retain another
object graph and require a deep copy at every consumer, so it needs a separate
parse-versus-copy memory and timing experiment before being justified. This
change instead removes repeated work already visible in the source hot path.
