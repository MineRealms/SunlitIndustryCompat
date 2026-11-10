# Immutable Archive Cache Review

Reviewed against Forge 47.4.23 `ResourcePackLoader.createPackForMod`,
`net.minecraftforge.resource.PathPackResources`, and the mapped Minecraft
`PathPackResources.listPath` bytecode.

## Corrected During Review

- Forge's mod pack stores the physical jar in `getSource()` and overrides
  `resolve()` to use the mod file's resource filesystem. Comparing this physical
  path only to `findResource("")` would bypass every ordinary mod pack.
  `ArchiveSourceIdentity` now accepts the exact physical archive or its exact
  resource root.
- Optional resource packs below a subdirectory are bypassed. Their content is
  outside the snapshot's root `data/` and `assets/` index, so answering absence
  from that root index would hide their resources.
- Native fallback resolves through the original protected `resolve()` method.
  Resolving children beneath `getSource()` would try to open `mod.jar/data/...`.
- Native `listPath` can enumerate a single file as well as a directory. The
  snapshot's `listTree` handles both while retaining the directory boundary.

## Coverage Added

- Physical source and resource-filesystem root identities, including rejection
  of a subdirectory pack and a different archive.
- Exact-file and directory listing assertions alongside the existing disk
  round-trip, content replacement, corruption, and byte isolation tests.

No Gradle build or server lifecycle command was run by the review agent.
Root owns runtime verification and the final resource-cache commit.

## Measurement Requirements

- Confirm nonzero actual resource-cache counters on the production-shaped
  isolated Forge launch; helper benchmarks alone do not prove interception.
- Compare recipe counts and content after KubeJS modification/removal/restore.
- Keep cold fingerprint hashing separate from warm resource access timing.
- Client support caches source bytes, not baked model object graphs; no FPS
  improvement can be claimed from server-side resource measurements.
