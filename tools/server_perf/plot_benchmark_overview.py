"""Plot committed benchmark summaries without starting Minecraft or collecting new samples."""
from __future__ import annotations

import csv
import hashlib
import json
import math
from pathlib import Path

import matplotlib

matplotlib.use('Agg')
import matplotlib.pyplot as plt

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / 'docs' / 'benchmarks'
STEM = DATA / 'benchmark-overview-20261111'


def load(name: str) -> tuple[dict, str]:
    raw = (DATA / name).read_bytes()
    return json.loads(raw), hashlib.sha256(raw).hexdigest()


def normalize(row: dict, series: str, group: str, default: str, label: str | None = None) -> dict:
    before, after = row['before'], row['after']
    values = [before['medianNanosPerOp'], after['medianNanosPerOp'],
              before['medianBytesPerOp'], after['medianBytesPerOp']]
    if not all(math.isfinite(value) and value >= 0 for value in values) or min(values[:2]) <= 0:
        raise ValueError(f'Invalid measurement: {row["label"]}')
    return {
        'series': series, 'group': group, 'label': label or row['label'],
        'defaultIn080': default, 'beforeName': row['beforeName'], 'afterName': row['afterName'],
        'beforeNs': values[0], 'afterNs': values[1], 'beforeBytes': values[2], 'afterBytes': values[3],
        'speedup': values[0] / values[1],
        'beforeRounds': before['rounds'], 'afterRounds': after['rounds'],
        'beforeIterationsPerRound': before['iterationsPerRound'],
        'afterIterationsPerRound': after['iterationsPerRound'],
    }


def compact_number(value: float) -> str:
    if value == 0:
        return '0'
    if value < 0.1:
        return f'{value:.4f}'
    for scale, suffix in ((1e6, 'M'), (1e3, 'k')):
        if value >= scale:
            return f'{value / scale:.3g}{suffix}'
    return f'{value:.3g}'


def panel(ax, title: str, rows: list[dict]):
    ax.set_xlim(0, 1)
    ax.set_ylim(len(rows) + 0.7, -2.4)
    ax.axis('off')
    ax.text(0, -1.95, title, fontsize=12, weight='bold', color='#172b34')
    ax.text(0, -0.85, 'Workload', fontsize=8, color='#52606b')
    ax.text(0.47, -0.85, 'Latency ratio: before / candidate', fontsize=8, color='#52606b')
    ax.text(0.82, -0.85, 'Bytes/op: before -> after', fontsize=8, color='#52606b')

    def position(ratio):
        return 0.47 + (math.log2(ratio) + 2) / 11 * 0.26

    for ratio in (0.25, 1, 4, 16, 64, 256):
        x = position(ratio)
        ax.plot([x, x], [-0.1, len(rows) - 0.25], color='#dce3e7', linewidth=0.7,
                linestyle='-' if ratio == 1 else ':', zorder=0)
        ax.text(x, len(rows) + 0.35, f'{ratio:g}x', ha='center', fontsize=7, color='#52606b')
    for y, row in enumerate(rows):
        if y % 2 == 0:
            ax.axhspan(y - 0.43, y + 0.43, color='#f1f5f7', zorder=-1)
        ratio = row['speedup']
        color = '#14816d' if ratio > 1.05 else '#b6424b' if ratio < 0.95 else '#74818a'
        x = position(ratio)
        ax.text(0.005, y, row['label'], va='center', fontsize=8.1, color='#172b34')
        ax.plot([position(1), x], [y, y], color=color, linewidth=2.5, solid_capstyle='round')
        ax.scatter([x], [y], color=color, s=15, zorder=3)
        ax.text(0.74, y, f'{ratio:.2f}x', va='center', fontsize=7.8, color=color, weight='bold')
        sizes = f"{compact_number(row['beforeBytes'])} -> {compact_number(row['afterBytes'])}"
        ax.text(0.82, y, sizes, va='center', fontsize=7.6, family='DejaVu Sans Mono', color='#172b34')


