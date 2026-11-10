"""Parse Rosetta GT server benchmark captures and draw a reproducible summary chart."""
from __future__ import annotations

import csv
import argparse
import json
import re
from pathlib import Path
from statistics import median

import matplotlib.pyplot as plt

ROOT = Path(__file__).resolve().parents[2]
EVIDENCE = Path('G:/emoney-void-suit-test/evidence')
OUTPUT = ROOT / 'docs' / 'benchmarks'


def capture(name: str) -> dict:
    response = json.loads((EVIDENCE / name).read_text(encoding='utf-8'))
    text = response['result']['result']
    match = re.search(r'PASS (\{.*\})\s+\[\d+ms\]$', text, re.DOTALL)
    if not response.get('ok') or not match:
        raise ValueError(f'Invalid benchmark capture: {name}')
    return json.loads(match.group(1))


def summarize(payload: dict, key: str) -> dict[str, dict]:
    samples = payload[key]
    result = {}
    for scenario in sorted({sample['scenario'] for sample in samples}):
        measured = [sample for sample in samples if sample['scenario'] == scenario and sample['round'] > 0]
        result[scenario] = {
            'medianNanosPerOp': median(sample['nanosPerOp'] for sample in measured),
            'medianBytesPerOp': median(sample['bytesPerOp'] for sample in measured),
            'rounds': len(measured),
            'iterationsPerRound': measured[0]['iterations'],
            'samples': measured,
        }
    return result


def bar(ax, labels, baseline, optimized, ylabel, title, unit_scale=1.0, unit=''):
    x = list(range(len(labels)))
    width = 0.34
    before = [value / unit_scale for value in baseline]
    after = [value / unit_scale for value in optimized]
    left = ax.bar([i - width / 2 for i in x], before, width, label='Baseline', color='#58758f')
    right = ax.bar([i + width / 2 for i in x], after, width, label='Optimized', color='#38a68b')
    ax.set_xticks(x, labels)
    ax.set_ylabel(ylabel)
    ax.set_title(title)
    ax.grid(axis='y', alpha=0.22)
    ax.set_axisbelow(True)
    def label(value):
        return (f'{value:,.0f}' if value >= 100 or float(value).is_integer() else f'{value:.1f}') + unit
    ax.bar_label(left, labels=[label(value) for value in before], padding=3, fontsize=8)
    ax.bar_label(right, labels=[label(value) for value in after], padding=3, fontsize=8)
    ax.set_ylim(0, max(before + after) * 1.18)


