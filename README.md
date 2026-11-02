<p align="center">
  <img src="src/main/resources/assets/projecte/logo.png" alt="ProjectEF Neo Logo" width="220">
</p>
<h1 align="center">ProjectEF Neo</h1>
<p align="center">
  <b>Equivalent Exchange: Reborn for Fabric</b><br>
  中文名：等价交换Neo<br>
  本分支为 <b>Minecraft 26.3</b> 移植版
</p>
<p align="center">
<!-- <a href="https://modrinth.com/mod/projectef">
<img src="https://img.shields.io/modrinth/dt/projectef?logo=modrinth&label=Modrinth%20Downloads&color=00AF5C" alt="Modrinth Downloads">
</a> -->
<a href="https://github.com/SpacJoy/ProjectEF/releases">
<img src="https://img.shields.io/github/v/release/SpacJoy/ProjectEF?logo=github&label=Latest%20Release" alt="Latest Release">
</a>
<a href="https://github.com/SpacJoy/ProjectEF/blob/26.3/LICENSE">
<img src="https://img.shields.io/github/license/SpacJoy/ProjectEF?label=License" alt="License">
</a>
<a href="https://github.com/deepseek-ai/deepseek-harness">
<img src="https://img.shields.io/badge/powered_by-dsh-4D6BFE?style=flat-square&logo=deepseek&logoColor=white" alt="powered by dsh">
</a>

</p>

## Quick Links

