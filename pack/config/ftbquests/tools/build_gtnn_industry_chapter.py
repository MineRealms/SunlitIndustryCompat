"""Build the compact GTNN chapter from the pack's enabled process chains.

Recipe evidence: GTNN PlatinumLine.kt, NaquadahLine.kt, RocketFuel.kt,
SelfRecipes.kt, CircuitAssemblyLineMachineRecipeHandler.kt, and the pack's
gcyr/{heavyAlloys,hardenRockets,gtnnLinks}.js. Material entries use gtceu:.
"""

from industrial_quest_support import quest as q, run


Q = [
    q("process_plan", "GTNN：工艺与量产", "gtnn:exxonmobil_chemical_plant", (0, 0), [
        "本包启用 GTNN 困难铂线与困难硅岩线。铂族金属先经历分离、沉淀和还原；富集硅岩与超能硅岩还需要中子活化及多段溶液处理。",
        "化工设备从 MV 起步，火箭材料从 HV 开始，精密组装与电路装配线进入 LuV。各支线按实际制造条件推进，配方电压与整条产线最早可建成的阶段分别说明。",
        "先规划酸碱、气体与副产物回流，再扩充并行。详细电压与物料用量以当前 JEI 配方为准；本章中的取得任务不消耗物品。"
    ], check="规划化工、铂族、硅岩、火箭与电路产线", ext=("1BE0349B8F3890D3",)),

    q("chemical_plant", "MV：化工厂控制器", "gtnn:exxonmobil_chemical_plant", (3, -12), [
        "化工厂控制器在 MV 组装机制造，基础配方 120 EU/t、120 秒，集成电路设为 19。准备 MV 机器外壳、退火铜板、聚乙烯大管道、铝齿轮、黑物质框架与熔融黑钢。",
        "按 JEI 的 7×7×7 预览建造厂体。工厂外壳、机器方块、管道方块和线圈分别承担结构等级、输入电压上限、并行与速度的作用，不能只升级能源仓。"
    ], deps=("process_plan",), items=("gtnn:exxonmobil_chemical_plant",)),
    q("dehydration", "脱水与溶液分离", "gtnn:mv_dehydrator", (6, -12), [
        "单方块脱水机提供 MV 至 ZPM 机型。第一台 MV 脱水机使用中压外壳、中压电路、铜线缆、钢齿轮和机械臂，适合从小规模溶液处理开始。",
        "后续困难硅岩线要分别处理残渣溶液和超能硅岩废液，所需配方可到 EV；升级机型前先核对目标配方的电压与输出槽。大型脱水机则是 LuV 组装的另一条扩产路线。"
    ], deps=("chemical_plant",), items=("gtnn:mv_dehydrator",)),
    q("plant_grades", "外壳、管道与电压", "gtnn:exxonmobil_chemical_plant", (9, -12), [
        "化工厂外壳要求从青铜、钢、铝、不锈钢、钛到钨钢逐级提升。高等级外壳满足较低等级要求，但结构有效不代表电压条件已经满足。",
        "RP-1 混合需要不锈钢外壳与 HV；高密度肼混合燃料需要钛外壳与 HV；富集硅岩溶液合成需要钨钢外壳与 LuV。机器方块限制可运行的配方等级。",
        "管道等级提高并行，线圈等级缩短耗时。扩产前同时核算能源仓、液体输入速度和副产物出口，避免高并行只把堵塞提前。"
    ], deps=("dehydration",), check="核对目标配方的外壳、机器方块与供能等级"),
    q("catalyst_hatch", "专用催化剂仓", "gtnn:catalyst_hatch", (12, -12), [
        "催化剂仓用 MV 组装机制造，但方块自身的注册等级是 IV。这两个等级分别描述制造配方和机器部件，不能据此把整个化工厂判成 IV。",
        "催化剂放入专用催化剂仓，常规物品输入仓不能代替它。化工厂结构最多接受两个催化剂仓；按配方要求准备数量并留意催化剂的耗损。",
        "钒与钯制成的橙色金属催化剂参与蒽醌制过氧化氢；钯碳催化剂用于富集硅岩溶液合成。把催化剂供应纳入工厂物流。"
    ], deps=("plant_grades",), items=("gtnn:catalyst_hatch",)),

    q("platinum_feed", "铂族原料分流", "gtceu:platinum_metal_dust", (3, -6), [
        "困难铂线将部分矿物副产物改为铂金属粉和钯金属粉。它们仍是待精炼的中间产物，不能直接代替配方中的纯铂粉、纯钯粉。",
        "先对照镍矿、黄铜矿、镍黄铁矿的当前处理页，保留有价值的含铂副产物。为金属粉、铂族泥渣及稀有金属混合物设置独立出口。"
    ], deps=("process_plan",), items=("gtceu:platinum_metal_dust", "gtceu:palladium_metal_dust")),
    q("pgs_separation", "铂族泥渣先离心", "gtceu:mv_centrifuge", (6, -6), [
        "从洁净的镍黄铁矿或黄铜矿与硫酸制取铂族泥渣；不同处理路线的收率和配方电压不同，可按现有设备选择。",
        "GTNN 的泥渣先在离心机分离，得到二氧化硅、金、铂金属、钯金属、铱残渣和稀有金属混合物等出口。先把各个固体出口接齐，再启动连续生产。",
        "随后铂、钯分别进入自己的溶液精炼线；稀有金属混合物与铂渣用于继续回收其它铂族金属。"
    ], deps=("platinum_feed",), check="完成一批铂族泥渣离心并分离中间产物"),
    q("pure_platinum", "纯铂：沉淀与还原", "gtceu:platinum_dust", (9, -7.5), [
        "铂金属粉经硝化混酸生成浓缩铂溶液与铂渣，再用氯化铵进行沉淀分离。保留富钯氨溶液、氮氧化物与稀硫酸出口。",
        "筛分、焙烧得到的铂金属仍需要继续精炼。真正产出纯铂的一条末端配方是 4 份粗铂粉与 1 份钙粉，得到 2 份纯铂粉及氯化钙。",
        "粗铂的电解路线回到铂金属中间产物，并不直接得到纯铂；连接回流时要区分这两种出口。"
    ], deps=("pgs_separation",), items=("gtceu:platinum_dust",)),
    q("pure_palladium", "纯钯：富钯氨溶液", "gtceu:palladium_dust", (9, -4.5), [
        "钯金属与氨形成富钯氨溶液；铂线也会提供这股溶液。继续沉淀、筛分与还原，分开处理钯盐和粗钯中间产物。",
        "粗钯粉的纯化使用甲酸：4 份粗钯粉与 4000 mB 甲酸，得到 2 份纯钯粉和 4000 mB 氨。回收氨并补充甲酸，才能维持稳定循环。",
        "纯钯还要分配给化工催化剂与后续电子材料，避免所有产出都被合金线抽走。"
    ], deps=("pgs_separation",), items=("gtceu:palladium_dust",)),
    q("platinum_slag", "焦硫酸钾焙烧铂渣", "gtceu:potassium_pyrosulfate_dust", (12, -6), [
        "钾、硫和氧在 MV 化学工艺中合成焦硫酸钾，熔化后用于铂渣焙烧。这里的焦硫酸盐是其它铂族分支的关键试剂。",
        "铂渣与熔融焦硫酸钾在 775 K 电力高炉中反应，生成惰性金属混合物和硫酸铑气体。液体输出与固体输出都必须预留。",
        "惰性金属混合物进入钌线，硫酸铑气体进入铑线；这两股支流可与铂、钯主线并行运行。"
    ], deps=("pgs_separation",), items=("gtceu:potassium_pyrosulfate_dust",)),
    q("ruthenium_rhodium", "钌与铑的独立精炼", "gtceu:rhodium_dust", (15, -7.5), [
        "钌线从惰性金属混合物继续分离；铑线从硫酸铑气体转为盐类，经锌还原、再溶解与沉淀等步骤得到纯铑。两种金属不再由简单泥渣离心直接获得。",
        "按 JEI 分别设置硫酸盐、粗金属和滤饼回路。铑会用于后续高阶电路与电路装配线控制器，建议尽早保留独立缓冲。"
    ], deps=("platinum_slag",), items=("gtceu:ruthenium_dust", "gtceu:rhodium_dust")),
    q("iridium_osmium", "稀有金属中的铱与锇", "gtceu:iridium_dust", (15, -4.5), [
        "铂族泥渣离心产生的稀有金属混合物继续分离铱、锇等金属；铱残渣也有自己的处理步骤。保留这些中间物，而不是只收走纯铂。",
        "铂渣回收与稀有金属精炼共同补齐铂族供应。酸与卤素试剂并非每一步都完全回收，连续运行时要监测补料和废液出口。"
    ], deps=("pgs_separation",), items=("gtceu:iridium_dust", "gtceu:osmium_dust")),

    q("naquadah_oxides", "困难硅岩：氧化物原料", "gtceu:naquadah_oxide_mixture_dust", (3, 0), [
        "困难硅岩线把矿物产出与精炼中间产物改为硅岩、富集硅岩和超能硅岩氧化物混合粉，并移除部分旧离心捷径。三种氧化物要分别储存。",
        "基础硅岩经中子活化还原；富集与超能硅岩需经过氟化、溶液合成和后续活化。先规划氟锑酸、氢、氙、氟化物及废液的供给与回收。"
    ], deps=("process_plan",), items=("gtceu:naquadah_oxide_mixture_dust",)),
    q("precision_core", "LuV：精密组装与夸克核心", "gtnn:quark_core", (6, 0), [
        "GTNN 精密组装配方由 GCYM 大型组装机运行。先建造大型组装机并准备 LuV 供电；它没有可直接替代的单方块精密组装机。",
        "夸克核心配方需要 30720 EU/t，即 LuV，使用 IV 电路、钻石透镜、NAND 芯片、铝转子和聚乙烯、钠钾合金、润滑油、丁苯橡胶等液体。",
        "这里的 IV 电路只是配方原料等级，不能把机器电压降到 IV。精密组装配方也不能直接靠普通并行仓获得任意并行。"
    ], deps=("naquadah_oxides",), items=("gtceu:large_assembler", "gtnn:quark_core"), ext=("7033A24F37F9B9B9",)),
    q("neutron_activator", "中子活化器整机", "gtnn:neutron_activator", (9, 0), [
        "控制器在大型组装机的精密组装模式制造，配方标称 IV、7680 EU/t，并使用两个夸克核心。因此整机实际要到 LuV 核心可生产后才可建成。",
        "按 JEI 预览搭建可增高的中子活化器，装入中子源，并配置中子加速器与中子传感器。控制器本身的低电压加工不代表中子能量自动满足。",
        "中子源提供起始条件，加速器提高能量；反应目标看配方自己的中子能量区间。"
    ], deps=("precision_core",), items=("gtnn:neutron_activator", "gtnn:neutron_source")),
    q("neutron_control", "能量窗口与红石控制", "gtnn:neutron_sensor", (12, 0), [
        "用中子传感器读取能量并控制加速器，利用石墨粉或铍粉降低能量。加速器的档位只影响加速能力，不能代替配方要求的能量窗口。",
        "基础硅岩活化要求 220–230 MeV，富集硅岩阶段为 460–480 MeV，超能硅岩阶段为 1050–1100 MeV。先稳定在目标窗口内再连续供料。",
        "能量不在配方窗口内会得到放射性废物，超过 1200 MeV 会触发整机爆炸。给加速器设置自动停机阈值，并为废物预留出口。"
    ], deps=("neutron_activator",), items=("gtnn:neutron_sensor", "gtnn:ulv_neutron_accelerator")),
    q("naquadah_reduction", "基础硅岩活化还原", "gtceu:naquadah_dust", (15, 0), [
        "4 份硅岩氧化物混合粉与 6000 mB 氟锑酸进入中子活化器，在 220–230 MeV 得到 2 份硅岩粉、氟化钛以及概率产出的镓。",
        "产物含粉末和氟化物副产物，不能只连接硅岩出口。先验证一次完整还原，再把氟锑酸补料和能量闭环自动化。"
    ], deps=("neutron_control",), items=("gtceu:naquadah_dust",)),
    q("enriched_naquadah", "富集硅岩：钨钢化工厂", "gtceu:enriched_naquadah_ingot", (18, 0), [
        "富集氧化物经 EV、IV 氟化与溶液工艺处理后，与氙配合物、氟锑酸、氢和三氟化金在 LuV 化工厂合成。该步骤要求钨钢外壳与钯碳催化剂。",
        "残渣溶液经 EV 脱水机处理，后续电解可回收部分氙、金和含氟试剂。洁净室条件与不同溶液的浓度也要分别满足。",
        "酸性富集硅岩溶液在 460–480 MeV 活化为富集硅岩硫酸盐，并副产不纯超能硅岩溶液。硫酸盐仍需高炉还原与真空冷却，才得到富集硅岩锭。"
    ], deps=("naquadah_reduction",), items=("gtceu:enriched_naquadah_ingot",)),
    q("naquadria", "超能硅岩：高能活化与高炉", "gtceu:naquadria_ingot", (21, 0), [
        "继续处理不纯超能硅岩溶液，建立氟化、氡与氙及铯盐循环。部分步骤要求洁净室，废液脱水也要分开接线。",
        "酸性超能硅岩氟化铯溶液在 1050–1100 MeV 活化，得到超能硅岩硫酸盐、铯和氟。这个窗口离 1200 MeV 上限较近，必须让加速器自动停机。",
        "硫酸盐进入 9000 K、ZPM 配方的电力高炉还原为热超能硅岩，再经真空冷却得到锭。完成活化并不等于已经获得可用的超能硅岩金属。"
    ], deps=("enriched_naquadah",), items=("gtceu:naquadria_ingot",), ext=("370FC1CDF5F9A9CC",)),
    q("naquadah_generators", "固体硅岩燃料与回收", "gtnn:ev_naquadah_reactor", (21, 3), [
        "GTNN 固体燃料硅岩反应堆提供 EV、IV、LuV、ZPM 与 UV 机型。较低三档使用不同形态的富集硅岩，较高两档使用超能硅岩。",
        "燃料分别加工为螺栓、棒或长棒，并回收对应形态的普通硅岩产物。为燃料输入和回收品分别安排库存，核对实际发电配方后再决定升级机型。",
        "大型硅岩反应堆还有独立的液体燃料工艺。固体燃料机型与液体燃料线的物料、设备不能混为一套配方。"
    ], deps=("enriched_naquadah",), items=("gtnn:ev_naquadah_reactor",)),

    q("heavy_alloy_t1", "HV：T1 重型合金", "gtnn:heavy_plate_t1", (3, 6), [
        "本包使用 GCYR 与 GTNN 联动材料链。T1 重型合金锭在 HV 组装机制造：黄铜、铝和钢的致密板各一份，加入 72 mB 不锈钢，电路 1、耗时 15 秒。",
        "若某种致密板标签为空，配方用九倍普通板替代，保持金属总量。重型合金锭随后在内爆压缩机压成重型板，准备炸药并给暗灰副产物留出槽位。",
        "GCYR 基础火箭发动机和基础燃料罐各需要两块 T1 重型板，属于 HV 部件。"
    ], deps=("process_plan",), items=("gtnn:heavy_ingot_t1", "gtnn:heavy_plate_t1"), ext=("2D01E80DE837F6A6",)),
    q("heavy_alloy_t23", "EV / IV：逐级升级重型锭", "gtnn:heavy_plate_t3", (6, 6), [
        "T2 在 EV 组装：T1 重型锭加两份钛致密板、72 mB 钨钢。T3 在 IV 组装：T2 加四份钨钢致密板、72 mB 铂。两步均用电路 1、耗时 15 秒。",
        "继续使用内爆压缩机把对应级别的锭压成板。本包用钛与钨钢替代未安装的 Ad Astra 金属，直接查看当前 JEI，不要寻找 Desh 或 Ostrum。",
        "GCYR 先进发动机与燃料罐在 IV 使用 T2 板；精英部件在 ZPM 使用 T3 板。重型板生产电压与火箭部件组装电压不是同一个等级。"
    ], deps=("heavy_alloy_t1",), items=("gtnn:heavy_plate_t2", "gtnn:heavy_plate_t3")),
    q("heavy_alloy_t4", "LuV：T4 与太空重工业", "gtnn:heavy_plate_t4", (9, 6), [
        "T4 在 LuV 组装：T3 重型锭、四份硅岩合金致密板与 72 mB GCYR 三硅岩合金，电路 1、15 秒。硅岩合金加工与三硅岩合金供应都要接入。",
        "T4 同样通过内爆压缩机制成重型板，用于高阶太空设施与后续跨模组制造。扩建前先确认硅岩、铂和炸药的稳定吞吐。"
    ], deps=("heavy_alloy_t23",), items=("gtnn:heavy_plate_t4",)),
    q("rp1_fuel", "煤油与 RP-1 燃料", "gtceu:mv_distillery", (3, 9), [
        "用 MV 蒸馏室把柴油加工成煤油，再精馏 RP-1 火箭煤油，两步集成电路均设为 23。不要把中间的煤油直接当作最终混合燃料。",
        "最终 RP-1 燃料在 HV 化工厂混合氧与 RP-1 火箭煤油，要求不锈钢外壳、电路 1。先稳定氧气与柴油供应，再接入火箭燃料仓。"
    ], deps=("process_plan",), check="完成柴油、煤油、RP-1 与氧混合的燃料链"),
    q("hydrazine_fuel", "过氧化氢与肼", "gtnn:exxonmobil_chemical_plant", (6, 9), [
        "过氧化氢使用蒽醌循环，并需要橙色金属催化剂。其上游邻苯二甲酸酐等步骤到 EV，因此最终混合配方为 HV 并不代表整条原料链在 HV 就齐全。",
        "氨与过氧化氢在钢外壳化工厂制肼，HV、电路 2；甲醇与肼进一步混合为高密度肼燃料，则需要钛外壳，仍为 HV、电路 2。",
        "分别储存肼、甲醇、过氧化氢与混合燃料。催化剂与循环载体补料不足时，先解决上游再增加工厂并行。"
    ], deps=("rp1_fuel",), check="建立过氧化氢循环并生产高密度肼混合燃料"),
    q("advanced_rocket_fuel", "高级推进剂与 GCYR 接口", "gtnn:exxonmobil_chemical_plant", (9, 9), [
        "甲基肼在铝外壳化工厂合成；甲基肼与硝酸混合成硝酸甲基肼燃料则需要不锈钢外壳。继续按 JEI 处理偏二甲肼等高级推进剂。",
        "本包把 RP-1、高密度肼、偏二甲肼和硝酸甲基肼燃料加入 GCYR 火箭燃料规则，分别覆盖一至三档推进需求。按火箭实际要求选择已解锁的推进剂。",
        "配方 duration 属于 GCYR 燃料消耗计算参数，不能直接解释成现实秒数的续航。火箭部件材料线与燃料线可并行建设。"
    ], deps=("hydrazine_fuel",), check="核对火箭燃料等级并接通一种高阶推进剂"),
    q("rocket_power", "火箭燃料也可发电", "gtnn:ev_rocket_engine", (12, 9), [
        "GTNN 火箭发动机提供 EV、IV 与 LuV 机型，是消耗推进剂的发电设备，与 GCYR 火箭的发动机物品用途不同。",
        "本包联动额外接受 GT 火箭燃料、汽油、柴油以及可用的氢气来源。按 JEI 查看单次用量、耗时与输出，不能直接把火箭航行续航套用为发电收益。",
        "推进剂产线需要电解、化工与蒸馏的上游能耗。先测量实际供给能否覆盖发电消耗，再决定是否扩展为基地常用电源。"
    ], deps=("advanced_rocket_fuel",), items=("gtnn:ev_rocket_engine",)),

    q("circuit_line", "LuV：专用电路装配线", "gtnn:circuit_assembly_line", (3, 15), [
        "GTNN 电路装配线控制器使用 GT 装配线制造，配方电压 LuV、30720 EU/t，基础耗时 20 分钟。准备 LuV 电路组装机、LuV 部件、铑板与 Cerrobase-140 合金。",
        "控制器制造配方的前置研究是扫描 LuV 电路组装机，使用数据棒。这一步与后面为量产电路制作蚀刻模板是两套研究，不能共用任意一根数据棒。",
        "专用装配线支持自己的批量电路配方，也能处理符合等级限制的常规电路组装机配方。先按预览完成结构，再安排沿线输入仓。"
    ], deps=("process_plan",), items=("gtnn:circuit_assembly_line",), ext=("7033A24F37F9B9B9",)),
    q("etched_sample", "蚀刻处理器样品", "gtnn:etched_micro_processor", (6, 15), [
        "使用激光蚀刻机把处理器电路制成蚀刻样品。微处理器使用黄色透镜、MV 配方；纳米、量子、晶体与湿件依次采用对应等级的透镜与电压。",
        "蚀刻过程消耗处理器样品。选择希望长期量产的电路，再为它准备研究模板，避免只拿到普通电路就期待装配线自动识别。"
    ], deps=("circuit_line",), items=("gtnn:etched_micro_processor",)),
    q("etched_base", "电路板与蚀刻基板", "gtnn:etched_micro_processor_base", (9, 15), [
        "蚀刻样品先与塑料电路板和胶水在 LV 组装机制成对应蚀刻电路板，再用成型机与四片锻铁箔压成蚀刻基板。",
        "普通处理器、蚀刻样品、蚀刻电路板和蚀刻基板是不同物品。后续扫描器需要基板，按工序逐步制造并保留额外样品。"
    ], deps=("etched_sample",), items=("gtnn:etched_micro_processor_board", "gtnn:etched_micro_processor_base")),
    q("circuit_template", "IV 扫描：量产数据棒", "gtceu:data_stick", (12, 15), [
        "把目标蚀刻基板交给扫描器，配方需要 IV、7680 EU/t，基础耗时 30 秒，得到绑定该电路工艺的数据棒。研究用数据棒必须携带正确 NBT。",
        "将研究成果放入电路装配线控制器的配置槽，而不是普通物品输入仓。配方需要匹配的数据类型与研究内容；空白数据棒没有作用。",
        "控制器会保存专用工艺的模板锁定状态。部署前选定这条装配线负责的电路系列，并使用实际研究数据验证首次运行。取得任务只检查数据棒物品，下面的勾选需在机器验证后完成。"
    ], deps=("etched_base",), items=("gtceu:data_stick",), check="将正确研究数据棒装入控制器并验证工艺识别"),
    q("packaged_components", "封装元件与有序输入", "gtnn:wrap_smd_transistor", (15, 15), [
        "批量配方会使用封装后的 SMD 元件。常见封装在 LV 组装机中由 16 个元件与 72 mB 聚乙烯加工，电路设为 16；按目标配方分别准备电阻、电容、晶体管等。",
        "部分导线还会改用多股规格，数量与形态以批量配方页为准。不能只把普通电路组装机的原料整体乘十六后塞进同一个仓。",
        "本包启用装配线有序物品输入。按机器相对右侧排列输入仓，并严格对应 JEI 物品顺序；数据棒留在控制器，流体按各自输入仓供应。"
    ], deps=("circuit_template",), items=("gtnn:wrap_smd_transistor", "gtnn:wrap_smd_capacitor", "gtnn:wrap_smd_resistor")),
    q("circuit_batch", "十六倍产出与运行边界", "gtnn:circuit_assembly_line", (18, 15), [
        "GTNN 为选中的电路工艺生成批量配方：基础产出为原配方的十六倍，耗时三倍，超频前 EU/t 保持原值。只有符合转换规则的配方具备这个倍率，不能推广到所有电路。",
        "常规电路组装机配方在此还要求配方电压等级严格低于机器输入电压等级；同档供电可能无法运行。专用批量配方另有研究数据与模板条件。",
        "验证一批完整生产：确认研究数据、输入顺序、元件封装、流体供给与输出容量，记录基础耗时和实际产出，再逐步提高供电与吞吐。"
    ], deps=("packaged_components",), check="完成一批电路生产并确认倍率、顺序与研究条件"),
]


