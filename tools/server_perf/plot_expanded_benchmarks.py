"""Summarize the final isolated-server A/B probes and the rejected candidates."""
from __future__ import annotations

import csv
import json
from pathlib import Path
from statistics import median

import matplotlib.pyplot as plt

ROOT = Path(__file__).resolve().parents[2]
EVIDENCE = Path('G:/emoney-void-suit-test/evidence')
OUTPUT = ROOT / 'docs' / 'benchmarks'


def capture(phase: str, stem: str) -> dict:
    path = EVIDENCE / f'{phase}-{stem}.json'
    response = json.loads(path.read_text(encoding='utf-8'))
    if not response.get('ok'):
        raise ValueError(f'Failed probe capture: {path}')
    text = response['result']['result']
    start, end = text.find('{'), text.rfind('}')
    if start < 0 or end <= start:
        raise ValueError(f'Invalid probe payload: {path}')
    return json.loads(text[start:end + 1])


def summary(samples: list[dict]) -> dict:
    selected = [sample for sample in samples if sample['round'] > 0]
    if not selected:
        raise ValueError('No recorded benchmark rounds')
    return {
        'medianNanosPerOp': median(sample['nanosPerOp'] for sample in selected),
        'medianBytesPerOp': median(sample['bytesPerOp'] for sample in selected),
        'rounds': len(selected),
        'iterationsPerRound': selected[0]['iterations'],
        'checksums': [sample['checksum'] for sample in selected],
    }


def compare(label: str, before: list[dict], after: list[dict], before_name='native', after_name='optimized') -> dict:
    left, right = summary(before), summary(after)
    return {
        'label': label,
        'beforeName': before_name,
        'afterName': after_name,
        'before': left,
        'after': right,
        'latencyReductionPercent': 100 * (1 - right['medianNanosPerOp'] / left['medianNanosPerOp']),
        'allocationReductionPercent': None if left['medianBytesPerOp'] == 0 else
            100 * (1 - right['medianBytesPerOp'] / left['medianBytesPerOp']),
    }


def only(samples: list[dict], **criteria) -> list[dict]:
    return [sample for sample in samples if all(sample[key] == value for key, value in criteria.items())]


def make_chart(accepted: list[dict], rejected: list[dict], compact: dict, path: Path) -> None:
    plt.rcParams.update({'font.size': 9, 'axes.titlesize': 11, 'figure.titlesize': 16})
    fig, axes = plt.subplots(2, 2, figsize=(16, 13), constrained_layout=True)

    def grouped(ax, rows, metric, title, xlabel):
        y = list(range(len(rows)))
        height = 0.36
        before = [row['before'][metric] for row in rows]
        after = [row['after'][metric] for row in rows]
        if metric == 'medianBytesPerOp':
            before_plot = [max(0.5, value) for value in before]
            after_plot = [max(0.5, value) for value in after]
        else:
            before_plot, after_plot = before, after
        bars_before = ax.barh([value - height / 2 for value in y], before_plot, height,
                              label='Before', color='#65798d')
        bars_after = ax.barh([value + height / 2 for value in y], after_plot, height,
                             label='After', color='#38a68b')
        ax.set_yticks(y, [row['label'] for row in rows])
        ax.invert_yaxis()
        ax.set_xscale('log')
        ax.set_xlabel(xlabel)
        ax.set_title(title)
        ax.grid(axis='x', alpha=0.22)
        ax.set_axisbelow(True)
        for bars, values in ((bars_before, before), (bars_after, after)):
            labels = [f'{value:,.0f}' if metric == 'medianNanosPerOp' else
                      ('0' if value == 0 else f'{value:,.1f}') for value in values]
            ax.bar_label(bars, labels=labels, padding=3, fontsize=7)
        ax.legend(loc='lower right')

    grouped(axes[0, 0], accepted, 'medianNanosPerOp', 'Retained improvements: latency', 'ns / operation (log scale)')
    grouped(axes[0, 1], accepted, 'medianBytesPerOp', 'Retained improvements: allocation', 'bytes / operation (log scale; zero plotted at 0.5)')
    grouped(axes[1, 0], rejected, 'medianNanosPerOp', 'Rejected candidates: latency', 'ns / operation (log scale)')
    ax = axes[1, 1]
    sizes = [compact['beforeBytes'], compact['afterBytes']]
    bars = ax.bar(['Native cell NBT', 'Compact cell payload'], sizes, color=['#65798d', '#38a68b'])
    ax.bar_label(bars, labels=[f'{size:,} B' for size in sizes], padding=4)
    ax.set_ylabel('Serialized fixture size, bytes')
    ax.set_title(f"Compact cell fixture: {compact['reductionPercent']:.1f}% smaller")
    ax.grid(axis='y', alpha=0.22)
    ax.set_axisbelow(True)
    fig.suptitle('SunlitIndustryCompat server optimization A/B | 2026-11-11')
    fig.savefig(path, dpi=180)
    plt.close(fig)


