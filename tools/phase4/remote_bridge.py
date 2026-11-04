
"""通过 frp 映射连接远端 Rosetta 桥。用法:
  python remote_bridge.py ping
  python remote_bridge.py exec <snippet>
  python remote_bridge.py console <cmd...>
环境变量: ROSETTA_HOST ROSETTA_PORT ROSETTA_TOKEN
"""
import json, os, socket, sys
from pathlib import Path

HOST = os.environ.get("ROSETTA_HOST", "frp-top.com")
PORT = int(os.environ.get("ROSETTA_PORT", "17799"))
TOKEN = os.environ.get("ROSETTA_TOKEN", "")

if not TOKEN:
    cand = Path(r"H:/MinecraftMods/tmp/rosetta_token.txt")
    if cand.is_file():
        TOKEN = cand.read_text(encoding="utf-8").strip()
if not TOKEN:
    raise SystemExit("no token")

cmd = sys.argv[1] if len(sys.argv) > 1 else "ping"
req = {"token": TOKEN, "cmd": cmd}
if cmd == "exec":
    req["args"] = {"code": Path(sys.argv[2]).read_text(encoding="utf-8")}
elif cmd == "console":
    req["args"] = {"command": " ".join(sys.argv[2:])}

with socket.create_connection((HOST, PORT), timeout=60) as s:
    s.settimeout(240)
    f = s.makefile("rwb")
    f.write((json.dumps(req) + "\n").encode())
    f.flush()
    line = f.readline()
sys.stdout.reconfigure(encoding="utf-8")
print(json.dumps(json.loads(line), ensure_ascii=False))
