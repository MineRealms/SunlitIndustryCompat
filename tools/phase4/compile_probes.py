"""Compile SRG probe snippets without starting another Minecraft server."""
from pathlib import Path
import json
import sys
sys.stdout.reconfigure(encoding="utf-8")
import subprocess
import zipfile

RUN=Path('G:/emoney-full-phase4-20261104')
ROOT=Path(__file__).resolve().parent
OUT=Path('H:/MinecraftMods/SunlitIndustryCompat/.local/phase4-probe-compile')
OUT.mkdir(parents=True,exist_ok=True)
DEPS=RUN/'libraries/phase4-debug'
DEPS.mkdir(exist_ok=True)
for jar in (RUN/'mods').glob('*.jar'):
    with zipfile.ZipFile(jar) as z:
        for n in z.namelist():
            if n.startswith('META-INF/jarjar/') and n.endswith('.jar'):
                p=DEPS/Path(n).name
                if not p.exists(): p.write_bytes(z.read(n))
# Files here are only compiler dependencies, not mods/. Existing jars have SRG names.
cpdir=OUT/'classpath'
cpdir.mkdir(exist_ok=True)
import os
jars=list((RUN/'libraries/net/minecraft/server').rglob('*-srg.jar'))
jars+=list((RUN/'libraries/net/minecraftforge/forge').rglob('*universal.jar'))
jars+=list((RUN/'mods').glob('*.jar'))
jars+=[p for p in (RUN/'libraries').rglob('*.jar') if '/net/minecraft/' not in p.as_posix() and '/net/minecraftforge/forge/' not in p.as_posix()]
cp=[]
for i,p in enumerate(jars):
    if p.stat().st_size==0: continue
    target=cpdir/(str(i)+'.jar')
    if not target.exists():
        import shutil
        shutil.copyfile(p,target)
    cp.append(target)
results={}
for snippet in ROOT.glob('*.snippet'):
    name='Probe_'+snippet.stem
    src=OUT/(name+'.java')
    src.write_text('public class '+name+' { public static String run() throws Exception {\n'+snippet.read_text(encoding='utf-8')+'\n}}\n',encoding='utf-8')
    args=OUT/'args.txt'
    args.write_text('-proc:none\n-encoding\nUTF-8\n--release\n17\n-classpath\n"'+ ';'.join(p.as_posix() for p in cp)+'"\n-d\n"'+OUT.as_posix()+'"\n"'+src.as_posix()+'"\n',encoding='utf-8')
    r=subprocess.run(['C:/Program Files/Java/jdk-21/bin/javac.exe','@'+str(args)],capture_output=True)
    text=(r.stdout+r.stderr).decode('gb18030',errors='replace')
    (OUT/(snippet.stem+'.log')).write_text(text,encoding='utf-8')
    results[snippet.name]=r.returncode
    print(snippet.name,'PASS' if r.returncode==0 else text[:4500],flush=True)
(OUT/'results.json').write_text(json.dumps(results,indent=2))
raise SystemExit(1 if any(results.values()) else 0)
