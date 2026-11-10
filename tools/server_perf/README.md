# Dedicated Server Benchmark

These tools target only the marked test directory `G:/emoney-void-suit-test`, port 25584 and
loopback Rosetta port 48793. The production pack supplies the installed jars/configuration, but is
not changed. The stager resolves required `mods.toml`/nested-jar dependencies and refuses to stage
while the test bridge is active. Inspect `server.pid` before launching or removing its marker.

## Reproduce

1. Build with Java 17: `gradle --offline test build`.
2. With the isolated server stopped, run `python -I tools/void_suit/stage_test_server.py`.
3. Run `python -I tools/server_perf/launch_server.py clean-baseline`.
4. Run `benchmark.snippet` with save name `clean-baseline-warm-benchmark.json` and
   `ae2_default_key_benchmark.snippet` with save name `clean-baseline-warm-ae2-benchmark.json`, using
   `python -I tools/void_suit/bridge.py exec <snippet> --save <filename>`.
5. Run the key correctness and recipe status probes. Stop via `bridge.py console stop`, verify the
   recorded process has exited, and remove only the isolated `server.pid` marker.
6. Launch `clean-optimized`, then run the benchmarks with save names
   `clean-optimized-warm-benchmark.json` and `clean-optimized-warm-ae2-benchmark.json`. Save the key correctness
   probe as `clean-optimized-key-correctness.json` and group correctness as `clean-group-correctness.json`.
7. For each of the three variants, run `reload_variant_next.snippet`, `bridge.py console reload`,
   then `reload_variant_check.snippet`. Save the checks as `clean-reload-alternate.json`,
   `clean-reload-removed.json`, and `clean-reload-base.json` respectively. Run the key correctness
   probe again after the reloads and save `clean-after-reload-correctness.json`.
8. Stop the test server, verify exit, and generate the chart with
   `python -I tools/server_perf/plot_benchmarks.py --ae2`.

The baseline startup property skips the server performance mixins. All phases use Java 17,
`-Xms2G -Xmx6G`, the same flat `sunlit_perf_clean` world and fixed settings. Five warm-up rounds run
through the same loop before four recorded rounds; charts use the median of rounds 1-3. Raw captures,
stdout, GC logs and JFR stay
in the isolated `evidence` directory. The chart script writes normalized JSON, CSV and PNG into
`docs/benchmarks` and rejects differing recipe counts or unsuccessful reload checks.

Run the chart script without `--ae2` for GT recipe/group results. Preliminary captures with older
launch-mode names are archived in evidence, but the charts use the final `clean-*` runs only. An
earlier staging script left Create and its navigation addon in the folder despite omitting them
from its dependency manifest. They were backed up outside `mods`; the stager now rejects unexpected
jars and checks the PID marker as well as the bridge port.

Do not infer full-server TPS, post-GC heap or GC pause changes from these factory/path measurements.
The charts preserve individual rounds because JIT and host load affect sub-microsecond timings.
