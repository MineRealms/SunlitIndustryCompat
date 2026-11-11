# Code Quality Review - 2026-11-11

Scope: independently checked the 13 external review findings against the current
SunlitIndustryCompat source and the installed GTCEu 7.5.3 / AE2 15.4.10 / TianGong
1.40.03 APIs. This review changes failure handling and cache admission; it makes
no new benchmark or client-rendering claims.

## Findings and Resolution

| # | Assessment | Resolution |
|---|---|---|
| 1 | Real exception-isolation gap | Deferred ME flushing catches and logs each target's runtime exception, then continues the batch. The native hatch buffer retains authoritative contents. VM/linkage errors are not swallowed. |
| 2 | Real cache-admission limitation | A full aggregate budget now evicts least-recently-used snapshots instead of permanently blacklisting new packs. `retainedBytes` decreases on eviction. Intrinsically oversized snapshots still use native resources; their negative entries are bounded. Changing `memoryMiB` recreates the store, allowing a previously oversized pack to be reconsidered. |
| 3 | Real compatibility failure mode | ALI patching first counts supported bytecode matches. If the count differs from one, it logs and leaves the entire target untouched. No partial rewrite or startup exception from this shape check. |
| 4 | Real bad-data failure mode | Compact-cell expansion validates before committing and returns false with a warning on invalid data. The AE2 inventory quarantines unreadable cells, rejecting insertion/extraction and blocking native persistence. This preserves the complete original tag. |
| 5 | Not a semantic regression | Native TianGong `Recipes.all` uses `stream().toList()`, which is already unmodifiable. Both installed consumers use read-only stream operations. Keep the immutable cached result. |
| 6 | Native identity sharing preserved | Shape capture uses an identity palette, so it shares only objects already shared in the original pages. Every `copy()` creates a new mutable object palette. Native GT shape construction also shares candidate BlockInfo objects. Clarified the documentation. |
| 7 | Intentional diagnostic interface; probe cleanup needed | Recording remains off by default and controlled only by test tools. Both recording probes now disable it in `finally`, including failure paths. No permanent recording switch added to normal gameplay. |
| 8 | Bounded retention, not a demonstrated world leak | Release clears recipe, holder, handler, chance-cache, group-owner and collection contents on the calling thread. A long-lived worker may retain up to four empty workspaces until thread exit; server lifecycle cleanup only removes its own thread-local values. Documented the limit. |
| 9 | Real defensive gap | A null archive resource root is skipped before root-identity checks. |
| 10 | Real small cache-efficiency issue | Repeated fuzzy-range puts update the existing slot without consuming another ring slot. |
| 11 | Separate defensive bounds | The 256 limit applies to source capability entries; 512 checks resulting working lists at release. Kept both and clarified the documentation. |
| 12 | Invariant is valid; rejection order was unsafe | Out-of-order close still throws, but now checks the top lease before popping it, so rejected misuse leaves the stack intact and later cleanup can succeed. |
| 13 | Real small cache-efficiency issue | Digest metadata uses bounded access-order LRU eviction of one entry instead of clearing all 2,048 entries together. |

The 32 MiB archive budget counts estimated indexed resource snapshots, not the
entire jar size. Json bytes are selectively retained and noncacheable resources
use native streams. LRU eviction may reread a snapshot from disk under a working
set larger than the budget; it does not establish an overall speed improvement.

## Corrupt AE2 Cell Policy

Simply catching decode errors and continuing native writes is unsafe: native AE2
can interpret missing keys as empty contents and remove `amts` / `ic` during
`saveChanges`. The quarantine therefore blocks mutations and native persistence,
including simulated insertion/extraction. It applies even when compact writing
is disabled, so previously written cells remain protected.

The payload is not repaired automatically. Repair from a supported reader or
backup, then reopen the inventory. The writer remains off by default.

## Verification

Regression checks cover snapshot eviction/readmission and accounting, an exception
in the middle of an ME flush batch, supported/changed ALI bytecode, scratch lease
order, malformed compact metadata and duplicate fuzzy-cache slots.

The isolated-server probe additionally checks real compact-cell reopening and
eight corrupt/unknown/oversized/ambiguous payload cases with immediate/deferred
save providers, repeated persistence and reopening.

Final results:

- Gradle 8.14.5 / Java 17.0.18, offline `test build`: 65 tests in 17 suites,
  zero failures and zero errors; build passed.
- Isolated 18-mod dedicated server `G:/emoney-void-suit-test` reached ready state
  in 78.2 seconds. The recipe count stayed at 42,493 with one reload probe recipe.
- Default-off/master/dimension-policy probe passed: 7 transformed target classes
  and 18 dimension selection checks.
- AE2 compact-cell probe passed: 24 real item variants round-tripped, 8 quarantine
  scenarios preserved full NBT through save and reopen, writer remained disabled.
- Resource-index probe passed: 4 trees, 305 byte comparisons, 144 cached suppliers,
  8,428 native fallback suppliers and 10 independent cursor checks.
- Test evidence: `G:/emoney-void-suit-test/evidence/review-fixes-*.json` and
  `server-review-fixes-smoke-stdout.log`. The production server and client were not
  changed; no new benchmark or client rendering test was performed.
