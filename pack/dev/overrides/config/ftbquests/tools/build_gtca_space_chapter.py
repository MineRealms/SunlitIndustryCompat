#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Ground bootstrap and orbital operations for GTCA's elevator modules."""

from industrial_quest_support import quest as q, run

RESEARCH_STATION = "468EEFD9D2F69014"

Q = [
    q("overview", "GTCA：轨道工业总图", "gtca:space_elevator", (0, 0), [
        "GCYR 负责火箭、行星与空间站；GTCA 电梯是另一套模块供电枢纽。本章用 GCYR 轨道硬件与 GTNN 重型合金连接两条进度。",
        "模块 MKI/MKII/MKIII 的地面制造对应 LuV/ZPM/UV，但实际工作要由 UHV 制造的电梯启用。不能用提前造出一个模块来跳过电梯。",
        "研究样品、消耗材料与带研究成果的介质分别准备。任务勾选不会发放数据或让未成形的机器工作。",
    ], check="已区分 GCYR 太空探索与 GTCA 轨道工业"),
    q("research", "地面准备：装配线与研究网络", "gtceu:research_station", (3, 0), [
        "准备装配线、研究站、算力供给及空白研究介质。电梯、PIKO 晶圆和纳米管线轴各有独立研究 ID，不能共享一份无关数据。",
        "本章 128 CWU/t 的三项研究使用数据模块；以 JEI 对应研究配方的空白介质为准。装配线的数据仓读取完成后的介质。",
        "UHV 制造要求 1966080 EU/t。多方块的多能源仓规则可提高允许的配方档位，仍要满足总输入功率和结构仓数上限。",
    ], deps=["overview"], items=["gtceu:assembly_line", "gtceu:research_station"], ext=[RESEARCH_STATION]),
    q("piko", "UHV | PIKO 晶圆", "gtca:piko_wafer_ic", (6, -3), [
        "先在 UV COMET 得到量子异常体，再研究 gtca_space_piko_wafer：128 CWU/t、512000 总 CWU、UV 研究功率。",
        "装配线使用 SoC、量子之星、Neutronex 板、UHV 电路与 UV 场发生器，配 UU 物质和中子素；UHV 功率、6400 tick，产出 64 片。",
    ], deps=["research"], items=["gtca:piko_wafer_ic"]),
    q("nanotube", "UHV | 纳米管线轴", "gtca:nanotube_spool", (6, 3), [
        "首批碳纳米机器人先走地面装配线。取样研究 gtca_space_nanotube_spool：128 CWU/t、512000 总 CWU、UV 研究功率。",
        "16 个碳纳米机器人、8 个 UV 机械臂、2 颗量子之星、8 块中子素板和 UHV 电路，配 UU 物质与中子素；UHV 功率、4800 tick，产出一个线轴。",
        "当前 GTCEu 不生成中子素环，路线已经使用存在的中子素板，避免空原料。",
    ], deps=["research"], items=["gtca:nanotube_spool"]),
    q("controller_research", "电梯研究：线轴样品与独立数据", "gtceu:data_module", (9, 0), [
        "研究纳米管线轴得到 society_gtca_space_elevator_controller_v2。要求 UHV 研究功率、128 CWU/t、512000 总 CWU。",
        "线轴研究与控制器研究不是同一份成果；JEI 中核对研究 ID、输出数据模块与装配线要求。这里的勾选用于记录人工核对。",
    ], deps=["piko", "nanotube"], check="已完成匹配电梯控制器的研究数据"),
    q("orbital_hardware", "跨模组投入：轨道硬件与 T4 重型合金", "gcyr:space_station_packager", (12, 0), [
        "电梯控制器额外投入 4 件 GCYR 太空织物、4 块 GTNN T4 重型合金板与 1 台 GCYR 空间站打包机。打包机是机器本体，不是廉价的空纸包。",
        "T4 重型合金在 LuV 组装，沿 T1→T4 逐级升级；缺少致密板的材料按 9 块普通板等价投入。火箭部件继续使用自己的 T1/T2/T3 板材门槛。",
    ], deps=["controller_research"], items=["gcyr:space_station_packager", "gcyr:space_fabric", "gtnn:heavy_plate_t4"]),
    q("elevator", "UHV | 太空电梯控制器", "gtca:space_elevator", (15, 0), [
        "在装配线制造控制器：1966080 EU/t、7200 tick。保留原有中子素结构、PIKO 晶圆、UV 元件和四种流体，再投入跨模组轨道硬件。",
        "原生控制器配方及对应旧研究已成对替换；本章不提供低成本平行配方。研究数据、物品与流体顺序按 JEI 布置。",
    ], deps=["orbital_hardware"], items=["gtca:space_elevator"]),
    q("structure", "地面建设：结构件与首级电机", "gtca:space_elevator_base_casing", (18, 0), [
        "电梯基座外壳、内部结构、支撑结构、缆索和 MK1 电机都有地面制造入口，不需要先运行太空组装机。按多方块预览准备完整数量。",
        "模块本身也需要专用外壳和输入输出部件；控制器持有任务并不等于结构验收。电梯和模块所在区块都要加载。",
    ], deps=["elevator"], items=["gtca:space_elevator_base_casing", "gtca:space_elevator_cable",
        "gtca:space_elevator_internal_structure", "gtca:space_elevator_support_structure", "gtca:space_elevator_motor_mk_1"]),
    q("commission", "电梯验收：供电、朝向与模块槽", "gtca:space_elevator", (21, 0), [
        "先让电梯独立成形并建立稳定输入。模块安装在控制器定义的固定槽位，位置随电梯朝向旋转；不能随意放在附近就期待无线供电。",
        "MK1 电机启用 6 个槽位；当前布局定义 12 个物理模块位置。更高电机的名义额度不代表可以在任意位置安装更多模块。",
        "核对模块自己的结构状态、电梯链接状态与无线能量缓冲。电梯断开或结构失效时，模块应停止工作。",
    ], deps=["structure"], check="已确认电梯成形、供电和模块槽位"),
    q("miner_i", "采矿模块 MKI", "gtca:space_miner_mki", (24, -6), [
        "MKI 可在地面装配线制造并研究，不要求先拥有太空组装机。安装后由电梯启用；按目标准备对应档位的 GTCA 无人机作为非消耗介质。",
        "矿机另外消耗钻头、长杆和等离子体，还需要算力。GCYR 卫星不能直接替代无人机，无人机也不能代替会消耗的钻具。",
    ], deps=["commission"], items=["gtca:space_miner_mki"]),
    q("mining_supplies", "采矿补给：钻头、长杆与等离子体", "gtceu:steel_drill_head", (27, -9), [
        "以铁目标做首次开采：电路 3、LV 无人机、钢钻头与钢长杆，按 JEI 准备等离子体和算力。高阶无人机会改变所需钻具材料。",
        "最低等离子体是 GT 氦等离子体，先建立 GT 核聚变供给；COMET 的氢等离子体不能替代它。无人机从 LV 到 UV 逐级地面组装，更高型号走装配线研究；工作中不消耗无人机。",
    ], deps=["miner_i"], items=["gtca:lv_drone"], check="已备齐一个可运行目标的全部补给"),
    q("mining_run", "采矿验收：拿到第一批产物", "gtca:space_miner_mki", (30, -6), [
        "启动一个符合 MKI 等级与电压的目标，核对补给扣除、算力供给和实际产物。预留多个输出槽，矿物种类可能不止一种。",
        "不存在一个氢离子换 1024 个金纳米机器人的测试捷径。输出与消耗按正式目标配方计算。",
    ], deps=["mining_supplies"], check="已由 MKI 采矿模块完成一次正式采矿"),
    q("miner_ii", "选修 | 采矿模块 MKII", "gtca:space_miner_mkii", (33, -6), [
        "地面装配线以 ZPM 档制造，消耗 MKI 并研究前级模块。升级提高可用目标与处理能力，不移除钻头、长杆、等离子体和算力要求。",
    ], deps=["mining_run"], items=["gtca:space_miner_mkii"]),
    q("miner_iii", "选修 | 采矿模块 MKIII", "gtca:space_miner_mkiii", (36, -6), [
        "地面装配线以 UV 档制造，消耗 MKII，研究前级模块。先确认高阶目标所需补给和输出容量，再连续运行。",
    ], deps=["miner_ii"], items=["gtca:space_miner_mkiii"]),
    q("pump_i", "抽取模块 MKI", "gtca:space_pump_mki", (24, 0), [
        "地面制造并接入电梯。通过集成电路选择抽取流体；本模块使用电梯无线能量，不是把能源仓随意接在模块旁边。",
        "单并行基础功率 32768 EU/t，基础周期 9600 tick。必须为整批流体预留容量，不能只接一个很小的输出罐。",
    ], deps=["commission"], items=["gtca:space_pump_mki"]),
    q("pump_run", "抽取验收：先从石油开始", "gtca:space_pump_mki", (27, 0), [
        "电路 1 抽取石油，基础批次 1400000 mB。MKI 最多 4 并行，实际并行还受输出空间和能量供给限制。",
        "先完成一批并确认输出提取，再切换汞、气体或熔融金属。该路线为 UHV 电梯后的工业资源供给，不替代早期化工入门。",
    ], deps=["pump_i"], check="已完成一批石油抽取并导出"),
    q("pump_ii", "选修 | 抽取模块 MKII", "gtca:space_pump_mkii", (30, 0), [
        "ZPM 地面升级消耗 MKI，研究前级模块。最大并行提高到 16，流体仓容和电梯供电应同步升级。",
    ], deps=["pump_run"], items=["gtca:space_pump_mkii"]),
    q("pump_iii", "选修 | 抽取模块 MKIII", "gtca:space_pump_mkiii", (33, 0), [
        "UV 地面升级消耗 MKII，研究前级模块。最大并行 256，对应最高基础功率 8388608 EU/t；满并行只是上限，不是免费产量。",
    ], deps=["pump_ii"], items=["gtca:space_pump_mkiii"]),
    q("assembler_i", "组装模块 MKI", "gtca:space_assembler_mki", (24, 6), [
        "第一台模块在地面装配线制造。它接入电梯之后提供轨道批量组装路线，不承担自己的首台制造前置。",
        "配方带 required_module_mk 条件，低级模块不能运行高级配方。原料与输出依照正式太空组装配方，不依赖未注册的测试内容。",
    ], deps=["commission"], items=["gtca:space_assembler_mki"]),
    q("assembler_run", "轨道量产：结构件与补给", "gtca:space_elevator_base_casing", (27, 6), [
        "在 MKI 先验证钢钻头或钢长杆的批量配方，再试内部结构件：4 个框架、16 根硅岩合金长杆、8 块中子素板和 576 mB PBI，产出 4 个内部结构。",
        "轨道结构件与地面路线材料等价，按模块等级分批量；它们保留材料成本，以电梯投入换取较低加工能耗。MKII 有基座批量路线，MKIII 有缆索批量路线。",
    ], deps=["assembler_i"], check="已在太空组装模块完成一条批量配方"),
    q("assembler_ii", "选修 | 组装模块 MKII", "gtca:space_assembler_mkii", (30, 6), [
        "ZPM 地面升级消耗 MKI 并研究前级模块。新增配方使用等级 2 条件，保留低阶配方的兼容性。",
    ], deps=["assembler_run"], items=["gtca:space_assembler_mkii"]),
    q("assembler_iii", "选修 | 组装模块 MKIII", "gtca:space_assembler_mkiii", (33, 6), [
        "UV 地面升级消耗 MKII 并研究前级模块。等级 3 配方仍受材料、无线供电、输出容量与电梯状态约束。",
    ], deps=["assembler_ii"], items=["gtca:space_assembler_mkiii"]),
    q("motors", "选修 | 电机升级与槽位规划", "gtca:space_elevator_motor_mk_2", (24, 12), [
        "电机从 MK1 逐级升级至 MK5，均由地面装配线制造与研究。所有电机位置要使用同档部件，混装按最低档计算。",
        "MK2 已可覆盖当前定义的 12 个物理模块槽。更高档电机的名义额度不能创造额外安装位置，不要为不存在的第 13 个槽反复改结构。",
    ], deps=["commission"], items=["gtca:space_elevator_motor_mk_2"]),
    q("recovery", "运行验收：断电、重连与库存", "gtca:space_elevator", (24, 18), [
        "先暂停一台模块，再验证电梯断电、结构失效和重连后的行为。保留足够的无线能量缓冲，避免把正常的缓冲耗尽误判成配方注册失败。",
        "模块未形成、槽位未启用、目标等级不足、算力不足与输出堵塞是不同问题。按界面逐项定位，不要通过重复重载配方尝试修复结构。",
    ], deps=["commission"], check="已完成暂停、重连与库存检查"),
]

# Accepted compact layout; the graph and stable IDs remain independent of geometry.
LAYOUT_Y = {
    "piko": -1.5, "nanotube": 2.5, "miner_i": -4.5,
    "mining_supplies": -5.0, "mining_run": -3.5, "miner_ii": -4.0,
    "miner_iii": -3.5, "pump_i": -1.5, "pump_ii": -0.5,
    "pump_iii": -1.0, "assembler_i": 1.5, "assembler_run": 3.5,
    "assembler_ii": 4.5, "assembler_iii": 3.5, "motors": 5.0,
    "recovery": 10.0,
}
for entry in Q:
    entry["xy"] = (entry["xy"][0], LAYOUT_Y.get(entry["key"], entry["xy"][1]))

SPEC = dict(filename="gtca_space", title="GTCA：轨道工业", icon="gtca:space_elevator",
            order=26, salt="sunlit:gtca_space:20261112", quests=Q)

if __name__ == "__main__":
    run(SPEC)