def main() -> None:
    base = capture('expanded-final-baseline', 'benchmark')
    final = capture('expanded-release', 'benchmark')
    if (base['registeredRecipeCount'], base['gtRecipeId'], base['inputHandlers']) != \
            (final['registeredRecipeCount'], final['gtRecipeId'], final['inputHandlers']):
        raise ValueError('GT benchmark workloads differ')

    accepted = []
    for key, label in (('empty_recipe_io', 'GT empty recipe I/O'),
                       ('simulated_recipe_match', 'GT simulated recipe match')):
        accepted.append(compare(label,
            only(base['recipeSamples'], scenario=key), only(final['recipeSamples'], scenario=key)))
    accepted.append(compare('GT handler-group cache',
        only(final['groupSamples'], scenario='native_group_build'),
        only(final['groupSamples'], scenario='cached_group_lookup'), 'native grouping', 'cached lookup'))

    gt_runner = capture('expanded-release', 'gt_runner_benchmark')['samples']
    for scenario, label in (('executing_runner', 'GT executing recipe workspace'),
                            ('simulated_runner', 'GT simulated recipe workspace')):
        accepted.append(compare(label,
            only(gt_runner, scenario=scenario, pooled=False), only(gt_runner, scenario=scenario, pooled=True),
            'unpooled', 'pooled'))

    for prop, label in (('processingPatternDecode', 'AE2 processing-pattern decode'),
                        ('providerPatternSelection', 'AE2 provider-pattern selection')):
        before = capture('expanded-final-baseline', 'ae2_crafting_benchmark')[prop]
        after = capture('expanded-release', 'ae2_crafting_benchmark')[prop]
        accepted.append(compare(label, before, after))

    before = capture('expanded-final-baseline', 'ae2_network_benchmark')
    after = capture('expanded-release', 'ae2_network_benchmark')
    accepted.append(compare('AE2 fuzzy lookup', before['fuzzyQuery'], after['fuzzyQuery']))

    planner_state = capture('expanded-release', 'ae2_planner_state_benchmark')
    accepted.append(compare('AE2 primitive craft-count merge', planner_state['native']['primitiveCraftMerge'],
                            planner_state['optimized']['primitiveCraftMerge']))
    planner = capture('expanded-release', 'ae2_planner_correctness')['nativeGraphMetadataBenchmark']
    accepted.append(compare('AE2 planner graph metadata', planner['native'], planner['optimized']))

    resource = capture('expanded-release', 'resource_source_benchmark')['samples']
    accepted.append(compare('Immutable archive resource lookup', only(resource, cached=False),
                            only(resource, cached=True), 'native source', 'snapshot cache'))

    old_route = capture('expanded-final', 'ae2_network_benchmark')
    old_adjacent = capture('expanded-final', 'ae2_adjacent_world_benchmark')
    rejected = [
        compare('AE2 route override: insert (256 stores)', before['routingInsert'], old_route['routingInsert'],
                'native', 'snapshot override'),
        compare('AE2 route override: extract (256 stores)', before['routingExtract'], old_route['routingExtract'],
                'native', 'snapshot override'),
        compare('AE2 BlockApiCache wrapper',
                capture('expanded-final-baseline', 'ae2_adjacent_world_benchmark')['adjacentCapabilityLookup'],
                old_adjacent['adjacentCapabilityLookup'], 'native', 'custom wrapper'),
    ]
    old_planner = capture('expanded-final', 'ae2_planner_state_benchmark')
    rejected.append(compare('AE2 hot-primary simulation shortcut',
        old_planner['native']['hotInventorySimulation'], old_planner['optimized']['hotInventorySimulation'],
        'native', 'shortcut'))
    rejected.append(compare('AE2 cell-accounting override', before['cellAccounting'], old_route['cellAccounting'],
                            'native', 'primitive override'))

    cell = capture('expanded-release', 'ae2_compact_cell_correctness')
    compact = {
        'beforeBytes': cell['syntheticNbtBytesBefore'],
        'afterBytes': cell['syntheticNbtBytesAfter'],
    }
    compact['reductionPercent'] = 100 * (1 - compact['afterBytes'] / compact['beforeBytes'])

    output = {
        'date': '2026-11-11',
        'environment': {
            'minecraft': '1.20.1', 'forge': '47.4.23', 'java': final['javaVersion'],
            'gtceu': '7.5.3', 'ae2': '15.4.10', 'modJarCount': 18,
            'heap': '2G initial / 6G max', 'recipeCount': final['registeredRecipeCount'],
            'kubejsProbeCount': final['probeRecipeCount'], 'inputHandlers': final['inputHandlers'],
        },
        'method': 'Same isolated 18-mod set, same workloads, fresh JVM baseline vs final; per-run comparisons use five warmups and four measured rounds. Reported values are medians; not whole-pack TPS.',
        'accepted': accepted,
        'rejected': rejected,
        'compactCellFixture': compact,
    }

    OUTPUT.mkdir(parents=True, exist_ok=True)
    json_path = OUTPUT / 'expanded-server-benchmark-20261111.json'
    csv_path = OUTPUT / 'expanded-server-benchmark-20261111.csv'
    png_path = OUTPUT / 'expanded-server-benchmark-20261111.png'
    json_path.write_text(json.dumps(output, indent=2), encoding='utf-8')
    with csv_path.open('w', newline='', encoding='utf-8') as stream:
        fields = ['group', 'label', 'beforeName', 'afterName', 'beforeMedianNs', 'afterMedianNs',
                  'latencyReductionPercent', 'beforeMedianBytes', 'afterMedianBytes', 'allocationReductionPercent']
        writer = csv.DictWriter(stream, fieldnames=fields)
        writer.writeheader()
        for group, rows in (('retained', accepted), ('rejected', rejected)):
            for row in rows:
                writer.writerow({
                    'group': group, 'label': row['label'], 'beforeName': row['beforeName'],
                    'afterName': row['afterName'], 'beforeMedianNs': row['before']['medianNanosPerOp'],
                    'afterMedianNs': row['after']['medianNanosPerOp'],
                    'latencyReductionPercent': row['latencyReductionPercent'],
                    'beforeMedianBytes': row['before']['medianBytesPerOp'],
                    'afterMedianBytes': row['after']['medianBytesPerOp'],
                    'allocationReductionPercent': row['allocationReductionPercent'],
                })
    make_chart(accepted, rejected, compact, png_path)
    print(json.dumps({'json': str(json_path), 'csv': str(csv_path), 'png': str(png_path),
                      'accepted': len(accepted), 'rejected': len(rejected)}, indent=2))


if __name__ == '__main__':
    main()
