"""Parse Rosetta GT server benchmark captures and draw a reproducible summary chart."""
from __future__ import annotations

import csv
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
    ax.bar_label(left, fmt=f'%.3g{unit}', padding=3, fontsize=8)
    ax.bar_label(right, fmt=f'%.3g{unit}', padding=3, fontsize=8)


def main() -> None:
    base = capture('baseline-benchmark.json')
    opt = capture('optimized-benchmark.json')
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
            'heap': '2G initial / 6G max', 'recipeCount': opt['registeredRecipeCount'],
            'kubejsProbeCount': opt['probeRecipeCount'], 'inputHandlers': opt['inputHandlers'],
        },
        'baselineCounters': base['optimizationCounters'],
        'optimizedCounters': opt['optimizationCounters'],
        'metrics': metrics,
        'rawSamples': {'baseline': {'recipes': base_recipe, 'groups': base_groups},
                       'optimized': {'recipes': opt_recipe, 'groups': opt_groups}},
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
        'Dedicated Forge server, same 16-mod closure\n'
        'Java 17; 2G initial / 6G max heap\n'
        'Server-thread calls; 4 rounds per scenario\n'
        'Round 0 excluded as warm-up; median of rounds 1-3\n'
        'Handler microbenchmark uses 14 synthetic input handlers',
        va='top', ha='left', fontsize=10, linespacing=1.45)
    fig.suptitle('SunlitIndustryCompat server optimization benchmark | 2026-11-11')
    fig.savefig(OUTPUT / 'gto-server-benchmark-20261111.png', dpi=180)
    print(json.dumps({'metrics': metrics, 'chart': str(OUTPUT / 'gto-server-benchmark-20261111.png')}, indent=2))


if __name__ == '__main__':
    main()
