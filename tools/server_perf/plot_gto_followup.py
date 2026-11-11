"""Aggregate warmed in-server GTO follow-up probes; retain losses in the output."""
from __future__ import annotations

import csv
import json
from pathlib import Path
from statistics import median

import matplotlib.pyplot as plt

ROOT = Path(__file__).resolve().parents[2]
RUN = Path('G:/emoney-void-suit-test')
OUTPUT = ROOT / 'docs/benchmarks'


def capture(phase: str, stem: str) -> dict:
    response = json.loads((RUN / 'evidence' / f'{phase}-{stem}.json').read_text(encoding='utf-8'))
    if not response.get('ok'):
        raise ValueError(f'Failed probe: {phase}/{stem}')
    value = response['result']['result']
    return json.loads(value[value.index('{'):value.rindex('}') + 1])


def select(samples: list[dict], **criteria) -> list[dict]:
    return [row for row in samples if row['round'] > 0 and
            all(row[key] == value for key, value in criteria.items())]


def compare(group: str, label: str, before: list[dict], after: list[dict],
            before_name='native', after_name='optimized') -> dict:
    def summarize(rows):
        if not rows:
            raise ValueError(f'No measured rows: {label}')
        return {'medianNanosPerOp': median(row['nanosPerOp'] for row in rows),
                'medianBytesPerOp': median(row['bytesPerOp'] for row in rows),
                'rounds': len(rows), 'iterationsPerRound': rows[0]['iterations'],
                'checksums': [row['checksum'] for row in rows]}
    left, right = summarize(before), summarize(after)
    if left['checksums'] != right['checksums']:
        raise ValueError(f'Workload checksum mismatch: {label}')
    return {'group': group, 'label': label, 'beforeName': before_name, 'afterName': after_name,
            'before': left, 'after': right,
            'latencyReductionPercent': 100 * (1 - right['medianNanosPerOp'] / left['medianNanosPerOp']),
            'allocationReductionPercent': 100 * (1 - right['medianBytesPerOp'] / left['medianBytesPerOp'])
            if left['medianBytesPerOp'] else None}


def chart(groups: list[tuple[str, list[dict]]], path: Path):
    plt.rcParams.update({'font.size': 9, 'axes.titlesize': 11})
    fig, axes = plt.subplots(4, 2, figsize=(17, 20), constrained_layout=True)
    for index, (title, rows) in enumerate(groups):
        for column, (metric, unit) in enumerate((('medianNanosPerOp', 'ns / operation'),
                                                ('medianBytesPerOp', 'bytes / operation'))):
            ax = axes[index, column]
            y = list(range(len(rows)))
            for offset, name, color in ((-0.18, 'before', '#65798d'), (0.18, 'after', '#329b78')):
                values = [row[name][metric] for row in rows]
                bars = ax.barh([value + offset for value in y], [max(0.5, v) for v in values],
                               height=0.36, color=color, label=name.title())
                ax.bar_label(bars, labels=[f'{value:,.1f}' for value in values], padding=3, fontsize=7)
            ax.set_yticks(y, [row['label'] for row in rows])
            ax.invert_yaxis()
            ax.set_xscale('log')
            ax.set_xlim(right=max(row[name][metric] for row in rows for name in ('before', 'after')) * 2)
            ax.set_xlabel(f'{unit} (log scale; lower is better)')
            ax.set_title(f'{title}: {"latency" if column == 0 else "allocation"}')
            ax.set_axisbelow(True)
            ax.grid(axis='x', alpha=0.2)
    handles, labels = axes[0, 0].get_legend_handles_labels()
    fig.legend(handles, labels, loc='lower center', bbox_to_anchor=(0.5, -0.008), ncol=2)
    fig.suptitle('SunlitIndustryCompat 0.7.0 | GTO follow-up probes | 2026-11-11\n'
                 'Warmed method benchmarks, 18-mod isolated server; not whole-pack TPS or heap', fontsize=15)
    fig.savefig(path, dpi=170, bbox_inches='tight')
    plt.close(fig)