# Accepted by the layout optimizer: no edge crossings or conservative node hits.
LAYOUT_Y = {
    "chemical_plant": -9.5, "dehydration": -9.5, "plant_grades": -9.5,
    "catalyst_hatch": -9.5, "platinum_feed": -4.5, "pgs_separation": -4.0,
    "pure_platinum": -6.0, "pure_palladium": -2.0, "platinum_slag": -3.0,
    "ruthenium_rhodium": -2.5, "iridium_osmium": -5.0,
    "naquadah_oxides": -0.5, "precision_core": 0.5, "neutron_activator": 0.5,
    "neutron_control": 0.0, "naquadah_reduction": 0.0, "enriched_naquadah": 0.0,
    "naquadria": -0.5, "naquadah_generators": 2.0, "heavy_alloy_t1": 2.5,
    "heavy_alloy_t23": 4.5, "heavy_alloy_t4": 4.5, "rp1_fuel": 6.5,
    "hydrazine_fuel": 9.0, "advanced_rocket_fuel": 8.0, "rocket_power": 7.5,
    "circuit_line": 13.0, "etched_sample": 11.5, "etched_base": 12.0,
    "circuit_template": 11.5, "packaged_components": 12.0, "circuit_batch": 12.0,
}
for entry in Q:
    entry["xy"] = (entry["xy"][0], LAYOUT_Y.get(entry["key"], entry["xy"][1]))

SPEC = dict(
    filename="gtnn_industry",
    salt="society-gtnn-industry-v1",
    order=24,
    title="GTNN：工艺与量产",
    icon="gtnn:exxonmobil_chemical_plant",
    quests=Q,
    minimum_spacing=2.5,
)


if __name__ == "__main__":
    run(SPEC)
