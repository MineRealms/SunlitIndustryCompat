# AE2 Fuzzy Search And Cells

Target: AE2 15.4.10 on Forge 1.20.1. This module keeps AE2's native storage
routing and data format authoritative. It adapts only measured fuzzy-search and
cell-accounting hot paths; it does not import GTO's modified key map or alter
custom addon key types.

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

Compact writing is **off by default**. Its reader remains enabled on both client
and server so previously written cells can still be opened when writing is off.
Enabling writing requires matching SunlitIndustryCompat versions on both sides.

- Stores a compressed version-1 key list inside the cell's own NBT; native
  `amts`, `ic`, upgrades, partition and other tags stay in their normal places.
- Preserves arbitrary generic-key NBT without resolving or rewriting addon IDs.
- No UUID database, shared-cell alias or world-external storage is introduced.
  ItemStack copies still own independent contents.
- Checks version, declared size (maximum 16 MiB), CRC32 and key/amount counts
  before modifying a cell. Invalid payloads fail explicitly and stay untouched.
- Empty, small or poorly compressible cells keep their complete native format.
- Opening a compact cell always expands it to native keys first. Disabling
  compact writing and reopening/saving affected cells migrates them back.
  Unopened cells still require this reader; do not remove the mod first.

The synthetic 63-entry custom-key fixture shrank from 25,212 to 1,068 NBT bytes.
The same fixture shows compression adds CPU work and temporary buffers, so this
is a storage-size experiment, not a speed claim. It intentionally avoids GTO's
UUID-backed save format and capacity/type-limit changes.

## Measured And Rejected

An immutable priority-routing snapshot and pending-removal index were tested
against native `NetworkStorage` using 256 stores in 32 priorities. The measured
insert path grew from about 3.5 to 8.0 microseconds per call; extract grew from
about 1.1 to 3.7 microseconds. The override has been removed from the shipped
Mixin set and source. The Rosetta correctness probe remains as a smoke test for
native AE2 priority, simulation, reentrancy and queue semantics.

## Configuration And Mixins

- `ae2FuzzyBounds` is enabled by default.
- `ae2CellAccounting` is enabled by default.
- `ae2CompactCellNbt` is disabled by default; its reader accepts prior compact
  payloads regardless of the write flag.
- `AE2NetworkRoutingMixin` is not shipped: its 256-store benchmark regressed.

Live probes cover native priority-routing semantics, fuzzy damage buckets and
mutable live views, cell accounting, arbitrary-key codec round trips, real AE2
cell reopen/extract, and compact/decompress cost. These microbenchmarks do not
represent a populated player network or region-file size.