def main():
    phase = 'gto-followup-final'
    muffler = capture(phase, 'gt_muffler_recovery_benchmark')
    sunlight = capture(phase, 'gt_sunlight_benchmark')
    resource = capture(phase, 'resource_index_benchmark')
    preview = capture('gto-followup-release', 'gt_preview_benchmark')
    rows = []
    muffler_rows = []
    for tier in ('lv', 'mv', 'hv', 'ev', 'iv', 'luv', 'zpm', 'uv'):
        for count in (0, 1, 4):
            scenario = f'gtceu:{tier}_muffler_hatch_full_inventory_{count}_entries'
            row = compare('muffler', f'{tier.upper()}, {count} recovery entries',
                          select(muffler['samples'], scenario=scenario, optimized=False),
                          select(muffler['samples'], scenario=scenario, optimized=True))
            muffler_rows.append(row)
    rows.extend(muffler_rows)
    sun_rows = [compare('sunlight', name.replace('_', ' '),
                        select(sunlight['samples'], scenario=name, optimized=False),
                        select(sunlight['samples'], scenario=name, optimized=True))
                for name in ('clear_day', 'clear_night', 'rainy_day', 'thunder_day')]
    rows.extend(sun_rows)
    resource_rows = []
    for label, workload, native, previous, indexed in (
            ('Source open', 'source-open', 'native-source', 'previous-supplier', 'indexed-supplier'),
            ('Recipe tree list', 'tree-list', 'native-list', 'previous-list-copy', 'indexed-list')):
        for mode, suffix in ((native, ' vs native'), (previous, ' vs previous cache')):
            resource_rows.append(compare('resources', label + suffix,
                select(resource['samples'], workload=workload, mode=mode),
                select(resource['samples'], workload=workload, mode=indexed), mode, indexed))
    rows.extend(resource_rows)
    preview_rows = [compare('preview', name.replace('_', ' '),
                            select(preview['preview'], machine=name, cached=False),
                            select(preview['preview'], machine=name, cached=True), 'native', 'snapshot palette')
                    for name in ('assembly_line', 'large_chemical_reactor', 'zpm_fusion_reactor', 'research_station')]
    rows.extend(preview_rows)
    manifest = json.loads((RUN / 'VOID_SUIT_TEST.json').read_text(encoding='utf-8'))
    payload = {
        'date': '2026-11-11', 'version': '0.7.0',
        'environment': {'minecraft': '1.20.1', 'forge': '47.4.23', 'java': '17.0.18',
                        'gtceu': '7.5.3', 'ae2': '15.4.10', 'modJarCount': len(manifest['mods']),
                        'heap': '2G initial / 6G max', 'recipeCount': 42493, 'kubejsProbeCount': 1},
        'method': 'Server-thread A/B in the same transformed JVM. Discard negative warmup rounds and round 0. '
                  'Medians of four rounds (three for previews); raw checksums must agree. '
                  'ThreadMXBean counts current-thread allocation, not retained memory or whole-process allocation.',
        'evidencePhases': {'mufflerSunlightResources': phase, 'finalPreview': 'gto-followup-release'},
        'sourceManifest': manifest['mods'], 'mufflerCoverage': muffler['coverage'],
        'javdLoaded': sunlight['javdLoaded'], 'results': rows,
    }
    OUTPUT.mkdir(parents=True, exist_ok=True)
    stem = OUTPUT / 'gto-followup-20261111'
    stem.with_suffix('.json').write_text(json.dumps(payload, indent=2), encoding='utf-8')
    with stem.with_suffix('.csv').open('w', newline='', encoding='utf-8') as stream:
        fields = ['group', 'label', 'beforeName', 'afterName', 'beforeNs', 'afterNs',
                  'latencyReductionPercent', 'beforeBytes', 'afterBytes', 'allocationReductionPercent']
        writer = csv.DictWriter(stream, fieldnames=fields)
        writer.writeheader()
        for row in rows:
            writer.writerow({key: row[key] for key in fields if key in row} |
                            {'beforeNs': row['before']['medianNanosPerOp'],
                             'afterNs': row['after']['medianNanosPerOp'],
                             'beforeBytes': row['before']['medianBytesPerOp'],
                             'afterBytes': row['after']['medianBytesPerOp']})
    selected_mufflers = [row for row in muffler_rows if row['label'] in {
        'LV, 0 recovery entries', 'UV, 0 recovery entries', 'LV, 1 recovery entries',
        'UV, 1 recovery entries', 'LV, 4 recovery entries', 'IV, 4 recovery entries',
        'ZPM, 4 recovery entries', 'UV, 4 recovery entries'}]
    chart([('Muffler: native vs loop (full output inventory)', selected_mufflers),
           ('Sunlight: native vs per-call short circuit', sun_rows),
           ('Immutable resource index (two reference paths)', resource_rows),
           ('Preview generation (optional cache)', preview_rows)], stem.with_suffix('.png'))
    print(json.dumps({'results': len(rows), 'json': str(stem.with_suffix('.json')),
                      'chart': str(stem.with_suffix('.png'))}))


if __name__ == '__main__':
    main()
