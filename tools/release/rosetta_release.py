"""Inspect or stage a release through Rosetta; never install or reload a live server."""
import argparse
import base64
import json
import os
from pathlib import Path
import socket
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from package_recent_update import DEFAULT_PACK, ROOT, WORKSPACE, collect, sha

EXPECTED_ROOT = 'C:\\Users\\Administrator\\Downloads\\emoney'


def call(command, arguments=None, timeout=45):
    config = json.loads((WORKSPACE / 'SunlitMarket/tools/rosetta.local.json').read_text(encoding='utf-8-sig'))
    host = os.environ.get('SM_ROSETTA_HOST', config['host'])
    port = int(os.environ.get('SM_ROSETTA_PORT', config['port']))
    token = os.environ.get('SM_ROSETTA_TOKEN', config['token'])
    request = {'token': token, 'cmd': command, 'args': arguments or {}}
    with socket.create_connection((host, port), timeout=30) as connection:
        connection.settimeout(timeout)
        stream = connection.makefile('rwb')
        stream.write((json.dumps(request) + '\n').encode('utf-8'))
        stream.flush()
        response = json.loads(stream.readline())
    if not response.get('ok'):
        raise RuntimeError(str(response.get('error', 'Rosetta request failed')))
    return response['result']


def execute(code):
    response = call('exec', {'code': code})
    return json.JSONDecoder().raw_decode(response['result'])[0]


def inspect(names):
    code = (ROOT / 'tools/release/inspect_server.snippet').read_text(encoding='utf-8')
    code = code.replace('var files = new com.google.gson.JsonArray();',
                        'names.clear(); names.addAll(java.util.List.of(' +
                        ','.join(json.dumps(name) for name in sorted(names)) +
                        '));\nvar files = new com.google.gson.JsonArray();')
    result = execute(code)
    if result['root'].casefold() != EXPECTED_ROOT.casefold():
        raise ValueError('Unexpected remote server directory')
    return result


def read(path, expected):
    response = call('read', {'file': path, 'max': expected['bytes'], 'offset': 0})
    data = base64.b64decode(response['base64'])
    if len(data) != expected['bytes'] or sha(data) != expected['sha256']:
        raise ValueError('Remote file changed during capture: ' + path)
    return data


def upload(path, data):
    call('upload', {'path': path, 'base64': base64.b64encode(data).decode('ascii')}, timeout=60)


