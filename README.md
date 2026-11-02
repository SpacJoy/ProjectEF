<p align="center">
  <img src="src/main/resources/assets/projecte/logo.png" alt="ProjectEF Neo Logo" width="220">
</p>
<h1 align="center">ProjectEF Neo</h1>
<p align="center">
  <b>等价交换：Fabric 重制版</b><br>
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

## 快速链接

[CurseForge 下载](https://www.curseforge.com/minecraft/mc-mods/projectef) · [GitHub Releases](https://github.com/wchiway/ProjectEF/releases) · [安装说明](#安装) · [反馈问题](https://github.com/wchiway/ProjectEF/issues) · [开发指南](DEV.md)

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

## 概述

**ProjectEF Neo** 是经典模组 **ProjectE** 的现代化 Fabric 移植版，把完整的 **等价交换（Equivalent Exchange）** 体验带到了 Minecraft 上。

名字里的 **「F」** 代表 **Fabric**。ProjectEF Neo 完整保留了经典的 **EMC（能量-物质等价）** 玩法：

* 把物品转换成 EMC，并永久记录该物品的价值。
* 随时通过转换桌或便携式转换桌，把已记录的物品再造出来。
* 用能量收集器、反物质继电器、能量凝聚器自动生产 EMC。
* 从哲学之石一路进阶到暗物质 / 红物质工具与宝石护甲。

原有的 ProjectE 配置文件、资源路径、数据包和世界数据在可能的情况下都保持兼容。

## 功能

### EMC 与转换

* 物品与 EMC 双向转换，物品价值永久记录。
* 转换桌与便携式转换桌。
* 可自定义 EMC 值（`custom_emc.json`、游戏内 EMC 管理器，或命令）。
* 按 **F8**（可在「控制」里改键）或使用暂停菜单里的 **EMC 管理器**。可按物品名称 / ID 搜索，或直接选中手持物品的基础类型，然后保存、移除或重置它的 EMC。点 **应用** 即可重新计算并同步所有已保存的改动，不需要 `/reload`。
* 编辑需要对应的 ProjectE 命令权限（`set_emc`、`remove_emc`、`reset_emc`，默认 OP 等级 2）；应用需要这三项全部具备。移除会保存一个零值覆盖；重置会删掉基础物品的覆盖值。该界面不编辑标签和组件变体。

### EMC 生产与自动化

* 能量收集器（MK1–MK3）
* 反物质继电器（MK1–MK3）
* 能量凝聚器（MK1/MK2）
* 暗物质台座

### 装备与实用物品

* 哲学之石 —— 方块转换与世界转化。
* 暗物质与红物质工具，支持可配置的范围挖掘。
* 暗物质熔炉与红物质熔炉。
* 炼金袋与永恒密度宝石。
* 疾风之戒（Swiftwolf's Rending Gale）。
* 完整的宝石护甲能力：夜视、自动上台阶、飞行与战斗效果。

### Trinkets 集成

饰品槽由 [Trinkets](https://modrinth.com/mod/trinkets) 提供：戒指、护身符、符咒和克莱因之星可以装备到 `hand/ring` 与 `chest/necklace` 槽位。没有装 Trinkets 也能正常运行 —— 饰品会当作普通背包物品使用。

### 兼容与本地化

* 经典 Avaritia 物品的内置 EMC 值**默认关闭**。要启用，在 `config/ProjectE/mapping.toml` 顶层设置 `enableAvaritiaEMC = true`，然后重启服务器或重新加载世界。如果开了 `usePregenerated`，先关掉它，EMC 值才会重新计算。这个开关不影响自定义 EMC 值和由配方推导出的值。
* AvaritiaNeo Fabric：自动启用暗物质与红物质奇点，压缩配方消耗 200 个对应的 ProjectE 物质块。不需要物品炼金。
* 完整的中文与英文本地化。
* 兼容原版 ProjectE 的配置格式与模组 ID（`projecte`）。

## 安装

### 环境要求

当前分支（26.3）：

* Minecraft 26.3
* Fabric Loader 0.19.5 或更高
* Fabric API 0.165.0+26.3
* Java 25

（1.21.1 主线分支的要求是 Minecraft 1.21.1、Fabric Loader 0.16.9+、Java 21。）

> Forge Config API Port 和 fabric-permissions-api 已经打包在 ProjectEF Neo 的 JAR 里（jar-in-jar），**不需要额外下载**。

### 安装步骤

1. 安装 Minecraft 26.3 并装上 Fabric Loader。
2. 安装对应版本的 Fabric API。
3. 从 [CurseForge](https://www.curseforge.com/minecraft/mc-mods/projectef) 或 [GitHub Releases](https://github.com/SpacJoy/ProjectEF/releases) 下载 ProjectEF Neo。
4. 把 JAR 放进游戏的 `mods` 文件夹。

### 可选联动

这些模组只是锦上添花，都不是必需的：

* JEI / EMI / REI —— 配方查看器
* Jade / WTHIT —— 方块信息浮层。装了 Jade 时，ProjectEF Neo 会通过 Jade 的 Fabric 插件入口注册自己的 EMC 数据源，不需要给 Jade 打补丁。看向有 EMC 值的方块就会显示。请保持 ProjectE `server.toml` 里的 `misc.lookingAtDisplay` 开启，并在 Jade 的插件设置里启用 **EMC Provider**（`projecte:emc_provider`）。
* Trinkets —— 饰品槽

## 兼容性说明

ProjectEF Neo 保留原版 ProjectE 的模组标识：

```
projecte
```

因此可以兼容：

* 已有的配置文件
* 已有的资源路径
* 已有的世界数据

以下联动来自 NeoForge 版本，本版本**没有**包含：

* CraftTweaker
* The One Probe
* Curios（Fabric 上由 Trinkets 取代）

## 反馈问题

请[提交 issue](https://github.com/wchiway/ProjectEF/issues)，并附上：

* ProjectEF Neo 版本
* Minecraft 版本
* Fabric Loader 版本
* Fabric API 版本
* 复现步骤
* 相关游戏日志

日志请以文件或外部粘贴链接的形式附上，不要直接把完整日志贴在 issue 里。

## 开发

源码构建、开发环境运行、源码集状态等内容见[开发指南](DEV.md)。架构说明、示例与历史资料见[文档索引](docs/README.md)。

## 维护者

**Chiway Wang**

Fabric 移植版开发者与维护者。

## 致谢

ProjectEF Neo 基于原版 **ProjectE** 项目。

特别感谢：

* ProjectE 的开发者与贡献者
* EE2 的原始创作者
* Minecraft 模组社区

原始贡献者包括：

* SinKillerJ
* pupnewfster
* MaPePeR
* williewillus
* Lilylicious
* MozeIntel
* Kolatra

其他致谢：

* x3n0ph0b3 —— EE2 创作者与原始素材授权
* MidnightLightning —— EE2 的 GUI 贴图

## 许可证

ProjectEF Neo 以 MIT 许可证发布。
