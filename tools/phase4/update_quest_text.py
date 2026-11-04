"""Update existing quest descriptions only; stable keys and quest IDs are untouched."""
from pathlib import Path
import ast

P = Path('G:/MinecraftGames/Sunlit Valley(BaopuEdition)/.minecraft/versions/Society Sunlit Valley/config/ftbquests/tools/build_thaumic_industry_chapter.py')
s = P.read_text(encoding='utf-8')
s = s.replace('  35F501BCCBED6EC4  thaumcraft', '  4F15F9F6021B0AF8  thaumcraft')
s = s.replace('  pollution:assembler/thaumic_circuit/0..6      200t x5, 800t x2',
              '  Phase 4: circuit_assembler/thaumic_circuit/0..5; assembly_line/thaumic_circuit/6')
s = s.replace('  pollution:industrial_infusion_recipes/industrial_infusion/thaumic/...  (19 total)',
              '  pollution:industrial_infusion_recipes/industrial_infusion/thaumic/... (count depends on loaded mods)')
s = s.replace('奥术工作台（附魔台旁的书架阵）', 'TC 奥术工作台')
s = s.replace('基础血电路', '奥术电路基底').replace('进阶血电路', '奥术电路进阶基底').replace('终极血电路', '奥术电路终极基底').replace('至尊血电路', '奥术电路至尊基底')
s = s.replace('pollution:assembler/thaumic_circuit/', 'pollution:circuit_assembler/thaumic_circuit/')
s = s.replace('退役的 7 个 ID', '退役的 6 个 ID')
changes = {
 'template_3': {'title':'模板 3：秘银锂 + 影金', 'desc':[
  '注魔祭坛：中心物 &6奥术电路进阶基底&r，组件 &7thaumadds:mithrillium_ingot 1 + 影金锭 1 + 以太碎片 2&r。',
  '要素：&6源质 24 / 异界 16 / 金属 12&r；不稳定度 5。',
  '终极基底用于 LuV／ZPM；以太碎片要放两个独立的基座，不是一个材料的两种替代品。']},
 'template_4': {'title':'模板 4：灵宝 + 创造碎片', 'desc':[
  '中心物 &6奥术电路终极基底&r；组件 &7灵宝（thaumictinkerer:ichorium）2 + 创造碎片 1 + 下界之星 1&r。',
  '要素：源质 32 / 异界 24 / 灵气 24；不稳定度 8。创造碎片已经消耗原始珍珠，这一步不再重复加一颗。',
  '至尊基底用于 UV；后期先完成原始珍珠研究，再用工业注魔合成珍珠供整条材料线使用。']},
 'circuit_iv': {'title':'神秘电路 IV：秘银锂', 'desc':[
  '电路组装机：模板 2 + thaumadds:mithrillium_ingot + 多层纤维增强印刷电路板 + 8 细铌钛线 + 72mB 焊锡 → 2 个；9600 EU/t、200t、洁净室。',
  '配方 id：pollution:circuit_assembler/thaumic_circuit/3。',
  '神秘领域的实际顺序是 mithrillium → adaminite → mithminite，不再按旧译名把顺序排反。']},
 'circuit_luv': {'title':'神秘电路 LuV：灵宝与神经处理单元', 'desc':[
  '电路组装机：模板 3 + 灵宝（thaumictinkerer:ichorium）+ 神经处理单元 + 8 细钇钡铜氧线 + 72mB 焊锡 → 2 个；38400 EU/t、200t、洁净室。',
  '配方 id：pollution:circuit_assembler/thaumic_circuit/4。',
  '保留神经处理单元，因此生物培养与无菌洁净室前置不可绕过；只替换 GT 的芯片／贴片元件部分。']},
 'circuit_zpm': {'title':'神秘电路 ZPM：LuV 电路组件升级', 'desc':[
  '电路组装机：模板 3 + adaminite + 湿件印刷电路板 + 2 个神秘 LuV 电路 + 16 细钇钡铜氧线 + 144mB 焊锡 → 2 个；38400 EU/t、400t、洁净室。',
  '配方 id：pollution:circuit_assembler/thaumic_circuit/5。',
  '对标 GT 湿件处理器组件：低一级电路和基板必须投入。adaminite 消耗的珍珠可在工业注魔中晚期合成，不必每次刷 Boss。']},
 'circuit_uv': {'title':'神秘电路 UV：研究站与装配线', 'desc':[
  '装配线：湿件印刷电路板 + 2 个神秘 ZPM 电路 + 24 细钇钡铜氧线 + 32 PBI 箔 + 4 铕板 + 模板 4 + mithminite + 1152mB 焊锡 → 1 个；38400 EU/t、400t。',
  '配方 id：pollution:assembly_line/thaumic_circuit/6。',
  '必须在研究站研究神秘 ZPM 电路（16 CWU/t），并把数据带入装配线；不再允许普通组装机越过研究制造 UV 电路。']},
 'industrial_intro': {'desc':[
  '工业注魔多方块的控制器需要高级材料，前期仍用坩埚、奥术工作台和注魔祭坛。后期通过并行仓与超频扩大产能。',
  '只转精选白名单：神秘锭、虚空锭、平衡碎片、神秘领域金属、影金、创造碎片、以太碎片、灵液和灵宝等；四个电路模板也有工业孪生。',
  '源质每点 144mB，奥术 vis 每点 36mB。转换保留全部材料数量、中心物和研究条件，不能跳过不认识的材料降低成本。',
  '通常为 HV 配方；原始珍珠工业合成单独需要 LuV 电压与 PRIMPEARL 研究，详见下一任务。',
  '原始手工路线全部保留；研究与工业设备是解锁门槛，并行和超频负责产量。']},
 'industrial_batch': {'title':'工业量产：四模板与原始珍珠', 'desc':[
  '模板 1～4 都可工业生产：模板 1 仍消耗 2 神秘锭、水银、LV 电路和对应 vis 流体；模板 2～4 必须消耗上一块模板。',
  '原始珍珠工业替代：1 下界之星 + 4 平衡碎片 + 2 以太碎片；源质／灵气／异界／虚空各 32 点（各 4608mB）；LuV 电压、1200t，机主须完成 PRIMPEARL。',
  '高级神秘金属和创造碎片继续消耗珍珠，但供应改由工业线承担。配方仍耗材料与能量，不是免费复制或可回收催化。',
  '转换配方数受已安装模组影响，以启动日志的 registered 数量和 JEI 为准，不把旧测试环境的 19 条当固定值。']}
}
# Replace only keyword values inside Q's dict calls, preserving business keys and item/check tasks.
tree = ast.parse(s)
q = next(n for n in tree.body if isinstance(n, ast.Assign) and any(isinstance(t,ast.Name) and t.id=='Q' for t in n.targets))
lines=s.splitlines(keepends=True); offsets=[0]
for line in lines: offsets.append(offsets[-1]+len(line.encode('utf-8')))
b=s.encode('utf-8'); edits=[]
for call in q.value.elts:
 kw={k.arg:k.value for k in call.keywords}; key=ast.literal_eval(kw['key'])
 for field,value in changes.get(key,{}).items():
  n=kw[field]; start=offsets[n.lineno-1]+n.col_offset; end=offsets[n.end_lineno-1]+n.end_col_offset
  text=repr(value) if not isinstance(value,list) else '['+',\n               '.join(repr(x) for x in value)+']'
  edits.append((start,end,text.encode('utf-8')))
for a,z,v in sorted(edits,reverse=True): b=b[:a]+v+b[z:]
P.write_bytes(b)
print('Updated',len(edits),'fields; quest keys and task items unchanged')
