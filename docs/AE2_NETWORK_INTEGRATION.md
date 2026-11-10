# AE2 Network, Fuzzy Search And Cells

Target: AE2 15.4.10 on Forge 1.20.1. Reference: GTOCore's NetworkStorage,
KeyCounter/FuzzyPriorityList and BasicCellInventory mixins. These modules adapt the
ideas to the installed API; they do not import GTO's modified AEKeyMap or change
custom addon key types into item/fluid slots.

## Modules

`performance/ae2/network` owns topology snapshots, the pending-removal identity
index, fuzzy range views, primitive cell accounting and the versioned cell codec.
`mixin/performance/ae2/network` contains the narrow AE2 integration points.
No helper owns a global network, level, item stack or crafting job reference.

## Network Routing

- The native priority map remains authoritative. Mount/unmount still execute
  native queueing; each invalidates a lazy immutable routing snapshot.
- Insertion preserves descending priority and preferred-first/non-preferred
  second-pass selection within every priority. Extraction preserves ascending
  priority and original mount order within each priority.
- Inventory calls retain the original key, action mode and security source.
  There is no cached acceptance decision, stored amount or security result.
- Nested insert/extract/enumeration retains the native `mountsInUse` guard.
- Pending unmounts use a lazy identity index instead of repeatedly scanning the
  operation queue. Flushing clears the index before native queue playback.
- Duplicate mounts are retained. GTO's owner deduplication is not copied because
  it would change this AE2 version's storage semantics.

## Fuzzy Search

Native primary-key indexes, comparators and damage ranges remain authoritative.
Each fuzzy variant map retains at most four native live range views. A live view
sees direct entry mutations, iterator removal, clear and later insertion; no
cached match collection or copied amount index can become stale.

Misses reuse immutable integer range bounds from a per-thread 128-slot cache.
The bounds contain only damage integers, not keys or levels. The thread-local
cache avoids cross-thread mutation between crafting workers and the server.
Custom keys still enter their native AE2 indexes, and all five fuzzy modes keep
their original inclusive/exclusive breakpoints.

## Cell Accounting

`saveChanges` uses a primitive long iterator instead of boxing each stored amount
as `Long`. Count arithmetic, dirty state, save-provider callbacks and persistence
remain native. Amounts are recomputed on every change; direct map mutation cannot
invalidate a retained total.

## Optional Compact Cell Format

Compact writing is **off by default**. The reader must load on both client and
server, including when writing is disabled. Enabling this requires matching
SunlitIndustryCompat versions on both sides.

- Stores a compressed version-1 key list inside the cell's own NBT; native
  `amts`, `ic`, upgrades, partition and other tags stay in their normal places.
- Preserves arbitrary generic-key NBT without resolving or rewriting addon IDs.
- No UUID database, shared-cell alias or world-external storage is introduced.
  ItemStack copies still own independent contents.
- Checks version, declared size (maximum 16 MiB), CRC32 and key/amount counts
  before modifying a cell. Invalid payloads fail explicitly and stay untouched.
- Empty, small or poorly compressible cells keep their complete native format.
- Opening a compact cell always expands it to native keys first. Disabling
  compact writing and reopening/saving every affected cell migrates it back.
  Unopened cells still require this reader; do not remove the mod first.

Compression trades CPU and temporary buffers for smaller retained/serialized NBT.
Chunk and packet compression already exist, so this is not a claim of smaller
region files or faster I/O. The prototype is opt-in until populated-drive and
addon round trips are measured. It intentionally avoids GTO's UUID-backed save
format and capacity/type-limit changes.

## Integration Requirements

Root config methods:

- `ae2NetworkRouting()` default true.
- `ae2FuzzyBounds()` default true (also gates live range-view reuse).
- `ae2CellAccounting()` default true.
- `ae2CompactCellNbt()` default false; always-readable format regardless of flag.

Register these mixins under the existing exact-version/conflict gates:

- `performance.ae2.network.AE2NetworkRoutingMixin`
- `performance.ae2.network.AE2FuzzyBoundsMixin`
- `performance.ae2.network.AE2FuzzySearchInvoker`
- `performance.ae2.network.AE2FuzzyRangeViewMixin`
- `performance.ae2.network.AE2CellAccountingMixin`
- `performance.ae2.network.AE2CompactCellNbtMixin` (reader on both distributions)

## Validation

Unit coverage: 1,000 randomized priority topologies; same-priority ordering,
duplicate mounts, queue snapshot isolation and identity removal; fuzzy range
boundaries, collision isolation, thread ownership and live-view mutation;
cell count arithmetic including overflow; lossless arbitrary-key codec round
trips, independent copies, malformed checksums/versions/counts and legacy cells.

Rosetta probes:

- `ae2_network_correctness.snippet`: actual NetworkStorage preferred routing,
  simulation, source propagation, reentrancy, queued mount/unmount; all native
  damage buckets and mutation of returned fuzzy views.
- `ae2_network_benchmark.snippet`: 256 mounts in 32 priorities; full traversal
  insert/extract, fuzzy lookup and 63-type cell accounting, with five warm-ups
  and four recorded rounds. Run identical workloads with mixins on/off.
- `ae2_compact_cell_correctness.snippet`: arbitrary-key codec fixture plus real
  AE2 cell reopen/extract; repeat with compact writing enabled and disabled.
- `ae2_compact_cell_benchmark.snippet`: explicit compression/decompression cost.

Build and live-server execution are coordinated by root. These probes do not
constitute a complete populated ME-network or addon compatibility benchmark.