def main() -> None:
    base = capture('clean-baseline-warm-benchmark.json')
    opt = capture('clean-optimized-warm-benchmark.json')
    if base['registeredRecipeCount'] != opt['registeredRecipeCount']:
        raise ValueError('Baseline and optimized recipe sets differ')
    if base['gtRecipeId'] != opt['gtRecipeId'] or base['inputHandlers'] != opt['inputHandlers']:
        raise ValueError('GT benchmark workloads differ')
    base_recipe = summarize(base, 'recipeSamples')
    opt_recipe = summarize(opt, 'recipeSamples')
    base_groups = summarize(base, 'groupSamples')
    opt_groups = summarize(opt, 'groupSamples')

    metrics = []
    for scenario in ('empty_recipe_io', 'simulated_recipe_match'):
        before = base_recipe[scenario]
        after = opt_recipe[scenario]
        metrics.append({
            'kind': 'recipe_path', 'scenario': scenario,
            'baselineMedianNanosPerOp': before['medianNanosPerOp'],
            'optimizedMedianNanosPerOp': after['medianNanosPerOp'],
            'latencyReductionPercent': 100 * (1 - after['medianNanosPerOp'] / before['medianNanosPerOp']),
            'baselineMedianBytesPerOp': before['medianBytesPerOp'],
            'optimizedMedianBytesPerOp': after['medianBytesPerOp'],
            'allocationReductionPercent': 100 * (1 - after['medianBytesPerOp'] / before['medianBytesPerOp']),
            'rounds': before['rounds'], 'iterationsPerRound': before['iterationsPerRound'],
        })

    for phase, groups in (('baseline', base_groups), ('optimized', opt_groups)):
        native = groups['native_group_build']
        cached = groups['cached_group_lookup']
        metrics.append({
            'kind': 'handler_group_microbenchmark', 'scenario': f'{phase}: native build vs warm cache',
            'nativeMedianNanosPerOp': native['medianNanosPerOp'],
            'cachedMedianNanosPerOp': cached['medianNanosPerOp'],
            'latencyReductionPercent': 100 * (1 - cached['medianNanosPerOp'] / native['medianNanosPerOp']),
            'nativeMedianBytesPerOp': native['medianBytesPerOp'],
            'cachedMedianBytesPerOp': cached['medianBytesPerOp'],
            'allocationReductionPercent': 100 * (1 - cached['medianBytesPerOp'] / native['medianBytesPerOp']),
            'rounds': native['rounds'], 'iterationsPerRound': native['iterationsPerRound'],
        })

    OUTPUT.mkdir(parents=True, exist_ok=True)
    (OUTPUT / 'gto-server-benchmark-20261111.json').write_text(json.dumps({
        'date': '2026-11-11',
        'environment': {
            'minecraft': '1.20.1', 'forge': '47.4.23', 'java': '17.0.18',
            'gtceu': '7.5.3', 'kubejs': '2001.6.5-build.16',
            'heap': '2G initial / 6G max', 'modJarCount': 18, 'recipeCount': opt['registeredRecipeCount'],
            'kubejsProbeCount': opt['probeRecipeCount'], 'inputHandlers': opt['inputHandlers'],
        },
        'baselineCounters': base['optimizationCounters'],
        'optimizedCounters': opt['optimizationCounters'],
        'metrics': metrics,
        'rawSamples': {'baseline': {'recipes': base_recipe, 'groups': base_groups},
                       'optimized': {'recipes': opt_recipe, 'groups': opt_groups}},
        'rawCaptures': {'baseline': base, 'optimized': opt},
    }, indent=2), encoding='utf-8')

    with (OUTPUT / 'gto-server-benchmark-20261111.csv').open('w', newline='', encoding='utf-8') as stream:
        fields = sorted({key for row in metrics for key in row})
        writer = csv.DictWriter(stream, fieldnames=fields)
        writer.writeheader()
        writer.writerows(metrics)

    plt.rcParams.update({'font.size': 9, 'axes.titlesize': 10, 'figure.titlesize': 14})
    fig, axes = plt.subplots(2, 3, figsize=(15, 7.2), constrained_layout=True)
    recipe_labels = ['Empty I/O', 'Simulated match']
    recipe_cases = ('empty_recipe_io', 'simulated_recipe_match')
    bar(axes[0, 0], recipe_labels,
        [base_recipe[key]['medianNanosPerOp'] for key in recipe_cases],
        [opt_recipe[key]['medianNanosPerOp'] for key in recipe_cases],
        'ns / call', 'Recipe path latency')
    bar(axes[1, 0], recipe_labels,
        [base_recipe[key]['medianBytesPerOp'] for key in recipe_cases],
        [opt_recipe[key]['medianBytesPerOp'] for key in recipe_cases],
        'bytes / call', 'Recipe path allocation')
    axes[0, 0].legend(loc='upper left')

    group_cases = ('native_group_build', 'cached_group_lookup')
    bar(axes[0, 1], ['Native build', 'Warm cache'],
        [base_groups[key]['medianNanosPerOp'] for key in group_cases],
        [opt_groups[key]['medianNanosPerOp'] for key in group_cases],
        'ns / call', 'Handler grouping latency')
    bar(axes[1, 1], ['Native build', 'Warm cache'],
        [base_groups[key]['medianBytesPerOp'] for key in group_cases],
        [opt_groups[key]['medianBytesPerOp'] for key in group_cases],
        'bytes / call', 'Handler grouping allocation')

    axes[0, 2].axis('off')
    axes[0, 2].text(0.03, 0.94,
        'Runtime checks\n\n'
        f"Recipes: {base['registeredRecipeCount']:,} in both runs\n"
        f"KubeJS probe: {base['probeRecipeCount']} / {opt['probeRecipeCount']}\n"
        f"Cached handlers: {opt['inputHandlers']}\n"
        f"Empty IO hits: {opt['optimizationCounters']['EMPTY_IO']:,}\n"
        f"Simulation hits: {opt['optimizationCounters']['SIMULATION']:,}\n"
        f"Group cache hits: {opt['optimizationCounters']['GROUP_HIT']:,}\n"
        f"Cache fallbacks: {opt['optimizationCounters']['GROUP_FALLBACK']:,}",
        va='top', ha='left', fontsize=11, linespacing=1.45)
    axes[1, 2].axis('off')
    axes[1, 2].text(0.03, 0.94,
        'Method\n\n'
        'Dedicated Forge server; same 18 mod jars\n'
        'Java 17; 2G initial / 6G max heap\n'
        'Server-thread calls; 5 warm-up + 4 recorded rounds\n'
        'Round 0 excluded; median of rounds 1-3\n'
        'Grouping microbenchmark: 14 synthetic handlers',
        va='top', ha='left', fontsize=10, linespacing=1.45)
    fig.suptitle('SunlitIndustryCompat server optimization benchmark | 2026-11-11')
    fig.savefig(OUTPUT / 'gto-server-benchmark-20261111.png', dpi=180)
    print(json.dumps({'metrics': metrics, 'chart': str(OUTPUT / 'gto-server-benchmark-20261111.png')}, indent=2))