def render(groups: dict[str, list[dict]], compact: dict):
    plt.rcParams.update({'font.family': 'DejaVu Sans', 'font.size': 9, 'savefig.facecolor': 'white'})
    fig, axes = plt.subplots(2, 2, figsize=(23, 21), gridspec_kw={'height_ratios': [1.6, 1]},
                             layout='constrained')
    panel(axes[0, 0], 'A  Server paths enabled by default in 0.8.0', groups['default'])
    panel(axes[0, 1], 'B  Muffler recovery loop: optional, default OFF', groups['muffler'])
    panel(axes[1, 0], 'C  Other experiments: optional, default OFF', groups['optional'])
    ax = axes[1, 1]
    ax.axis('off')
    ax.text(0, 0.96, 'D  Serialized-cell fixture and measurement scope', fontsize=12,
            weight='bold', color='#172b34')
    inset = ax.inset_axes([0.08, 0.59, 0.82, 0.24])
    bars = inset.barh([0, 1], [compact['beforeBytes'], compact['afterBytes']],
                      color=['#7d8c9b', '#14816d'], height=0.5)
    inset.set_yticks([0, 1], ['Native NBT', 'Compact payload'])
    inset.invert_yaxis()
    inset.bar_label(bars, labels=[f"{compact['beforeBytes']:,} B", f"{compact['afterBytes']:,} B"],
                    padding=5, fontsize=10)
    inset.set_xlim(0, compact['beforeBytes'] * 1.23)
    inset.set_xlabel('Serialized bytes in one 63-key fixture; writer default OFF', fontsize=9)
    inset.spines[['top', 'right']].set_visible(False)
    ax.text(0.08, 0.87, f"{compact['reductionPercent']:.1f}% smaller on disk; compression costs CPU and allocations",
            fontsize=10, color='#172b34')
    notes = (
        '55 historical method comparisons, including regressions.\n'
        'Minecraft 1.20.1 | Forge 47.4.23 | Java 17.0.18\n'
        'GTCEu 7.5.3 | AE2 15.4.10 | 18 mod jars | 42,493 recipes\n'
        'Heap: Xms2G / Xmx6G. Medians from each source series.\n\n'
        'Ratio > 1: candidate faster; ratio < 1: candidate slower.\n'
        'Grey: within +/-5% for display, NOT a confidence interval.\n'
        'Byte values count current-thread allocation, not retained heap.\n'
        'Some rows compare a previous cache to the revised index.\n'
        'Separate runs/fixtures must not be averaged into a pack speedup.\n\n'
        'Recorded code: 0.5.x-0.7.x. 0.8.0: correctness checks only.\n'
        'Client LOD / rendering / TianGong cache: no timing samples.\n'
        'No claim about whole-pack TPS, FPS, GC pauses, or process RAM.\n'
        'Full precision, per-row baselines, rounds and provenance: JSON / CSV.'
    )
    ax.text(0.02, 0.47, notes, va='top', fontsize=10, linespacing=1.65, color='#344955')
    fig.suptitle('Sunlit Industry Compat | Benchmark Overview | 2026-11-11\n'
                 'Recorded microbenchmarks; current defaults are policy, not a new measurement',
                 fontsize=19, weight='bold', color='#172b34')
    fig.savefig(STEM.with_suffix('.png'), dpi=150)
    plt.close(fig)


def main():
    core, core_hash = load('expanded-server-benchmark-20261111.json')
    keys, keys_hash = load('ae2-server-benchmark-20261111.json')
    supplement, supplement_hash = load('gto-followup-20261111.json')
    for source in (keys, supplement):
        for key in ('minecraft', 'forge', 'java', 'gtceu', 'ae2', 'modJarCount', 'recipeCount'):
            if source['environment'][key] != core['environment'][key]:
                raise ValueError(f'Environment mismatch: {key}')
    groups = {'default': [], 'muffler': [], 'optional': []}
    for row in core['accepted']:
        label = row['label'].replace('Immutable archive resource lookup', 'Archive source lookup (earlier run)')
        groups['default'].append(normalize(row, 'runtime', 'default', 'on', label))
    for metric in keys['metrics']:
        row = {'label': 'AE2 default key: ' + metric['scenario'], 'beforeName': 'native factory',
               'afterName': 'cached default key'}
        for side, prefix in (('before', 'baseline'), ('after', 'optimized')):
            row[side] = {'medianNanosPerOp': metric[prefix + 'MedianNanosPerOp'],
                         'medianBytesPerOp': metric[prefix + 'MedianBytesPerOp'],
                         'rounds': metric['rounds'], 'iterationsPerRound': metric['iterationsPerRound']}
        groups['default'].append(normalize(row, 'default-keys', 'default', 'on'))
    for row in supplement['results']:
        kind = row['group']
        group = 'default' if kind == 'resources' else 'muffler' if kind == 'muffler' else 'optional'
        label = row['label']
        if kind == 'sunlight':
            label = 'Sunlight: ' + label
        elif kind == 'preview':
            label = 'Preview: ' + label
        groups[group].append(normalize(row, 'resource-world-preview', group,
                                       'on' if group == 'default' else 'off', label))
    for row in core['rejected']:
        groups['optional'].append(normalize(row, 'runtime', 'optional', 'off'))
    rows = [row for group in groups.values() for row in group]
    if len(rows) != 55:
        raise ValueError(f'Unexpected comparison count: {len(rows)}')
    payload = {
        'date': '2026-11-11', 'currentPolicyVersion': '0.8.0',
        'environment': core['environment'],
        'measurementScope': 'Historical warmed method benchmarks; no new measurements in this chart. '
            '0.8.0 has correctness validation only. No whole-pack TPS/FPS/heap/GC claim.',
        'series': [
            {'id': 'runtime', 'sourceSha256': core_hash, 'method': core['method'],
             'roundSelection': 'See each row; medians retain 3 or 4 source rounds.'},
            {'id': 'default-keys', 'sourceSha256': keys_hash, 'method': keys['method'],
             'measuredVersion': '0.5.0'},
            {'id': 'resource-world-preview', 'sourceSha256': supplement_hash,
             'method': supplement['method'], 'measuredVersion': supplement['version']},
        ],
        'comparisons': rows, 'compactCellFixture': core['compactCellFixture'],
    }
    STEM.with_suffix('.json').write_text(json.dumps(payload, indent=2) + '\n', encoding='utf-8')
    with STEM.with_suffix('.csv').open('w', encoding='utf-8', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
    render(groups, core['compactCellFixture'])
    print(json.dumps({'comparisons': len(rows), 'chart': str(STEM.with_suffix('.png'))}))


if __name__ == '__main__':
    main()
