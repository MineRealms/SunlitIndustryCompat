#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Astral Sorcery FTBQ 2001.4.17. Python 3.10+, nbtlib 2.0.4.

Default: validate in memory, then publish only this chapter and its ID map.
--check: read-only validation including exact published bytes.
--self-test: additional in-memory negative and semantic-ID tests.
Never installs a mod or changes another chapter. IDs are business-key hashes.
"""
from pathlib import Path
import argparse
import copy
import hashlib
import itertools
import json
import math
import re
import struct
import zipfile
import nbtlib

TOOLS = Path(__file__).resolve().parent
ROOT = TOOLS.parents[2]
QUESTS = ROOT / 'config/ftbquests/quests'
OUTPUT = QUESTS / 'chapters/astral_sorcery.snbt'
MAP = TOOLS / 'astral_sorcery_ids.json'
SOURCE = Path(r'H:\MinecraftMods\AstralSorcery-1.20.1')
JAVA = SOURCE / 'src/main/java/hellfirepvp/astralsorcery/common'
JAR = SOURCE / 'build/libs/AstralSorcery-1.20.1.0.jar'
GROUP = '04716B2F19F75F08'
SALT = 'astral_sorcery'


def digest(data):
    return hashlib.sha256(data).hexdigest()


class SnbtParser:
    """Recursive FTB relaxed-SNBT reader; optional commas, comments, suffixes.

    Used to inspect the existing library, not as the sole candidate validator.
    Generated standard SNBT is independently parsed by nbtlib.
    """
    token = re.compile(r'''\s+|\#[^\n]*|//[^\n]*|/\*[\s\S]*?\*/|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|[{}\[\]:,;]|[^\s{}\[\]:,;"']+''')

    def __init__(self, text):
        text = text.lstrip('\ufeff')
        self.tokens = []
        end = 0
        for m in self.token.finditer(text):
            if m.start() != end:
                raise ValueError('Invalid SNBT token at ' + str(end))
            end = m.end()
            t = m.group()
            if not t.isspace() and not t.startswith(('#', '//', '/*')):
                self.tokens.append(t)
        if end != len(text):
            raise ValueError('Invalid SNBT trailing token')
        self.i = 0

    def peek(self):
        return self.tokens[self.i] if self.i < len(self.tokens) else None

    def take(self):
        if self.peek() is None:
            raise ValueError('Unexpected SNBT EOF')
        t = self.peek()
        self.i += 1
        return t

    def expect(self, expected):
        if self.take() != expected:
            raise ValueError('Expected SNBT token ' + expected)

    @staticmethod
    def string(t):
        return re.sub(r'\\(.)', lambda m: {'n': '\n', 'r': '\r', 't': '\t'}.get(m[1], m[1]), t[1:-1])

    def value(self):
        t = self.take()
        if t == '{':
            out = {}
            while self.peek() != '}':
                key = self.take()
                if key.startswith(('"', "'")):
                    key = self.string(key)
                self.expect(':')
                if key in out:
                    raise ValueError('Duplicate SNBT key: ' + key)
                out[key] = self.value()
                if self.peek() == ',':
                    self.take()
            self.take()
            return out
        if t == '[':
            out = []
            if self.peek() in ('B', 'I', 'L') and self.tokens[self.i+1:self.i+2] == [';']:
                self.take(); self.take()
            while self.peek() != ']':
                out.append(self.value())
                if self.peek() == ',':
                    self.take()
            self.take()
            return out
        if t.startswith(('"', "'")):
            return self.string(t)
        if t in ('true', 'false'):
            return t == 'true'
        if re.fullmatch(r'[-+]?\d+[bBsSlL]?', t):
            return int(t.rstrip('bBsSlL'))
        if re.fullmatch(r'[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?[fFdD]?', t):
            return float(t.rstrip('fFdD'))
        if t in ('}', ']', ':', ',', ';'):
            raise ValueError('Unexpected SNBT punctuation')
        return t

    def parse(self):
        out = self.value()
        if self.peek() is not None:
            raise ValueError('Trailing SNBT content')
        return out


def dump(v, depth=0):
    tab = '\t' * depth
    if isinstance(v, dict):
        return '{\n' + ',\n'.join(tab+'\t'+k+': '+dump(x, depth+1) for k, x in v.items()) + '\n'+tab+'}'
    if isinstance(v, list):
        return '[\n' + ',\n'.join(tab+'\t'+dump(x, depth+1) for x in v) + '\n'+tab+']' if v else '[]'
    if isinstance(v, str):
        return json.dumps(v, ensure_ascii=False)
    if isinstance(v, bool):
        return str(v).lower()
    if isinstance(v, float):
        return f'{v:.1f}d'
    return str(v)


def walk(value):
    if isinstance(value, dict):
        yield value
        for child in value.values():
            yield from walk(child)
    elif isinstance(value, list):
        for child in value:
            yield from walk(child)


def valid_id(value):
    return isinstance(value, str) and re.fullmatch('[0-9A-F]{16}', value) and 0 < int(value, 16) <= 0x7FFFFFFFFFFFFFFF


def stable_id(key):
    value = int.from_bytes(hashlib.sha256((SALT+':'+key).encode('utf-8')).digest()[:8], 'big') & 0x7FFFFFFFFFFFFFFF
    if not value:
        raise ValueError('Zero hash for ' + key)
    return f'{value:016X}'


PANELS = [
    ('discovery', 'A 发现', 'DISCOVERY', (0, 0), False),
    ('exploration', 'B 探索', 'BASIC_CRAFT', (12, 0), False),
    ('light', 'C 共鸣·光路', 'ATTUNEMENT', (24, 0), False),
    ('ritual', 'D 共鸣·仪式', 'ATTUNEMENT', (24, 13.5), True),
    ('constellation', 'E 星座', 'CONSTELLATION', (12, 13.5), True),
    ('radiance', 'F 五彩', 'RADIANCE', (0, 13.5), True),
]
Q = []


def q(key, title, icon, text, items=(), check=None, pages=()):
    # Item targets are existence checks. Checkmarks are explicitly manual, never
    # presented as automatic research, structure, active-star, or NBT detection.
    Q.append(dict(key=key, title=title, icon='astralsorcery:'+icon,
                  description=text.split('\n'), items=['astralsorcery:'+i for i in items],
                  check=check, pages=list(pages)))
    return key


def content():
    Q.clear()
    groups = {}
    q('welcome', 'A 发现：星芒宝典', 'tome',
      '从星芒宝典的“发现”页开始。宝典以研究进度开放内容，本章按实际手册分成发现、探索、共鸣、星座、五彩。\n任务物品仅检查取得，不消耗；手动勾选只代表你确认完成实作，不自动检测研究或结构。升级祭坛会原地替换，请先完成前一级目标，必要时挖下新祭坛供背包检测后再放回。\n地图上排从左向右，下排从右向左。各区入口需要对应阶段设备；箭头是局部学习顺序，不是完整配方表。', ('tome',), pages=('WELCOME',))
    q('shrine', '学院祭坛与露天水晶', 'rock_collector_crystal',
      '寻找学院遗迹。大型祭坛的浮动聚能水晶可帮助入门，宝箱可能有星图。按手册移除水晶上方遮蔽，让它能接收星光；不要把受保护的遗迹水晶当作可带回家的成品。', check='手动确认：找到祭坛并检查水晶上方遮挡', pages=('SHRINES',))
    q('paper', '星图不是星座发现', 'constellation_paper',
      '获取并查看星图，研究水平足够时它会揭示星座资料。Shift 打开宝典可存放星图；拿到星图不等于已经在夜空描绘并发现星座。', ('constellation_paper',), pages=('CPAPER',))
    q('sky_ready', '为观星做好准备', 'parchment',
      '核对宝典中的星图和观星地点。探索阶段制作手持望远镜，再按图描绘；不要用羊皮纸目标假装自动发现星座。', check='手动确认：已阅读星图并选好无遮挡观星地点', pages=('CPAPER',))
    q('aquamarine', '河岸的海蓝宝石', 'aquamarine',
      '寻找海蓝宝石砂岩并取得海蓝宝石。手册将其描述为河岸、沙滩附近的星能敏感资源；它用于宝典、共振星杖和早期透镜。', ('aquamarine',), pages=('ORES',))
    q('wand', '共振星杖：引导合成', 'wand',
      '用当前配方制作共振星杖（共鸣法杖）。在祭坛摆好原料后用它右击启动；夜间在露天手持还可寻找地下晶石的星能迹象。它也能提示多方块结构中缺失或放错的部件。', ('wand',), pages=('WAND',))
    q('altar1', '星辉合成台', 'altar_discovery',
      '把普通工作台放在学院聚能水晶附近，让星光转化为星辉合成台。自己采集星能时，正上方须无遮挡；高处与汇星浓度会影响可用星能。\n星能槽反映当前环境可用量，不是一直充电的电池。放好配方后用共振星杖右击。下一分区从这台设备起步。', ('altar_discovery',), pages=('ALTAR1',))
    q('marble', '星术师的大理石', 'marble_raw',
      '准备本模组的大理石，并加工雕纹、精致、柱与横梁等变体。结构辨认的是具体方块，不要把别的模组同名石材当作替代品。', ('marble_raw', 'marble_chiseled'), pages=('MARBLETYPES',))
    q('sooty', '炭黑大理石', 'black_marble_raw',
      '制作炭黑大理石，后续祭坛底座会大量使用。按当前配方加工，不把炭黑与普通大理石互换。', ('black_marble_raw',), pages=('SOOTYMARBLE',))
    q('structure_reading', '读懂结构分层图', 'marble_pillar',
      '在宝典结构页切换分层视图，核对方块变体、柱高和空气位置。任务中取得控制方块并不代表结构已成型；用共振星杖检查错误。', check='手动确认：能按宝典分层图辨认建材与空气位置', pages=('MARBLETYPES', 'WAND'))
    groups['discovery'] = ('welcome', [['shrine', 'paper', 'sky_ready'], ['aquamarine', 'wand', 'altar1'], ['marble', 'sooty', 'structure_reading']])

    q('explore', 'B 探索：让星光工作', 'altar_discovery',
      '已在发现阶段取得星辉合成台。这里对应 BASIC_CRAFT，宝典显示为“探索”，不是另一个额外研究等级。\n四条支线分别是液体、观星、照明与供能；升级用的聚星缸、晶石、星能液需一并准备。', ('altar_discovery',), pages=('WELL', 'ALTAR2'))
    q('crystal', '水晶石与属性', 'rock_crystal',
      '夜间利用共振星杖寻找晶石迹象，再用足够等级的镐开采水晶石矿石。保留不同属性的晶石，避免把唯一一枚投入消耗型催化流程。手册建议用精细方法处理，而非粗暴磨石。', ('rock_crystal',), pages=('ROCK_CRYSTALS',))
    q('well', '聚星缸：星辉池', 'well',
      '在星辉合成台制作聚星缸。右击放入海蓝宝石等催化剂，催化剂会逐渐耗损且不能随意取回。提供的星能越多，产液越快。\n保持顶部开口通畅；下部可接主动抽液设备。不要把聚星缸当作可任意回灌的储罐。', ('well',), pages=('WELL',))
    q('liquid', '星能液与晶石处理', 'liquid_starlight_bucket',
      '用桶收集星能液。按手册把晶石浸入星能液可使其成长；两个晶石可能发生属性合并。先留存一枚原始晶石作比较。液体与其他流体交互时会消耗，试验应放在独立区域。', ('liquid_starlight_bucket',), pages=('WELL', 'CRYSTAL_GROWTH'))
    q('glass', '玻璃透镜', 'glass_lens',
      '在星辉合成台把海蓝宝石与玻璃板加工为玻璃透镜。它是手持望远镜、星辉转继器等设备的基础材料，不同于放在世界中传光的水晶透镜方块。', ('glass_lens',), pages=('HAND_TELESCOPE',))
    q('looking', '手持望远镜', 'hand_telescope',
      '制作手持望远镜，在无遮挡的夜空观测。手册说明它只适合最明亮的星座，并且每晚只有一个星座能通过这种简易工具显现。', ('hand_telescope',), pages=('HAND_TELESCOPE',))
    q('bright', '描绘第一个明亮星座', 'constellation_paper',
      '先看过相应星图，再在星座出现的夜晚观测。按住 Shift 固定视角，用左键准确描绘星点；未固定或描绘不准确不会真正发现。这个目标需要手动确认。', check='手动确认：已在夜空描绘并发现一个明亮星座', pages=('HAND_TELESCOPE',))
    q('powder', '辉光粉与浮空光源', 'illumination_powder',
      '制作辉光粉并投出，可留下浮空光源。液体或方块覆盖可移除这些光源；不是所有发光效果都对应可采集的物品。', ('illumination_powder',), pages=('ILLUM_POWDER',))
    q('illuminator', '洞穴照明器（光照仪）', 'illuminator',
      '制作洞穴照明器。按手册，它借助夜间汇星之能在下方被天空遮蔽的区域布置光源；后期辉光星杖也能为它供能。拆掉设备不会同时清除已放的光源。', ('illuminator',), pages=('ILLUMINATOR',))
    q('nocturnal', '暗夜粉', 'nocturnal_powder',
      '暗夜粉形成临时黑暗云并产生当地群系的怪物。它不会遮断阳光对亡灵的影响；同处反复或过密使用会降低效果。它是可选应用，也会用于后期配方。', ('nocturnal_powder',), pages=('NOC_POWDER',))
    q('wood', '浸润注星木', 'infused_wood',
      '先完成聚星缸支线并取得星能液，再把木材浸泡其中得到注星木。它用于共振器和后续传光设备；继续加工可得到注星木板等变体。', ('infused_wood',), pages=('INFUSED_WOOD',))
    q('resonator', '寻找汇星之域', 'resonator',
      '制作汇星共振器，夜间手持寻找高浓度蓝色云区；云内的星芒提示浓度更高的位置。它帮助选址，不是共振星杖，也不是仪式范围升级模式。', ('resonator',), pages=('SKY_RESO',))
    q('relay_upgrade', '转继供能与二阶祭坛', 'altar_attunement',
      '转继器需要 3×3 底座：中央炭黑大理石、四角雕纹大理石、四边横梁；放上转继器并额外插入玻璃透镜。手册记录供能距离为 16 格。\n备齐晶石、星能液等升级材料后，把星辉合成台升级为星辉祭坛。其结构外缘 9×9，必须按宝典分层图施工；红色星能槽提示结构有误。升级原地发生，必要时挖下拾取再放回。', ('spectral_relay', 'glass_lens', 'altar_attunement'), pages=('SPEC_RELAY', 'ALTAR2'))
    groups['exploration'] = ('explore', [['crystal', 'well', 'liquid'], ['glass', 'looking', 'bright'], ['powder', 'illuminator', 'nocturnal'], ['wood', 'resonator', 'relay_upgrade']])

    q('light', 'C 共鸣：建立光路', 'altar_attunement',
      '以探索阶段的二阶星辉祭坛进入共鸣。注意 altar_attunement 是合成祭坛，attunement_altar 才是供玩家与晶石共鸣的独立设备。\n这里先解决星辉锭与星尘，三阶祭坛也依赖这些材料。', ('altar_attunement',), pages=('STARLIGHT_NETWORK',))
    q('network', '连接器与水晶透镜', 'linking_tool',
      '先右击聚能源，再右击目标建立光路。普通水晶透镜可以多源接收，但只向一个目标发送。源连接多个目标会均分能量，删除不用的连接。\n开始时可利用学院水晶；自制聚能水晶在星座阶段才制作，不把它倒置成星辉锭的前置。', ('linking_tool', 'lens'), pages=('LINKTOOL', 'LENS'))
    q('metal', '铁矿石到星辉锭', 'starmetal_ingot',
      '把聚能水晶连接到铁矿石，持续星能照射会将其转化为星辉矿石，再熔炼取得星辉锭。取得矿石后先完成检测，再投入熔炼；不是用普通铁锭直接照射。', ('starmetal_ore', 'starmetal_ingot'), pages=('STARMETAL_ORE',))
    q('dust', '切削工具与星尘', 'chisel',
      '用星辉锭、注星木板等制作切削工具。在世界中把星辉锭放到地面并用工具敲击获得星尘，不是工作台里的无序配方。\n星尘用于三阶天辉祭坛；当前升级配方允许普通晶石，不必先有天辉水晶。', ('chisel', 'stardust'), pages=('CUTTING_TOOL', 'STARDUST', 'ALTAR3'))
    q('telescope', '固定式天文望远镜', 'telescope',
      '升级手持望远镜。天文望远镜可以查看当晚出现的明亮与暗淡星座，转动方向查看夜空的八个区域；这里不是“天空只有八个星座”。周围应无遮挡。', ('telescope',), pages=('TELESCOPE',))
    q('dim', '理解暗淡星座的门槛', 'constellation_paper',
      '先完成 D 区个体共鸣，才能理解五个明亮星座之外的星座。设备能看到与玩家知识已足够是两项条件。参考星图，在合适夜晚发现一个暗淡星座。', check='手动确认：完成个体共鸣并发现一个暗淡星座', pages=('TELESCOPE', 'ATT_PLAYER'))
    q('share', '分享发现', 'knowledge_share',
      '制作知识共享卷轴。手册说明右击可写入或学习，Shift 右击覆盖已有知识。多人共用任务进度不等于模组研究已自动同步给所有人。', ('knowledge_share',), pages=('KNOWLEDGE_SHARE',))
    q('charge', '个人充能与工具', 'wand',
      '手册把个人星能充能与世界中的汇星之域区分开来。充能会从环境恢复，夜空下更有利；不同工具消耗不同，不能把它当作无限能源。', check='手动确认：已阅读个人充能及恢复方式', pages=('QUICK_CHARGE', 'TOOL_CHANNEL'))
    q('wands', '建造与移动星杖', 'architect_wand',
      '制作排列星杖与越空星杖。排列星杖需要背包内的实际建材；越空星杖有冲驰与闪现模式，使用前确认目标方块面。后续可选更替星杖与冲击星杖。', ('architect_wand', 'blink_wand'), pages=('TOOL_WANDS', 'TRAVERSAL_WAND', 'GRAPPLE_WAND'))
    q('gateway', '天辉星门', 'celestial_gateway',
      '先准备本区星尘支线的材料，再制作星门并按宝典搭建结构。站上星门观察其他目的地，面对目标按 Shift 或右击传送。可用铁砧命名，私有权限需按手册另行配置。', ('celestial_gateway',), pages=('CELESTIAL_GATEWAY',))
    groups['light'] = ('light', [['network', 'metal', 'dust'], ['telescope', 'dim', 'share'], ['charge', 'wands', 'gateway']])

    q('ritual', 'D 共鸣：独立共鸣祭坛', 'attunement_altar',
      '用二阶合成祭坛与星辉锭制作独立的共鸣祭坛。源码的中心炭黑大理石为 15×15，含外侧角部的结构总占地为 19×19；占地不是施工图，请按宝典分层放柱与横梁。', ('attunement_altar',), pages=('ATT_PLAYER',))
    q('pattern', '把星座摆到地面', 'spectral_relay',
      '夜间手持星图查看转继器位置，按星座图形摆在炭黑底座上。源码不仅检查图形，还检查星座当前月相是否活跃；祭坛也要看到天空。不要假设任何星座每晚都可用。', ('spectral_relay',), check='手动确认：结构、星座图形及当晚活跃条件均满足', pages=('ATT_PLAYER',))
    q('self_attune', '与明亮星座共鸣', 'tome',
      '选择明亮级星座，站上有效共鸣祭坛中心进行个体共鸣。个人只能与明亮星座共鸣；它会影响星能力起点与成长方式。', check='手动确认：已完成个体共鸣并打开宝典星能力页', pages=('ATT_PERKS',))
    q('reset', '封禁与重置不是一回事', 'perk_seal',
      '封禁印章临时抑制一个星能力，不影响其他节点。普通更替之星会清除个人共鸣及成长进度；先阅读说明再使用，任务只要求制作，不要求实际重置。后期灿芒之星可保留成长进度重新分配。', ('perk_seal', 'shifting_star'), pages=('ATT_PERKS_SEAL', 'SHIFT_STAR'))
    q('attuned', '共鸣水晶石', 'attuned_rock_crystal',
      '同样需要夜晚、有效图形与当前活跃星座，把未共鸣水晶放到祭坛中心进行共鸣。当前实现会将普通晶石转换为对应共鸣物品。\n物品目标不校验具体星座 NBT，务必查看提示确认共鸣星座。', ('attuned_rock_crystal',), pages=('ATT_CRYSTAL',))
    q('pedestal', '仪式基座', 'ritual_pedestal',
      '按宝典搭好基座支持结构并留出要求的空气。放上共鸣晶石产生对应效果；外接星能源必须与晶石星座一致，才可增强仪式。', ('ritual_pedestal',), check='手动确认：已搭建结构并用共鸣晶石启动仪式', pages=('RIT_PEDESTAL',))
    q('balance', '仪式促能与平衡', 'lens',
      '自制聚能水晶在 E 区解锁后，可进一步提供同星座能量。按射线提示放置透镜并连回基座；手册规定最多五次反射即可完全平衡。\n不同仪式可能增大范围或提高速度；不要照搬旧中文“过载必然碎裂”的说法。', ('lens',), check='手动确认：已阅读并理解同星座供能和透镜平衡', pages=('PED_ACCEL',))
    q('gem_seed', '培育能力宝石簇', 'illumination_powder',
      '将水晶石与辉光粉投入放在世界中的星能液，形成宝石水晶簇。它与用星尘培育的天辉水晶簇不是同一种流程。', ('rock_crystal', 'illumination_powder'), check='手动确认：已开始培育宝石水晶簇', pages=('ATT_PERK_GEMS',))
    q('gem_harvest', '观察日夜对宝石的影响', 'perk_gem_day',
      '等待宝石簇成熟，生长时段影响烈阳、皎月和天空宝石类型。收获后观察它的增益，再选择适合自己的宝石；这里允许手动确认任一类型，避免强制刷齐三种。', check='手动确认：收获任意一种能力宝石并查看增益', pages=('ATT_PERK_GEMS',))
    q('gem_socket', '把宝石嵌入星能力', 'perk_gem_sky',
      '先分配宝石镶嵌槽星能力，再从背包选宝石镶入。移除会返还宝石。没有解锁槽位时，仅把宝石放在背包不代表获得其增益。', check='手动确认：已解锁一个宝石槽并装备宝石', pages=('ATT_PERK_GEMS',))
    groups['ritual'] = ('ritual', [['pattern', 'self_attune', 'reset'], ['attuned', 'pedestal', 'balance'], ['gem_seed', 'gem_harvest', 'gem_socket']])

    q('constellation', 'E 星座：天辉祭坛', 'altar_constellation',
      '用 C 区的星辉锭和星尘把二阶祭坛升级为天辉祭坛。结构外缘为 11×11，并有更高的柱；按宝典图逐层核对。\n完成升级进入星座研究，四条支线分别为注入、天辉晶石、光学与应用。', ('altar_constellation',), pages=('INFUSER', 'COLL_CRYSTAL'))
    q('infuser', '星能注入器', 'infuser',
      '按结构图搭建注入器，准备周围所需星能液池。它依赖星能液而不要求露天；放入原料后用共振星杖启动。反应损耗液体后先补齐再开下一次。', ('infuser',), pages=('INFUSER',))
    q('resonating', '海蓝宝石变共振宝石', 'resonating_gem',
      '指定 JAR 的注入配方将海蓝宝石加工为共振宝石。它用于聚能水晶、四阶祭坛及后续设备；不要把普通海蓝宝石当作同一种物品。', ('resonating_gem',), pages=('INFUSER',))
    q('altar4', '五彩祭坛：最后一级合成祭坛', 'altar_radiance',
      '先完成天辉水晶支线。四阶升级配方明确要求天辉级晶石，同时需要共振宝石、玻璃透镜、精致及炭黑大理石。\n支持结构继承三阶并增加顶部大理石砖；按宝典图施工，不要把新增顶部砖填成实心屋顶。升级后进入 F 区。', ('altar_radiance',), pages=('ALTAR4',))
    q('celestial', '从星尘培育天辉水晶', 'celestial_crystal',
      '把水晶石与星尘投入世界中的星能液形成天辉水晶簇。让其接收星光并经历各生长阶段，成熟发光后再收获。\n手册建议在星辉矿石上生长；加速会让矿石退回铁矿石，可用聚能水晶再次转化。任务检测收获的晶石，不要求挖下生长中的簇。', ('celestial_crystal',), pages=('CEL_CRYSTAL_GROW', 'CEL_CRYSTALS'))
    q('collectors', '可移动的聚能水晶', 'rock_collector_crystal',
      '先在共鸣祭坛调谐晶石，再结合共振宝石等材料制作聚能水晶。制作后品质不可再改，先选好晶石属性。不要密集堆放，采集区相互影响会降低收益。\n这里分别检测普通与天辉聚能水晶；天辉版本需要共鸣天辉晶石。它们不同于受保护的学院水晶。', ('rock_collector_crystal', 'celestial_collector_crystal'), pages=('COLL_CRYSTAL',))
    q('enhanced', '让天辉聚能更平稳', 'celestial_collector_crystal',
      '按宝典搭建增强天辉聚能水晶的支持结构。增强结构平滑星座显隐造成的供能波动，不是凭空增加所有时段的最大输出；它有助于在星座未出现时维持工作。', check='手动确认：已搭建增强聚能结构并检查露天与光路', pages=('ENHANCED_COLLECTOR',))
    q('colored', '彩色透镜与幽芒', 'colored_lens_fire',
      '将彩色镜片装到水晶透镜上，可改变光束效果；燃烧镜片用于加热，幽芒镜片允许星光穿过实体方块但有损耗。经过另一普通透镜或棱镜后不会自动保留颜色效果。', ('colored_lens_fire', 'colored_lens_spectral'), pages=('LENSES_EFFECTS', 'IGNITION_LENS', 'SPECTRAL_LENS'))
    q('prism', '一束星光，多条去路', 'prism',
      '水晶棱镜把接收的星能均匀分配到多个目标。普通水晶透镜是单目标；分流不是复制能量，仍需合理安排源和负载。', ('prism',), pages=('PRISM',))
    q('refraction', '群星映射与注星玻璃', 'refraction_table',
      '准备映射台、羊皮纸与注星玻璃。按手册描绘同时在天空中的星座，最多使用三个；重复同一个星座不会带来重复增益。\n玻璃上各星座都在天空时，才能用它处理未附魔装备或书。普通注星玻璃目标不会验证已经蚀刻的 NBT。', ('refraction_table', 'infused_glass'), pages=('DRAWING_TABLE',))
    q('tools', '注能工具与辉光星杖', 'infused_crystal_pickaxe',
      '在注入器中处理水晶石镐可得到充能镐；工具效果消耗个人充能并有冷却。另制作辉光星杖用于放置光源，也可为洞穴照明器临时供能。', ('infused_crystal_pickaxe', 'illumination_wand'), pages=('CHARGED_TOOLS', 'ILLUMINATION_WAND'))
    q('anchor', '把仪式移到需要的地方', 'ritual_link',
      '制作一对仪式锚。按手册，在基座正上方五格放一个锚，用连接器连到同维度的另一锚，把效果中心转移过去。先完成 D 区仪式本体；锚不代替基座。', ('ritual_link',), check='手动确认：已阅读成对仪式锚的连接与高度要求', pages=('RITUAL_LINK',))
    q('tree', '树木信标的再现之影', 'tree_beacon',
      '信标捕获附近树苗成长时的树木精华，随后产出资源。再现之影寿命有限，最终仍要补栽树苗。额外星能可加快生产，手册记录生息座更有利。', ('tree_beacon',), pages=('TREEBEACON',))
    groups['constellation'] = ('constellation', [['infuser', 'resonating', 'altar4'], ['celestial', 'collectors', 'enhanced'], ['colored', 'prism', 'refraction'], ['tools', 'anchor', 'tree']])

    q('radiance', 'F 五彩：毕业后的研究', 'altar_radiance',
      '四阶五彩祭坛是本工件实际注册的最高合成祭坛。先完成 E 区，确认结构与供能，再研究聚焦配方、观星台和万象泉。\n持有祭坛物品不自动授予研究；仍要完成模组自身升级流程。', ('altar_radiance',), pages=('CRAFTING_FOCUS_HINT',))
    q('focus', '聚焦晶石与转继供料', 'spectral_relay',
      '需要星座聚焦的配方，应在祭坛放入匹配星座的共鸣晶石；外围转继器按合成提示供应材料。查看配方背景的聚焦星座与绕在网格外的原料。\n手册说明此级祭坛星能不足时会暂停等待恢复，不再直接取消合成。', ('spectral_relay', 'attuned_rock_crystal'), check='手动确认：已按配方检查聚焦星座和外围供料', pages=('CRAFTING_FOCUS_HINT',))
    q('observatory', '观星台与朦胧星座', 'observatory',
      '观星台配方需要圣芒座（Lucerna）聚焦以及外围原料。先通过个体共鸣与天文望远镜解决暗淡星座知识，再准备对应共鸣晶石。\n观星台可见朦胧星座；移动鼠标瞄准，Shift 锁定视角后描绘。右击或 Esc 离开视图。', ('observatory',), pages=('OBSERVATORY',))
    q('trait', '为共鸣晶石附加特质', 'attuned_celestial_crystal',
      '朦胧星座不足以作为普通仪式的主星座，却可为已经共鸣的晶石附加一个特质。按其图形布置共鸣祭坛，并等待有效夜晚。\n手册说明特质不能随意更换；先查星座页的效果再应用。普通物品 ID 不能检测特质，本项采用手动确认。', check='手动确认：已在共鸣晶石上应用并核对一种特质', pages=('ATT_TRAIT',))
    q('chalice', '纳星圣杯', 'chalice',
      '圣杯为聚星缸提供魔法储存与转运。手册记录容量 64 桶，可从视线通畅的 16 格内聚星缸抽液；移动前应先清空。红石可抑制其周围流体交互效果。', ('chalice',), pages=('C_CHALICE',))
    q('fountain', '万象泉与支持结构', 'fountain',
      '搭建万象泉结构，直接在上方放圣杯供给星能液。它靠内部液体工作，不需要露天；还要安装所需引口才有具体作用。结构内部不要存放重要方块。', ('fountain',), pages=('BORE_CORE',))
    q('primes', '液体与漩涡引口', 'fountain_prime_liquid',
      '纳耳狂曼引口用于抽取地下流体精华，配方聚焦南极座（Octans）；准备视线内空圣杯接收，并先勘察位置。其钻孔会破坏下方方块。\n费萨利德引口用于束缚附近生物并持续消耗星能液。只讨论 JAR 有配方且手册有页的这两种引口，不要求无配方的矿物引口。', ('fountain_prime_liquid', 'fountain_prime_vortex'), pages=('BORE_HEAD_LIQUID', 'BORE_HEAD_VORTEX', 'ICHOSIC'))
    q('mantle', '星斗披风', 'mantle',
      '制作星斗披风。它通过专门的五彩祭坛配方调谐，不是扔到共鸣祭坛上。先保证满耐久，再查对应星座页的调谐配方。', ('mantle',), pages=('ATT_CAPE',))
    q('mantle_align', '给披风选择星座', 'mantle',
      '按对应配方调谐满耐久披风。披风一次只对应一个星座，再用另一星座配方会改换调谐；星尘可作铁砧维修材料。\n这是 NBT 相关效果，任务以手动确认而非普通披风物品检测替代。', check='手动确认：已通过专门配方调谐披风', pages=('ATT_CAPE',))
    q('irradiant', '灿芒之星：保留成长的重分配', 'shifting_star_aevitas',
      '以生息座灿芒之星为例，配方需要生息座聚焦。与普通更替之星不同，它会变换共鸣起点、退回已分配点数，但保留成长进度。任务只要求制作，是否使用由你决定。', ('shifting_star_aevitas',), pages=('ENH_SHIFTING_STAR',))
    q('remake', '补制星图', 'constellation_paper',
      '五彩阶段可用配方补制星图，例如生息座星图。普通星图物品检测不会区分星座或来源，因此这里只要求手动确认确实使用配方补制。', check='手动确认：已在五彩祭坛补制一张星图', pages=('CRAFTING_FOCUS_HINT',))
    q('review', '重新检查你的星能网络', 'linking_tool',
      '逐项检查：源与目标星座是否兼容，天空与光路有无遮挡，多余连接是否均分能量，聚能源是否堆放过密，祭坛外围供料是否齐备。对照宝典对应页排查。', check='手动确认：已完成一次基地星能网络巡检', pages=('CRAFTING_FOCUS_HINT',))
    q('graduate', '辉煌：当前手册的边界', 'tome',
      '祝贺你完成本章的五个实际研究阶段。源码虽保留 BRILLIANCE（辉煌）枚举，当前 RegistryResearch 没有注册其研究页面，也没有可要求制作的第五级祭坛。\n本项仅为阅读结业，不宣称自动授予“辉煌”研究。可以继续优化晶石、光学网络与各星座应用。', check='手动确认：已理解现有阶段和辉煌的实现边界', pages=('CRAFTING_FOCUS_HINT',))
    groups['radiance'] = ('radiance', [['focus', 'observatory', 'trait'], ['chalice', 'fountain', 'primes'], ['mantle', 'mantle_align', 'irradiant'], ['remake', 'review', 'graduate']])
    return groups


def layout(groups):
    """Longest-path layers, deterministic barycenter sweeps, then panel folding."""
    bykey = {q['key']: q for q in Q}
    assert len(bykey) == len(Q)
    positions, deps, panel_of = {}, {}, {}
    for panel, _, _, (ox, oy), mirror in PANELS:
        root, branches = groups[panel]
        nodes = [root] + [n for branch in branches for n in branch]
        for n in nodes:
            if n in panel_of:
                raise ValueError('Node in multiple panels')
            panel_of[n] = panel
        deps[root] = []
        for branch in branches:
            previous = root
            for n in branch:
                deps[n] = [previous]
                previous = n
        levels = {root: 0}
        for n in nodes[1:]:
            levels[n] = 1 + max(levels[d] for d in deps[n])
        layers = {level: [n for n in nodes if levels[n] == level] for level in range(4)}
        for _ in range(4):
            for level in (1, 2, 3, 2, 1):
                down = level in (1, 2, 3)
                adjacent = layers[level-1]
                ranks = {n: i for i, n in enumerate(adjacent)}
                layers[level].sort(key=lambda n: (sum(ranks[d] for d in deps[n])/len(deps[n]), n))
            # Reverse sweep: successor barycenters preserve continuous branches.
            for level in (2, 1):
                ranks = {n: i for i, n in enumerate(layers[level+1])}
                layers[level].sort(key=lambda n: (sum(ranks[c] for c in ranks if n in deps[c]), n))
        for level, layer in layers.items():
            for lane, n in enumerate(layer):
                local_x = level * 3
                x = ox + (9-local_x if mirror else local_x)
                y = oy + (4.5 if level == 0 else (1.5 if len(layer) == 3 else 0) + lane*3)
                positions[n] = (x, y)
    if set(positions) != set(bykey):
        raise ValueError('Unplaced nodes')
    return positions, deps, panel_of


def geometry(positions, deps):
    half = .45
    def orientation(a, b, c):
        return (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0])
    def on(a, b, p):
        return abs(orientation(a,b,p)) < 1e-8 and all(min(a[i], b[i])-1e-8 <= p[i] <= max(a[i], b[i])+1e-8 for i in (0,1))
    def intersect(a,b,c,d):
        o = [orientation(a,b,c), orientation(a,b,d), orientation(c,d,a), orientation(c,d,b)]
        return (o[0]*o[1] < 0 and o[2]*o[3] < 0) or on(a,b,c) or on(a,b,d) or on(c,d,a) or on(c,d,b)
    for a,b in itertools.combinations(positions, 2):
        if all(abs(positions[a][i]-positions[b][i]) < 2*half for i in (0,1)):
            raise ValueError('Node overlap: '+a+' '+b)
    edges = [(d,n) for n in deps for d in deps[n]]
    for a,b in edges:
        pa,pb = positions[a],positions[b]
        for n,(x,y) in positions.items():
            if n in (a,b):
                continue
            corners = [(x-half,y-half),(x+half,y-half),(x+half,y+half),(x-half,y+half)]
            if any(intersect(pa,pb,corners[i],corners[(i+1)%4]) for i in range(4)):
                raise ValueError('Edge hits node: '+a+' -> '+b+' / '+n)
    for (a,b),(c,d) in itertools.combinations(edges,2):
        shared = {a,b}&{c,d}
        if shared:
            origin = positions[next(iter(shared))]
            p = positions[next(iter({a,b}-shared))]
            r = positions[next(iter({c,d}-shared))]
            if abs(orientation(origin,p,r)) < 1e-8 and sum((p[i]-origin[i])*(r[i]-origin[i]) for i in (0,1)) > 0:
                raise ValueError('Collinear overlapping incident edges')
        elif intersect(positions[a],positions[b],positions[c],positions[d]):
            raise ValueError('Crossing edges')
    xs,ys = zip(*positions.values())
    width,height = max(xs)-min(xs),max(ys)-min(ys)
    longest = max(math.dist(positions[a],positions[b]) for a,b in edges)
    assert 4/3 <= (width+.9)/(height+.9) <= 16/9
    assert longest <= 5.5
    return dict(center_width=width, center_height=height, outer_width=width+.9,
                outer_height=height+.9, ratio=(width+.9)/(height+.9),
                longest_edge=longest, edges=len(edges), crossings=0, node_overlaps=0, edge_node_hits=0)


def scan():
    """Scan only FTBQ definition containers.

    A recursive walk over every ``id`` field is incorrect: item-stack NBT,
    enchantments and filters legitimately contain namespaced IDs such as
    ``minecraft:potion``.  Keep the parser recursive for syntax validation,
    but collect IDs only from the FTBQ object positions defined below.
    """
    definitions, chapters, snapshots = {}, [], {}
    problems = []

    def add_definition(obj, path, label):
        if not isinstance(obj, dict) or 'id' not in obj:
            return
        value = obj['id']
        # This guard is defensive for malformed libraries.  Namespaced IDs
        # are item/block/filter IDs, never FTBQ object IDs.
        if not isinstance(value, str) or ':' in value:
            return
        if not valid_id(value):
            problems.append('invalid object ID '+str(value)+' in '+path+' ('+label+')')
            return
        if value in definitions:
            problems.append('duplicate object ID '+value+': '+definitions[value]+' and '+path+' ('+label+')')
        else:
            definitions[value] = path+' ('+label+')'

    def collect_chapter(path, doc):
        add_definition(doc, path, 'chapter')
        for index, quest in enumerate(doc.get('quests', [])):
            add_definition(quest, path, f'quest[{index}]')
            for task_index, task in enumerate(quest.get('tasks', [])):
                add_definition(task, path, f'quest[{index}].task[{task_index}]')
            for reward_index, reward in enumerate(quest.get('rewards', [])):
                add_definition(reward, path, f'quest[{index}].reward[{reward_index}]')
        for index, link in enumerate(doc.get('quest_links', [])):
            add_definition(link, path, f'quest_link[{index}]')

    def collect_group_file(path, doc):
        for index, group in enumerate(doc.get('chapter_groups', [])):
            add_definition(group, path, f'chapter_group[{index}]')

    def collect_reward_table(path, doc):
        add_definition(doc, path, 'reward_table')
        for index, reward in enumerate(doc.get('rewards', [])):
            add_definition(reward, path, f'reward_table.reward[{index}]')

    for path in sorted(QUESTS.rglob('*.snbt')):
        raw = path.read_bytes()
        snapshots[str(path)] = digest(raw)
        if path == OUTPUT:
            continue
        parsed = SnbtParser(raw.decode('utf-8-sig')).parse()
        relative = path.relative_to(QUESTS).as_posix()
        if path.parent.name == 'chapters':
            chapters.append(parsed)
            collect_chapter(relative, parsed)
        elif relative == 'chapter_groups.snbt':
            collect_group_file(relative, parsed)
        elif path.parent.name == 'reward_tables':
            collect_reward_table(relative, parsed)
    group_data = SnbtParser((QUESTS/'chapter_groups.snbt').read_text(encoding='utf-8-sig')).parse()
    assert GROUP in [g['id'] for g in group_data['chapter_groups']]
    graph = {q['id']: q.get('dependencies', []) for c in chapters for q in c.get('quests', [])}
    dangling = [(n,d) for n in graph for d in graph[n] if d not in graph]
    problems += ['existing dangling dependency '+str(x) for x in dangling]
    return definitions, chapters, snapshots, problems


def class_strings(data):
    """Read JVM constant pool UTF8 entries, never infer registration from lang."""
    assert data[:4] == b'\xca\xfe\xba\xbe'
    count = struct.unpack_from('>H', data, 8)[0]
    i,offset,strings = 1,10,set()
    widths = {3:4,4:4,5:8,6:8,7:2,8:2,9:4,10:4,11:4,12:4,15:3,16:2,17:4,18:4,19:2,20:2}
    while i < count:
        tag = data[offset]; offset += 1
        if tag == 1:
            length = struct.unpack_from('>H',data,offset)[0]; offset += 2
            strings.add(data[offset:offset+length].decode('utf-8',errors='replace')); offset += length
        else:
            offset += widths[tag]
            if tag in (5,6): i += 1
        i += 1
    return strings


def verify_artifact(items):
    registry = {}
    for holder, pattern in [('ItemsAS',r'ITEMS\.register\("([a-z0-9_]+)"'),
                            ('BlocksAS',r'registerBlockWithItem\("([a-z0-9_]+)"'),
                            ('FluidsAS',r'ItemsAS\.ITEMS\.register\("([a-z0-9_]+)"')]:
        text = (JAVA/'lib'/f'{holder}.java').read_text(encoding='utf-8')
        text = re.sub(r'/\*[\s\S]*?\*/|//[^\n]*', '', text)
        for name in re.findall(pattern,text):
            registry['astralsorcery:'+name] = holder
    with zipfile.ZipFile(JAR) as z:
        entries = set(z.namelist())
        strings = {h:class_strings(z.read('hellfirepvp/astralsorcery/common/lib/'+h+'.class')) for h in ('ItemsAS','BlocksAS','FluidsAS')}
        en = json.loads(z.read('assets/astralsorcery/lang/en_us.json'))
        zh = json.loads(z.read('assets/astralsorcery/lang/zh_cn.json'))
        research = (JAVA/'registry/RegistryResearch.java').read_text(encoding='utf-8')
        assert 'registerBrilliance' not in research
        for item in items:
            if item not in registry:
                raise ValueError('Item not explicitly registered: '+item)
            name = item.split(':')[1]
            if name not in strings[registry[item]]:
                raise ValueError('Registration name absent in JAR class: '+item)
            if f'assets/astralsorcery/models/item/{name}.json' not in entries:
                raise ValueError('Item model absent in JAR: '+item)
        for quest in Q:
            for page in quest['pages']:
                if '"'+page+'"' not in research or 'astralsorcery.journal.node.'+page+'.name' not in en:
                    raise ValueError('Unregistered research page '+page)
        # Source/JAR parity for the manual and key recipes, not stale handoff MD.
        resources = ['assets/astralsorcery/lang/en_us.json','assets/astralsorcery/lang/zh_cn.json']
        resources += ['data/astralsorcery/recipes/'+r+'.json' for r in (
            'altar/altar_attunement','altar/altar_constellation','altar/altar_radiance',
            'altar/observatory','altar/fountain_prime_liquid','altar/mantle_aevitas',
            'block_transmutation/craftingtable_altar','block_transmutation/iron_starmetal','infuser/aquamarine')]
        for r in resources:
            if json.loads(z.read(r)) != json.loads((SOURCE/'src/main/resources'/r).read_text(encoding='utf-8')):
                raise ValueError('Source/JAR resource divergence: '+r)
        recipes = {n:json.loads(z.read(n)) for n in entries if n.startswith('data/astralsorcery/recipes/') and n.endswith('.json')}
        assert recipes['data/astralsorcery/recipes/altar/altar_radiance.json']['key']['E']['hasToBeCelestial']
        assert recipes['data/astralsorcery/recipes/altar/observatory.json']['focus_constellation'] == 'astralsorcery:lucerna'
        assert recipes['data/astralsorcery/recipes/altar/fountain_prime_liquid.json']['focus_constellation'] == 'astralsorcery:octans'
    return dict(jar_sha256=digest(JAR.read_bytes()), registered_item_count=len(registry), checked_items=len(items),
                recipe_files=len(recipes), checked_resource_parity=len(resources))


def keys_for(groups):
    roots = {v[0] for v in groups.values()}
    keys = ['chapter']
    for quest in Q:
        k = quest['key']
        keys.append('quest/'+k)
        keys.extend('task/'+k+'/item/'+i for i in quest['items'])
        if quest['check']:
            keys.append('task/'+k+'/check')
        if k in roots:
            keys.append('reward/'+k+'/item/astralsorcery:aquamarine')
    assert len(keys) == len(set(keys))
    return keys


def allocate(old, keys, external):
    ids = dict(old.get('ids', {}))  # Keep retired keys reserved forever.
    assert len(ids.values()) == len(set(ids.values()))
    for k,v in ids.items():
        if not valid_id(v) or v != stable_id(k):
            raise ValueError('Persisted semantic ID differs: '+k)
        if v in external:
            raise ValueError('Persisted ID collision: '+k)
    reserved = set(ids.values())|set(external)
    for k in sorted(keys):
        if k not in ids:
            candidate = stable_id(k)
            if candidate in reserved:
                raise ValueError('ID hash collision: '+k)
            ids[k] = candidate
            reserved.add(candidate)
    return ids


def model(ids, order, groups, positions, deps, panel_of):
    roots = {v[0] for v in groups.values()}
    doc = dict(default_hide_dependency_lines=False, default_quest_shape='', filename='astral_sorcery',
               group=GROUP, icon='astralsorcery:tome', id=ids['chapter'], order_index=order, quest_links=[], quests=[])
    for spec in Q:
        k = spec['key']
        tasks = [dict(id=ids['task/'+k+'/item/'+item], type='item', item=item, consume_items=False) for item in spec['items']]
        if spec['check']:
            tasks.append(dict(id=ids['task/'+k+'/check'], type='checkmark', title=spec['check']))
        panel = next(p for p in PANELS if p[0] == panel_of[k])
        description = spec['description'] + ['', '手册阶段：'+panel[1]+'；参照页面：'+ '、'.join(spec['pages'])+'。']
        node = dict(id=ids['quest/'+k], title=spec['title'], icon=spec['icon'], description=description,
                    x=float(positions[k][0]), y=float(positions[k][1]), size=.9,
                    dependencies=[ids['quest/'+d] for d in deps[k]], tasks=tasks, rewards=[])
        if k in roots:
            node['rewards'] = [dict(id=ids['reward/'+k+'/item/astralsorcery:aquamarine'], type='item', item='astralsorcery:aquamarine', count=1)]
        doc['quests'].append(node)
    return doc


def validate(doc, ids, external):
    definitions = [o['id'] for o in walk(doc) if 'id' in o]
    if len(definitions) != len(set(definitions)) or not all(valid_id(i) for i in definitions):
        raise ValueError('Invalid or duplicate IDs')
    if set(definitions)&set(external):
        raise ValueError('Global ID collision')
    if doc['id'] != ids['chapter']:
        raise ValueError('Chapter identity drift')
    if len(doc['quests']) != len(Q):
        raise ValueError('Quest loss')
    qids = {q['id'] for q in doc['quests']}
    graph = {q['id']:q['dependencies'] for q in doc['quests']}
    active,seen = set(),set()
    def visit(n):
        if n in active:
            raise ValueError('Dependency cycle')
        if n in seen:
            return
        active.add(n)
        for d in graph[n]:
            if d not in qids:
                raise ValueError('Dependency points outside quests')
            visit(d)
        active.remove(n); seen.add(n)
    for n in graph:
        visit(n)
    for spec,node in zip(Q,doc['quests']):
        k = spec['key']
        if node['id'] != ids['quest/'+k]:
            raise ValueError('Semantic quest identity drift')
        expected = [('item',i) for i in spec['items']] + ([('checkmark',None)] if spec['check'] else [])
        actual = [(t['type'],t.get('item')) for t in node['tasks']]
        if not expected or actual != expected:
            raise ValueError('Missing/reordered targets for '+k)
        for target in node['tasks']:
            suffix = 'item/'+target['item'] if target['type'] == 'item' else 'check'
            if target['id'] != ids['task/'+k+'/'+suffix]:
                raise ValueError('Semantic target identity drift')
    geometry({q['id']:(q['x'],q['y']) for q in doc['quests']},graph)
    text = dump(doc)+'\n'
    if SnbtParser(text).parse() != doc or nbtlib.parse_nbt(text).unpack() != doc:
        raise ValueError('Independent SNBT round-trip mismatch')
    return text.encode('utf-8')


def self_test(doc, ids, external, positions, deps, groups):
    def fails(action):
        try:
            action()
        except (ValueError, AssertionError):
            return
        raise AssertionError('Negative validation accepted invalid input')
    bad = copy.deepcopy(doc); bad['quests'][0]['dependencies'] = [bad['quests'][0]['id']]
    fails(lambda: validate(bad,ids,external))
    bad = copy.deepcopy(doc); bad['quests'][0]['dependencies'] = [bad['quests'][0]['tasks'][0]['id']]
    fails(lambda: validate(bad,ids,external))
    bad = copy.deepcopy(doc); n = next(q for q in bad['quests'] if len(q['tasks']) > 1); n['tasks'].pop()
    fails(lambda: validate(bad,ids,external))
    bad = copy.deepcopy(doc); bad['quests'][0]['id'] = '8000000000000000'
    fails(lambda: validate(bad,ids,external))
    fails(lambda: validate(doc,ids,{doc['id']:['collision.snbt']}))
    badpos = dict(positions); names = list(badpos); badpos[names[1]] = badpos[names[0]]
    fails(lambda: geometry(badpos,deps))
    fails(lambda: geometry({'a':(0,0),'b':(6,0),'c':(3,0)}, {'a':[],'b':['a'],'c':[]}))
    fails(lambda: geometry({'a':(0,0),'b':(4,4),'c':(0,4),'d':(4,0)}, {'a':[],'b':['a'],'c':[],'d':['c']}))
    keys = keys_for(groups)
    assert allocate({'ids':ids}, list(reversed(keys))+['task/future/item/astralsorcery:tome'], external) | ids == allocate({'ids':ids}, keys+['task/future/item/astralsorcery:tome'], external)
    return 9


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--check',action='store_true')
    ap.add_argument('--self-test',action='store_true')
    args = ap.parse_args()
    groups = content()
    positions,deps,panel_of = layout(groups)
    geom = geometry(positions,deps)
    external,chapters,snapshots,problems = scan()
    old_map_raw = MAP.read_bytes() if MAP.exists() else None
    old_output_raw = OUTPUT.read_bytes() if OUTPUT.exists() else None
    old = json.loads(old_map_raw) if old_map_raw else {}
    if old_output_raw is not None and not old:
        raise ValueError('Existing chapter without mapping: refusing takeover')
    if old:
        if old.get('salt') != SALT or old.get('group') != GROUP:
            raise ValueError('Mapping ownership mismatch')
        if old_output_raw is None or digest(old_output_raw) != old.get('output_sha256'):
            raise ValueError('Published chapter was changed; refusing to overwrite manual work')
        previous = SnbtParser(old_output_raw.decode('utf-8')).parse()
        if previous['id'] != old['ids']['chapter']:
            raise ValueError('Published chapter ID changed')
    orders = [int(c.get('order_index',0)) for c in chapters if c.get('group') == GROUP]
    order = old['order_index'] if old else max(orders,default=-1)+1
    if order in orders:
        raise ValueError('Chosen group/order now occupied')
    ids = allocate(old,keys_for(groups),external)
    doc = model(ids,order,groups,positions,deps,panel_of)
    payload = validate(doc,ids,external)
    items = {o[f] for o in walk(doc) for f in ('item','icon') if f in o}
    plan = (TOOLS/'astral_sorcery_quest_plan.md').read_text(encoding='utf-8')
    items.update(re.findall(r'\|\s*(astralsorcery:[a-z0-9_]+)\s*\|',plan))
    artifact = verify_artifact(items)
    data = dict(schema=1,salt=SALT,group=GROUP,order_index=order,ids=dict(sorted(ids.items())),
                output_sha256=digest(payload),artifact=artifact)
    map_payload = (json.dumps(data,ensure_ascii=False,indent=2,sort_keys=True)+'\n').encode('utf-8')
    tests = self_test(doc,ids,external,positions,deps,groups) if args.self_test else 0
    # Re-scan immediately before publishing: another agent may have added a chapter.
    external2,chapters2,snapshots2,problems2 = scan()
    if snapshots2 != snapshots or (MAP.read_bytes() if MAP.exists() else None) != old_map_raw:
        raise ValueError('Concurrent file change detected; rerun after reviewing')
    validate(doc,ids,external2)
    if args.check:
        if old_output_raw != payload or old_map_raw != map_payload:
            raise ValueError('Published output differs from current generator; run generation after review')
    else:
        # No broad staging/temp paths: all validation precedes the two allowlisted
        # writes. Incomplete publication fails closed on the next run via SHA.
        if old_output_raw != payload:
            OUTPUT.write_bytes(payload)
        if old_map_raw != map_payload:
            MAP.write_bytes(map_payload)
        if OUTPUT.read_bytes() != payload or MAP.read_bytes() != map_payload:
            raise IOError('Post-write verification failed')
    report = dict(quests=len(Q),targets=sum(len(q['tasks']) for q in doc['quests']),
                  multi_target_quests=sum(len(q['tasks'])>1 for q in doc['quests']),
                  checkmarks=sum(t['type']=='checkmark' for q in doc['quests'] for t in q['tasks']),
                  rewards=sum(len(q['rewards']) for q in doc['quests']),ids=len(keys_for(groups)),
                  group=GROUP,order_index=order,layout=geom,artifact=artifact,
                  scanned_snbt=len(snapshots2),existing_library_issues=problems2,
                  self_tests=tests,chapter_sha256=digest(payload),mapping_sha256=digest(map_payload),
                  mode='check' if args.check else 'write',
                  stage_quest_counts={p[0]:sum(panel_of[q['key']]==p[0] for q in Q) for p in PANELS})
    print(json.dumps(report,ensure_ascii=False,indent=2))


if __name__ == '__main__':
    main()