def ae2_main() -> None:
    base = capture('clean-baseline-warm-ae2-benchmark.json')
    opt = capture('clean-optimized-warm-ae2-benchmark.json')
    if base['registeredRecipeCount'] != opt['registeredRecipeCount']:
        raise ValueError('Baseline and optimized recipe sets differ')
    cases = {'itemSamples': 'ItemLike', 'fluidSamples': 'Fluid', 'fluidStackSamples': 'FluidStack'}
    metrics = []
    for key, name in cases.items():
        before = [s for s in base[key] if s['round'] > 0]
        after = [s for s in opt[key] if s['round'] > 0]
        base_ns = median(s['nanosPerOp'] for s in before)
        opt_ns = median(s['nanosPerOp'] for s in after)
        base_bytes = median(s['bytesPerOp'] for s in before)
        opt_bytes = median(s['bytesPerOp'] for s in after)
        metrics.append({
            'scenario': name, 'baselineMedianNanosPerOp': base_ns,
            'optimizedMedianNanosPerOp': opt_ns,
            'latencyReductionPercent': 100 * (1 - opt_ns / base_ns),
            'baselineMedianBytesPerOp': base_bytes, 'optimizedMedianBytesPerOp': opt_bytes,
            'allocationReductionPercent': 100 * (1 - opt_bytes / base_bytes),
            'rounds': len(before), 'iterationsPerRound': before[0]['iterations'],
        })
    validation = {}
    for label, filename in {
        'keyCorrectness': 'clean-optimized-key-correctness.json',
        'keyCorrectnessAfterReload': 'clean-after-reload-correctness.json',
        'groupCorrectness': 'clean-group-correctness.json',
        'reloadAlternate': 'clean-reload-alternate.json',
        'reloadRemoved': 'clean-reload-removed.json',
        'reloadRestored': 'clean-reload-base.json',
    }.items():
        response = json.loads((EVIDENCE / filename).read_text(encoding='utf-8'))
        result = response['result']['result']
        if not response.get('ok') or not result.startswith('PASS '):
            raise ValueError(f'Validation did not pass: {filename}')
        validation[label] = result
    reload_counts = {
        name: int(re.search(r'recipeCount=(\d+)', validation[name]).group(1))
        for name in ('reloadAlternate', 'reloadRemoved', 'reloadRestored')
    }
    recipe_count = opt['registeredRecipeCount']
    if reload_counts != {'reloadAlternate': recipe_count, 'reloadRemoved': recipe_count - 1,
                         'reloadRestored': recipe_count}:
        raise ValueError(f'Unexpected recipe reload counts: {reload_counts}')
    manifest = json.loads((EVIDENCE.parent / 'VOID_SUIT_TEST.json').read_text(encoding='utf-8'))
    compat_jar = next(mod for mod in manifest['mods'] if mod['file'].startswith('sunlit-industry-compat-'))
    OUTPUT.mkdir(parents=True, exist_ok=True)
    (OUTPUT / 'ae2-server-benchmark-20261111.json').write_text(json.dumps({
        'date': '2026-11-11',
        'environment': {'minecraft': '1.20.1', 'forge': '47.4.23', 'java': '17.0.18',
                        'ae2': '15.4.10', 'gtceu': '7.5.3', 'modJarCount': 18,
                        'kubejs': '2001.6.5-build.16', 'heap': '2G initial / 6G max',
                        'recipeCount': opt['registeredRecipeCount'], 'compatJar': compat_jar},
        'method': 'Server-thread factory microbenchmark; identity consumed; diagnostic counters enabled; '
                  'median of rounds 1-3; not a network TPS/heap/GC measurement.',
        'metrics': metrics, 'validation': validation,
        'rawSamples': {'baseline': base, 'optimized': opt},
    }, indent=2), encoding='utf-8')
    with (OUTPUT / 'ae2-server-benchmark-20261111.csv').open('w', newline='', encoding='utf-8') as stream:
        writer = csv.DictWriter(stream, fieldnames=list(metrics[0]))
        writer.writeheader()
        writer.writerows(metrics)
    plt.rcParams.update({'font.size': 10, 'axes.titlesize': 11, 'figure.titlesize': 15})
    fig, axes = plt.subplots(2, 2, figsize=(12.8, 7.5), constrained_layout=True)
    labels = list(cases.values())
    bar(axes[0, 0], labels, [m['baselineMedianNanosPerOp'] for m in metrics],
        [m['optimizedMedianNanosPerOp'] for m in metrics], 'ns / call', 'Default key factory latency')
    bar(axes[0, 1], labels, [m['baselineMedianBytesPerOp'] for m in metrics],
        [m['optimizedMedianBytesPerOp'] for m in metrics], 'bytes / call', 'Default key factory allocation')
    axes[0, 0].legend(loc='upper right')
    axes[0, 0].set_ylim(0, max(m['baselineMedianNanosPerOp'] for m in metrics) * 1.28)
    axes[0, 1].set_ylim(0, max(m['baselineMedianBytesPerOp'] for m in metrics) * 1.28)
    for ax in axes[1]:
        ax.axis('off')
    axes[1, 0].text(0.03, 0.94,
        'Correctness and reload checks\n\n'
        'Item/fluid NBT snapshots and empty stacks: PASS\n'
        'Mekanism battery state distinction: PASS\n'
        'GT groups: 300 topologies / 14,400 comparisons\n'
        'KubeJS: input replaced, recipe removed, recipe restored\n'
        f"Recipe count: {recipe_count:,} -> {reload_counts['reloadRemoved']:,} -> {recipe_count:,}",
        va='top', linespacing=1.5)
    axes[1, 1].text(0.03, 0.94,
        'Measurement scope\n\n'
        'AE2 15.4.10 / GTCEu 7.5.3 / Java 17\n'
        'Same 18-mod-jar server; 2G initial / 6G maximum heap\n'
        '5 warm-up rounds; 200k calls/round; median 1-3\n'
        'Warm default keys; diagnostic counter overhead included\n'
        'Factory savings only; no whole-network TPS/GC claim',
        va='top', linespacing=1.5)
    fig.suptitle('SunlitIndustryCompat | AE2 server key cache benchmark | 2026-11-11')
    chart = OUTPUT / 'ae2-server-benchmark-20261111.png'
    fig.savefig(chart, dpi=180)
    print(json.dumps({'metrics': metrics, 'chart': str(chart)}, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--ae2', action='store_true')
    args = parser.parse_args()
    ae2_main() if args.ae2 else main()