def save(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def capture(args):
    entries = collect(args.pack)
    state = inspect(entries)
    save(args.record / 'server-before.json', state)
    changed = []
    for row in state['files']:
        name = row['path']
        if row.get('sha256') == sha(entries[name]):
            continue
        changed.append(name)
        if row['exists'] and not name.startswith('mods/'):
            target = args.record / 'server-before' / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(read(name, row))
    save(args.record / 'server-changes.json', changed)
    print(json.dumps({'root': state['root'], 'players': state['players'], 'mods': state['mods'],
                      'selectedFiles': len(state['files']), 'changedFiles': len(changed),
                      'paths': changed, 'record': str(args.record)}, ensure_ascii=True, indent=2))


def stage(args):
    package = json.loads(args.package.read_text(encoding='utf-8-sig'))
    directory = Path(package['stage'])
    archive = Path(package['archive'])
    manifest = json.loads((directory / 'UPDATE-MANIFEST.json').read_text(encoding='utf-8'))
    before = json.loads((directory / 'SERVER-BEFORE.json').read_text(encoding='utf-8'))
    names = [row['path'] for row in before['files']]
    now = inspect(names)
    baseline = {row['path']: row.get('sha256') for row in before['files']}
    for row in now['files']:
        if row.get('sha256') != baseline[row['path']]:
            raise ValueError('Server changed since release inspection: ' + row['path'])
    data = archive.read_bytes()
    if sha(data) != package['sha256']:
        raise ValueError('Local archive changed')
    remote = 'updates/Sunlit-Industry-' + manifest['release']
    upload(remote + '/' + archive.name, data)
    for name in ['UPDATE-MANIFEST.json', 'UPDATE-NOTES.zh-CN.md', 'SHA256SUMS.txt', 'SERVER-BEFORE.json']:
        upload(remote + '/' + name, (directory / name).read_bytes())
    changed = [row['path'] for row in manifest['files'] if row.get('beforeSHA256')]
    # Backups and the archive stay outside the active mods/config/KubeJS directories.
    backup_code = '''
var out = new com.google.gson.JsonObject();
var base = java.nio.file.Paths.get(BASE).toAbsolutePath().normalize();
var root = java.nio.file.Paths.get("").toAbsolutePath().normalize();
if (!base.startsWith(root.resolve("updates"))) throw new IllegalStateException("Unexpected staging path");
var names = new java.util.TreeSet<String>(java.util.List.of(NAMES));
try (var paths = java.nio.file.Files.list(root.resolve("mods"))) {
    paths.filter(java.nio.file.Files::isRegularFile).filter(p -> p.getFileName().toString().startsWith("sunlit-industry-compat-1.20.1-") && p.toString().endsWith(".jar")).forEach(p -> names.add(root.relativize(p).toString().replace('\\\\', '/')));
}
var backups = new com.google.gson.JsonArray();
for (String name : names) {
    var source = root.resolve(name).normalize();
    var target = base.resolve("backup").resolve(name).normalize();
    if (!source.startsWith(root) || !target.startsWith(base.resolve("backup"))) throw new IllegalStateException("Path outside release");
    if (!java.nio.file.Files.isRegularFile(source)) continue;
    byte[] original = java.nio.file.Files.readAllBytes(source);
    java.nio.file.Files.createDirectories(target.getParent());
    if (java.nio.file.Files.exists(target) && !java.util.Arrays.equals(original, java.nio.file.Files.readAllBytes(target))) throw new IllegalStateException("Existing backup differs: " + name);
    if (!java.nio.file.Files.exists(target)) java.nio.file.Files.copy(source, target);
    var row = new com.google.gson.JsonObject();
    row.addProperty("path", name);
    row.addProperty("sha256", java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(java.nio.file.Files.readAllBytes(target))));
    backups.add(row);
}
var archive = base.resolve(ARCHIVE);
byte[] bytes = java.nio.file.Files.readAllBytes(archive);
out.addProperty("archiveSHA256", java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)));
out.addProperty("archiveBytes", bytes.length);
out.add("backups", backups);
return out.toString();
'''.replace('BASE', json.dumps(remote)).replace('NAMES', ','.join(json.dumps(name) for name in changed)).replace('ARCHIVE', json.dumps(archive.name))
    verified = execute(backup_code)
    if verified['archiveSHA256'] != package['sha256'] or verified['archiveBytes'] != package['bytes']:
        raise ValueError('Uploaded archive hash differs')
    for row in verified['backups']:
        if row['path'] in baseline and row['sha256'] != baseline[row['path']]:
            raise ValueError('Backup differs from inspected server: ' + row['path'])
    after = inspect(names)
    if any(row.get('sha256') != baseline[row['path']] for row in after['files']):
        raise ValueError('Active files changed during staging')
    receipt = {'status': 'uploaded-verified-awaiting-stopped-install', 'remoteDirectory': remote,
               'archive': archive.name, 'sha256': verified['archiveSHA256'],
               'bytes': verified['archiveBytes'], 'payloadFiles': len(manifest['files']),
               'backups': verified['backups'], 'activeFilesChanged': False,
               'activated': False, 'reloaded': False, 'restarted': False,
               'players': after['players'], 'running': call('ping')['running']}
    save(args.record / 'upload-receipt.json', receipt)
    upload(remote + '/UPLOAD-RECEIPT.json', (args.record / 'upload-receipt.json').read_bytes())
    print(json.dumps(receipt, ensure_ascii=True, indent=2))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['inspect', 'stage'])
    parser.add_argument('--pack', type=Path, default=DEFAULT_PACK)
    parser.add_argument('--record', type=Path, required=True)
    parser.add_argument('--package', type=Path)
    args = parser.parse_args()
    if args.action == 'stage' and args.package is None:
        parser.error('--package is required to stage')
    capture(args) if args.action == 'inspect' else stage(args)


if __name__ == '__main__':
    main()