[Download on CurseForge](https://www.curseforge.com/minecraft/mc-mods/projectef) · [GitHub Releases](https://github.com/wchiway/ProjectEF/releases) · [Installation](#installation) · [Report an Issue](https://github.com/wchiway/ProjectEF/issues) · [Developer Guide](DEV.md)

> **本分支说明（Minecraft 26.3）**
>
> 当前分支是 **Minecraft 26.3** 的移植版，由 [SpacJoy](https://github.com/SpacJoy/ProjectEF) 在 [wchiway/ProjectEF](https://github.com/wchiway/ProjectEF) 基础上移植而来。
> 1.21.1 主线的玩法与用法仍然适用，差异只在下面列出的部分。

## 本分支相对上游的改动

### 一、移植到 Minecraft 26.3

26.3 改动了很多底层约定，以下位置与 1.21.1 分支不同：

| 项目 | 1.21.1 | 26.3 |
| --- | --- | --- |
| 物品模型入口 | `assets/<ns>/models/item/*.json` + `overrides` | `assets/<ns>/items/*.json`（`overrides` 已移除，改用 `select` / `condition` / `range_dispatch`） |
| 盔甲外观 | `textures/models/armor/*_layer_1/2.png` | `assets/<ns>/equipment/<name>.json` + `textures/entity/equipment/<layerType>/<name>.png` |
| 饰品槽图标 | 放在任意图集位置 | 必须在 `textures/gui/sprites/`（`blitSprite` 只查 GUI 图集） |
| GUI 分层 | 按绘制调用顺序 | `GuiRenderState` 按包围盒分层，需要置顶时调 `nextStratum()` |
| 数据组件读取 | `Registry#getOrThrow` | `Registry#getValueOrThrow`（`getOrThrow` 返回的是 Holder） |
| 方块物品翻译键 | 默认 `block.<ns>.<path>` | 默认 `item.<ns>.<path>`，需调 `useBlockDescriptionPrefix()` 切回 |
| access widener | `*.accesswidener` | `*.classtweaker` |
| 构建 | Loom 1.10.x / Gradle 8.12.1 / JDK 21 | Loom 1.18.2（`net.fabricmc.fabric-loom`）/ Gradle 9.8.0 / JDK 25 |
| 映射 | Mojang official + Parchment | 26.1 起不再混淆，无需映射 |

配方与整个世界转化表的数据格式也变了，JSON 里引用物品要写字符串而不是 `{"item": "..."}` 对象。

### 二、本分支额外新增的内容

这些是 1.21.1 分支没有、只在本分支有的功能：

**1. 「非卖品」附魔**

新增附魔 `projecte:unsellable`（中文名「非卖品」），带这个附魔的物品无法出售到转换桌，也就在转换桌里换不成 EMC。适合给不想误卖的贵重物品挂上。

实现位置：`data/projecte/enchantment/unsellable.json` 定义附魔，`utils/impl/SellProxyImpl.java` 做判断，`TransmutationContainer` / `SlotConsume` / `ToolTipEvent` 三处接入。

**2. 公开的 `ISellProxy` API**

新增 `moze_intel.projecte.api.proxy.ISellProxy`，其他模组可以通过它查询「某个物品是否可出售」，方便自己在界面里做同样的灰显或提示。

**3. 装备可附魔**

暗物质、红物质、宝石三套装备原本被强制设为不可附魔，本分支解除了这个限制：
- 移除 `PEArmor` / `PETool` 上的附魔屏蔽
- 重写 `enchantable/*` 物品标签，让护甲走护甲附魔池、锤 / 拳剑 / 钉头锤走武器附魔池
- 通过 `mixin/ItemMixin.java` 恢复装备在附魔台的附魔能力
- 同时屏蔽耐久类附魔（耐久、经验修补这类对无限耐久的暗物质装备无意义）

**4. 其他修复**

- EMC 单位按游戏语言显示（中文为「兆」「京」「垓」等中文数量级，不是 Quintillion 这类英文后缀）
- EMC 每秒速率显示修正（之前是实际值的两倍）
- EMC 文字在深色 GUI 背景下可读性修正
- 奥术转换桌的数量文字不再被物品图标盖住

### 三、暂未移植的部分

- **EMI 集成**：EMI 没有 26.3 版本，源码保留在树里但排除在编译外。JEI 和 REI 正常。
- **其他可选兼容**：Avaritia、AMEcs 等依赖没有 26.3 版本，运行时会静默跳过。

## Overview

**ProjectEF Neo** is a modern Fabric port of the classic **ProjectE** mod — the complete **Equivalent Exchange** experience on Minecraft 1.21.1.

The **"F"** stands for **Fabric**. ProjectEF Neo keeps the classic **EMC (Energy-Matter Covalence)** gameplay intact:

* Convert items into EMC and permanently learn their value.
* Recreate learned items anytime through the Transmutation Table or the portable Transmutation Tablet.
* Automate EMC production with Collectors, Relays, and Condensers.
* Progress from the Philosopher's Stone to Dark Matter / Red Matter tools and GEM armor.

Existing ProjectE configuration files, resource paths, datapacks, and world data remain compatible wherever possible.

## Features

### EMC & Transmutation

* Item ↔ EMC conversion with permanent item knowledge.
* Transmutation Table and portable Transmutation Tablet.
* Configurable custom EMC values (`custom_emc.json`, in-game EMC Manager, or commands).
* Press **F8** (rebindable in Controls) or use **EMC Manager** in the pause menu. Search by item name/ID or select your held item's base type, then save, remove, or reset its EMC. Click **Apply** to recalculate and synchronize all saved changes without `/reload`.
* Editing requires the corresponding ProjectE `set_emc`, `remove_emc`, or `reset_emc` command permission (OP level 2 by default); applying requires all three. Remove saves a zero override; Reset removes the base-item override. Tags and component-specific variants are not edited by this screen.

### EMC Generation & Automation

* Energy Collectors (MK1–MK3)
* Anti-Matter Relays (MK1–MK3)
* Energy Condensers (MK1/MK2)
* Dark Matter Pedestal

### Equipment & Utility Items

* Philosopher's Stone — block transmutation and world conversion.
* Dark Matter and Red Matter tools with configurable area mining.
* Dark Matter Furnace and Red Matter Furnace.
* Alchemical Bags and Eternal Density gems.
* Swiftwolf's Rending Gale.
* Full GEM Armor abilities: night vision, step assist, flight, and combat effects.

### Trinkets Integration

Accessory slots are provided through [Trinkets](https://modrinth.com/mod/trinkets): equip rings, amulets, charms, and Klein Stars in the `hand/ring` and `chest/necklace` slots. ProjectEF Neo works without Trinkets — accessories simply behave as regular inventory items.

### Compatibility & Localization

* Built-in EMC values for classic Avaritia items are **disabled by default**. To opt in, set `enableAvaritiaEMC = true` at the top level of `config/ProjectE/mapping.toml`, then restart the server or reload the world. If `usePregenerated` is enabled, disable it first so EMC values are recalculated. This switch does not block custom EMC values or values derived from recipes.
* AvaritiaNeo Fabric: automatically enables Dark Matter and Red Matter Singularities, with compressor recipes consuming 200 corresponding ProjectE matter blocks. Item Alchemy is not required.
* Full Simplified Chinese and English localization.
* Compatible with original ProjectE configuration format and mod ID (`projecte`).

## Installation

### Requirements

当前分支（26.3）：

* Minecraft 26.3
* Fabric Loader 0.19.5 or newer
* Fabric API 0.165.0+26.3
* Java 25

（1.21.1 主线分支的要求是 Minecraft 1.21.1、Fabric Loader 0.16.9+、Java 21。）

> Forge Config API Port and fabric-permissions-api are bundled inside the ProjectEF Neo JAR (jar-in-jar). **No extra downloads needed.**

### Steps

1. Install Minecraft 26.3 with Fabric Loader.
2. Install a compatible Fabric API version.
3. Download ProjectEF Neo from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/projectef) or [GitHub Releases](https://github.com/SpacJoy/ProjectEF/releases).
4. Place the JAR file into your Minecraft `mods` folder.

### Optional Integrations

These mods only enhance the experience and are never required:

* JEI / EMI / REI — recipe viewers
* Jade / WTHIT — block information overlays. With Jade installed, ProjectEF Neo registers its own EMC provider through Jade's Fabric plugin entrypoint; no Jade patch is required. Blocks with an EMC value display it when looked at. Keep `misc.lookingAtDisplay` enabled in ProjectE's `server.toml` and **EMC Provider** (`projecte:emc_provider`) enabled in Jade's plugin settings.
* Trinkets — accessory slots

## Compatibility Notes

ProjectEF Neo keeps the original ProjectE mod identifier:

```
projecte
```

This allows compatibility with:

* Existing configuration files
* Existing resource paths
* Existing world data

The following integrations from the NeoForge version are not included:

* CraftTweaker
* The One Probe
* Curios (replaced by Trinkets on Fabric)

## Reporting Issues

Please [open an issue](https://github.com/wchiway/ProjectEF/issues) and include:

* ProjectEF Neo version
* Minecraft version
* Fabric Loader version
* Fabric API version
* Steps to reproduce
* Relevant game logs

Please attach logs as files or external paste links instead of posting complete logs directly in the issue.

## Development

For source builds, development runs, and source-set status, see the [Developer Guide](DEV.md). Architecture notes, examples, and historical material are listed in the [documentation index](docs/README.md).

## Maintainer

**Chiway Wang**

Fabric port developer and maintainer.

## Credits

ProjectEF Neo is based on the original **ProjectE** project.

Special thanks to:

* ProjectE developers and contributors
* EE2 original creators
* Minecraft modding community

Original contributors include:

* SinKillerJ
* pupnewfster
* MaPePeR
* williewillus
* Lilylicious
* MozeIntel
* Kolatra

Additional credits:

* x3n0ph0b3 — EE2 creator and original asset permissions
* MidnightLightning — EE2 GUI textures

## License

ProjectEF Neo is released under the MIT License.
