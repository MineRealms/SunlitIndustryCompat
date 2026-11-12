#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""GTCA industrial routes for the Sunlit Valley GTCEu 7.5.3 pack."""

from industrial_quest_support import quest as q, run

ASSEMBLY_LINE = "77B7B1EDD19831E0"
RESEARCH_STATION = "468EEFD9D2F69014"

Q = [
    q("overview", "GTCA：工业材料与精密制造", "gtca:advanced_ebf", (0, 0), [
        "本章分为细磨浮选、粒子合成、纳米与电路板三条支线。GTNN 化工厂提供浮选药剂；轨道模块和电梯另见《GTCA：轨道工业》。",
        "所有物品目标仅检查持有，不扣除材料。手动验收不会给予研究数据，也不会改变机器的供电、结构或配方条件。",
        "电路使用同档标签。机器控制器能造出来，不代表所有高阶配方都已能运行；优先核对研究、线圈、实际电压与输出空间。",
    ], check="已选定首条 GTCA 产线"),
    q("alloy_stock", "合金库存：从通用金属到专用外壳", "gtceu:alloy_blast_smelter", (3, -6), [
        "为 IsaMill 和浮选单元分别备料：Inconel 625、Zeron 182、Stellite 100、Hastelloy N 不是可互换的钢材。按控制器和外壳的 JEI 分开下单。",
        "GTCA 的 Hastelloy X/C-276 外壳已修正为空材料兼容问题；不要使用缺失材料的旧配方缓存。",
    ], deps=["overview"], items=["gtceu:alloy_blast_smelter"]),
    q("aebf", "LuV | Volcanus 高级电炉", "gtca:advanced_ebf", (6, -6), [
        "控制器由原版电炉、LuV 场发生器和 LuV 电路制造。结构需要专用外壳、线圈、通风和消声部件，按多方块预览搭建。",
        "它处理电炉配方；细磨与浮选则是另一组机器。先验证常用合金的热锭冷却和流体输出，再扩大批量。",
    ], deps=["alloy_stock"], items=["gtca:advanced_ebf"], ext=[ASSEMBLY_LINE]),
    q("isamill", "LuV | IsaMill 细磨机", "gtca:isamill", (9, -6), [
        "控制器走装配线：30720 EU/t、12000 tick。先在扫描仪研究 LuV 粉碎机，保留与该配方对应的研究数据。",
        "细磨输入可以是原矿或粉碎矿，两者产率不同；不要先把所有原矿粉碎后才比较路线。",
    ], deps=["aebf"], items=["gtca:isamill"]),
    q("grinding_balls", "研磨介质：压坯与聚爆", "gtca:grinding_ball_tungsten_carbide", (12, -6), [
        "16 个碳化钨块在 EV 压缩机压成生坯，再经聚爆制成碳化钨研磨球。富集硅岩球使用独立的 IV 压坯配方。",
        "两种球是非消耗介质。IsaMill 集成电路 11 对应碳化钨路线，10 对应富集硅岩路线；请使用成品球而非生坯。",
    ], deps=["isamill"], items=["gtca:grinding_ball_tungsten_carbide"]),
    q("milled_feed", "首批细磨矿：镍与独居石", "gtca:milled_nickel", (15, -6), [
        "先用镍矿验证细磨机，再处理独居石。不同矿物的配方电压不同，独居石细磨需要 ZPM，不能因控制器在 LuV 制作就认为全部可做。",
        "细磨产物要留给浮选。JEI 中原矿与粉碎矿、两种研磨球的四条路线分别比较耗电、耗时和产量。",
    ], deps=["grinding_balls"], items=["gtca:milled_nickel"]),
    q("flotation", "LuV | 浮选单元", "gtca:flotation_cell_regulator", (18, -6), [
        "控制器走 30720 EU/t 装配线，研究样品为原版蒸馏塔。结构使用两种浮选外壳，不能全部替换成同一种。",
        "浮选需要细磨矿、云杉油和相应黄原酸盐。GTNN 已安装时，GTCA 会把部分药剂制造接入化工厂与脱水机；机壳等级和催化剂也是条件。",
    ], deps=["milled_feed"], items=["gtca:flotation_cell_regulator"]),
    q("froth_run", "闭环验收：药剂 → 浮选 → 焙烧", "gtca:milled_platinum", (21, -6), [
        "先完成一批镍浮选：按 JEI 准备黄原酸乙酯钾与云杉油，输出镍泡沫，再在电炉完成对应泡沫处理。预留多个固体和流体输出。",
        "浮选是矿物回收支线，不会替你完成 GTNN 困难铂线的全部化学分离。铂族残渣仍要按《GTNN》章节继续处理。",
    ], deps=["flotation"], check="已让一批矿物从细磨走到最终回收产物"),
    q("comet", "IV | COMET 回旋加速器", "gtca:comet", (3, 0), [
        "控制器在 IV 组装机制造：7680 EU/t、6000 tick。粒子配方实际从 LuV 起，量子异常体更需要 UV；控制器造价不是运行电压。",
        "先验证氢气供给、多个物品输出与等离子体流体输出。概率产物应使用缓冲库存，不能假定每批都有相同结果。",
    ], deps=["overview"], items=["gtca:comet"]),
    q("hydrogen_ion", "LuV | 氢离子", "gtca:hydrogen_ion", (6, 0), [
        "COMET 电路 24、1000 mB 氢气、30720 EU/t、400 tick。输出由多个独立概率槽构成，空产出批次是正常情况。",
        "氢离子既用于粒子研究，也用于后续受控的等离子体制造；为两条用途分别保留库存。",
    ], deps=["comet"], items=["gtca:hydrogen_ion"]),
    q("particles", "粒子分流：未知粒子与基础粒子", "gtca:unknown_particle", (9, 0), [
        "电路 21：氢离子与氢气在 LuV COMET 中生成多种概率粒子；电路 22 是另一条带氢等离子体输出的路线。质子、电子、中子与未知粒子分别存储。",
        "未知粒子用于 UV 量子异常体；基础粒子用于 Tegmark Forge。不要把一条用途的所有库存都拿去做另一条。",
    ], deps=["hydrogen_ion"], items=["gtca:unknown_particle", "gtca:proton", "gtca:electron", "gtca:neutron"]),
    q("tegmark", "UHV 制造 | Tegmark Forge", "gtca:tegmark_forge", (12, 0), [
        "控制器在装配线以 UHV 功率制造，尽管部分粒子合成配方只要求 ZPM。需要 Neutronex、Duranium X 与其他专用结构材料。",
        "本包默认不展开全部高电压单方块机器。多方块可用多能源仓提升可接受配方档位，但总功率、仓数上限和对应研究仍必须满足。",
    ], deps=["particles"], items=["gtca:tegmark_forge"]),
    q("particle_metals", "粒子冶金：Ohriharukon 与旋律合金", "gtca:tegmark_forge", (15, 0), [
        "在 Tegmark Forge 中用钯和相应数量的质子、电子、中子制造 Ohriharukon；先完成钯样品研究。其他金属有自己的研究样品。",
        "Ohriharukon 是旋律合金的前置。因此旋律合金的搅拌机配方虽然只写 EV，整条路线实际受到 UHV 控制器制造的门槛约束。",
    ], deps=["tegmark"], check="已完成一批 Ohriharukon，并核对旋律合金材料来源"),
    q("stellar", "恒星合金：为 PCB 工厂备料", "gtca:pcb_factory_mki", (18, 0), [
        "旋律合金由末地钢、末影之眼粉和 Ohriharukon 制造；再与下界之星粉、硅岩合成恒星合金。加工成 PCB MKI 所需框架。",
        "电压需求和材料来源分别核对。这里记录完整材料链，不把恒星合金的 IV 搅拌机功率当成 IV 解锁承诺。",
    ], deps=["particle_metals"], check="已备齐 PCB MKI 的恒星合金框架"),
    q("research", "研究基础：扫描仪、研究站与算力", "gtceu:research_station", (3, 6), [
        "扫描研究与研究站研究使用不同设备。研究站需要稳定的算力供给、足够供电、空白介质和正确样品。",
        "空白数据球或数据模块本身不算研究成果；装配线检查的是带有匹配研究 ID 的数据。不同研究不可因样品相同就混用。",
    ], deps=["overview"], items=["gtceu:research_station", "gtceu:high_performance_computation_array"], ext=[RESEARCH_STATION]),
    q("carbon_bootstrap", "UV 装配 / UHV 研究 | 首批碳纳米机器人", "gtca:carbon_nanites", (6, 6), [
        "先用地面装配线制造首批碳纳米机器人：原生路线产出 2 个，使用 UV 元件、干细胞和碳粉等材料。它不需要先建 Nano Forge。",
        "该路线研究碳粉，原生研究要求 UHV 功率和 128 CWU/t。纳米机器人是电梯纳米管与 Nano Forge 的共同前置，应分别备料。",
    ], deps=["research"], items=["gtca:carbon_nanites"]),
    q("nanoforge", "UV 制造 | Nano Forge", "gtca:nano_forge", (9, 6), [
        "UV 装配线控制器吃 16 个碳纳米机器人；研究样品也是碳纳米机器人。制造所需材料与研究样品分别准备。",
        "原生批量纳米配方的功率可达到 UEV；本章只要求首台控制器与研究条件，不要求在 UV 阶段完成全部金银纳米量产。",
    ], deps=["carbon_bootstrap"], items=["gtca:nano_forge"]),
    q("nano_limit", "产线选择：首批与量产分开", "gtca:carbon_nanites", (12, 6), [
        "首批碳纳米机器人走地面装配线，Nano Forge 则是后续批量路线。先确认配方功率、晶体透镜和 UU 物质消耗，别把整条进度押在当前供电无法运行的配方上。",
        "空间采矿不再保留「一个氢离子产 1024 个金纳米机器人」的测试配方。金、银纳米机器人应来自各自生产链。",
    ], deps=["nanoforge"], check="已区分首批路线和高功率量产路线"),
    q("anomaly", "UV | 量子异常体", "gtca:quantum_anomaly", (12, 3), [
        "COMET 电路 24：未知粒子 1 个、熔融 Duranium 40 mB，491520 EU/t、500 tick，基础概率 1%。这是概率研究样品，不是每批必出。",
        "轨道章节中的 PIKO 晶圆与 PCB MKIII 都需要相关研究。先得到一个样品并安排研究，再决定是否扩大库存。",
    ], deps=["particles"], items=["gtca:quantum_anomaly"]),
    q("pcb_mki", "PCB MKI：先接入材料与化工", "gtca:pcb_factory_mki", (21, 0), [
        "控制器在 UV 装配线制造，研究湿件电路板，98 CWU/t、ZPM 研究功率。恒星合金框架与全部结构件也要可达。",
        "PCB 工厂输出电路板基材，不会直接替代整条处理器或主机装配。原 GT 电路任务继续作为主线。",
    ], deps=["stellar"], items=["gtca:pcb_factory_mki"]),
    q("pcb_run", "PCB 首批：酸液、箔与板材", "gtceu:plastic_circuit_board", (24, 0), [
        "先用 MKI 的电路配置 1 运行一批塑料电路板。输入板材、铜/退火铜箔、硫酸与三氯化铁，按 JEI 对照当前机器等级。",
        "MKII 配方另需银纳米机器人，MKIII 配方另需金纳米机器人；升级控制器不等于获得这些介质。",
    ], deps=["pcb_mki"], items=["gtceu:plastic_circuit_board"], check="已在 PCB 工厂完成首批电路板"),
    q("pcb_upgrades", "选修 | PCB MKII / MKIII", "gtca:pcb_factory_mkii", (27, 0), [
        "MKII 研究 MKI 控制器；MKIII 研究量子异常体。不同级别具有独立机壳和配方条件，按多方块预览升级。",
        "控制器制造电压、机器等级和金银纳米机器人的生产电压分别计算。此分支为后续扩展，不阻塞轨道模块的首批制造。",
    ], deps=["pcb_run"], check="已核对升级所需研究、介质与实际供电"),
]

# Accepted compact layout; keep the authored coordinates above as the seed.
LAYOUT_Y = {
    "alloy_stock": -4.0, "aebf": -4.5, "isamill": -4.5,
    "grinding_balls": -4.5, "milled_feed": -4.5, "flotation": -4.5,
    "froth_run": -4.5, "particles": 0.5, "tegmark": -1.0,
    "particle_metals": -0.5, "stellar": 0.5, "research": 4.5,
    "carbon_bootstrap": 5.0, "nanoforge": 5.0, "nano_limit": 5.0,
    "anomaly": 2.5, "pcb_upgrades": 0.5,
}
for entry in Q:
    entry["xy"] = (entry["xy"][0], LAYOUT_Y.get(entry["key"], entry["xy"][1]))

SPEC = dict(filename="gtca_industry", title="GTCA：工业材料", icon="gtca:advanced_ebf",
            order=25, salt="sunlit:gtca_industry:20261112", quests=Q)

if __name__ == "__main__":
    run(SPEC)
