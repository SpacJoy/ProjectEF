# 更新日志

ProjectEF Neo 的所有重要变更都记录在此。

## [1.4.2] - 2026-11-12

### 修改

- 飓风长靴的移动速度加成从 +100% 下调到 +50%。原实现是 `ADD_MULTIPLIED_TOTAL` 1.0，把基础移速 0.1 直接翻倍成 0.2，跑起来快到手感失控；现在系数是 0.5，等效 `0.1 → 0.15`。旧的 `ItemStack` 会在穿戴时由 `GemFeet#inventoryTick` 的 `syncStepAssistModifiers` 重刷成新值，不需要重新拿一件。
- 客户端对视野拉伸的补偿跟着改动：`AbstractClientPlayerMixin` 里减 0.5 的常量换成 0.25。原版 `getFieldOfViewModifier` 按 `(speed / walkingSpeed + 1) / 2` 算，+100% 落在 1.5、减 0.5 刚好抵消，+50% 落在 1.25、只有减 0.25 才对得上。这两处数值必须同步，否则穿靴子跑动时视野会被拉宽或压窄。

## [1.4.1] - 2026-11-07

### 修复

- 贤者之石的世界转换预览原本渲染成一块实心黑/品红棋盘格，而不是半透明高亮。预览的渲染类型是基于 `textures/misc/white.png` 构造的——那只是一张纯白贴图，纯粹用来当「无贴图、只要纯色」的取巧手段；Minecraft 在 1.21.10 移除了该资源，于是在 26.3 上预览绑到了一张缺失贴图。现在改用原版的调试填充方块渲染类型：一条 POSITION_COLOR 四边形管线，不受光照、参与混合、不剔除、不写深度，并带视图偏移，因此不会与下方方块产生 z-fighting；每个顶点只提交位置与颜色。这与移植前 1.21.1 分支手工构造的 POSITION_COLOR 渲染类型一致，方块各面仍严格按原版的绕序输出，背面剔除依然能正确保留它们。
- 手持贤者之石时，如果世界转换预览的目标变成流体，游戏会直接崩溃。黑曜石会转换成岩浆，所以对准激活的地狱传送门框架的黑曜石时，预览目标就变成了岩浆方块，而 HUD 预览会无条件调用 `FluidModel#tintSource()`。Minecraft 26.3 只给真正有色调的流体烘焙 tintSource——原版水模型带 `BlockTintSources.water()`，岩浆模型是用 `null` 构造的——而原版自己的 `FluidRenderer` 对该方法做了判空并回退到 `-1`。现在预览也做同样的判空，把岩浆贴图按无色调渲染，而不是整个渲染帧被丢弃。此外，色调在预览重新补上不透明度之前会先截取 RGB 通道，这样本身就带 alpha 的流体颜色不会再泄进这次 blit。

## [1.4.0] - 2026-11-07

### 新增

- 「非卖品」附魔现在有了合成配方：一本书加一颗贤者之石合成为带有 `projecte:unsellable` 的附魔书，书会被消耗，贤者之石不会。这不需要任何配方代码：`PhilosophersStone.initCraftingRemainder()` 早就给贤者之石设置了指向自身的 `craftingRemainingItem`，而 26.3 的 `CraftingRecipe.getRemainingItems` 是一个默认方法，会遍历每个原料的 `Item.getCraftingRemainder()`，所以只要贤者之石出现在任何原版形态的配方里都不会被消耗。产物通过配方的 `components` 块携带附魔；由于 26.3 的 `ItemEnchantments.CODEC` 是普通的 `unboundedMap`，JSON 里直接写出这个映射即可，不需要 1.21.1 数据里那层 `levels` 包装。玩家一旦持有贤者之石，配方书里的对应进度就会解锁它。
- 「非卖品」附魔书现在可以在铁砧上附加到任意物品，不再局限于 `supported_items` 里列出的装备。这个附魔的用途就是把任意物品标记为不可出售，如果铁砧只接受装备列表内的物品，上面那条配方就完全没用了。原版对附魔台候选物品走 `Enchantment.isPrimaryItem` 过滤，而铁砧和 `/enchant` 走 `Enchantment.canEnchant`，因此只放开了 `canEnchant`：附魔台仍然只对与之前完全相同的装备提供该附魔，也无法把它随机到其他物品上。战利品表受影响的逻辑与铁砧相同，但只有在数据包显式把该附魔写进 `enchant_randomly` 函数时才会出现。

## [1.3.5] - 2026-11-03

### 修复

- 26.3 上 EMC 管理器里的文字不可见。Minecraft 26.3 会丢弃一切 alpha 通道为 0 的文字，而界面里多处标签写的是裸 `0xRRGGBB` 字面量，alpha 留在 0，导致覆写值、页码和物品列表各行都渲染成空白。现在界面里所有颜色都显式带上 `0xFF` alpha。
- EMC 管理器丢失了主手物品的数据组件。界面用 `new ItemStack(stack.getItem())` 重建条目，这会丢掉全部组件，于是靠组件决定显示名和图标的模组——TaCZ 最为典型——只能显示原始注册名和缺失图标。现在主手堆叠会连同组件一起复制，物品列表的显示堆叠也改为取自创造模式搜索页签，这样仅有组件差异的变体同样能保住名称和图标。加载的语言文件里没有对应翻译键的物品，现在回退显示注册名，而不是原始键名。
- `config/projecte/common.toml` 里的 `highAlchemistUUIDs` 列表每次加载都被判定无效并被重写。它用 `define(...)` 声明，构造出的 `ValueSpec` 校验器是针对整个值运行的，于是 `o instanceof String` 这个元素判定实际作用在 `List` 本身之上，永远不可能通过；`isCorrect()` 因此总报告需要纠正，每次启动都重写该文件并轮换一份 `common-N.toml.bak` 备份。现在改用 `defineListAllowEmpty(...)` 声明，其校验器逐元素检查并接受空列表，字段类型为 `ConfigValue<List<? extends String>>`。
- 关闭游戏时会产生一份来自 `ClientShutdownWatchdog` 的崩溃报告（`java.lang.Error: Watchdog (Client shutdown from post-main)`）。自 26.2 起原版退出不再调用 `System.exit(0)`，因此任何仍然存活的非守护线程池都会阻止 JVM 结束，看门狗在 15 秒后强制击杀它。`BasePEConfig` 的静态配置保存执行器正是这样的线程池；现在改用命名的守护线程（`ProjectE-Config-Saver`），排队中的写入不再能把进程吊住。

## [1.3.4] - 2026-10-09

### 新增

- 模式切换按键（默认 `G`）现在也能切换佩戴在饰品（Trinkets）栏位中带模式的物品，但仅在主手为空、主手是原版物品，或主手是自身没有模式的 ProjectE 物品时才生效。其他模组的物品被有意排除在外：它们可能也为同一个键绑定了功能，而原版一次物理按键只会派发给单个绑定，多增加一个消费者不是模组能安全做到的事。Swiftwolf 的狂飙之风现在对外暴露了模式切换能力，拿在手里的戒指也能用该按键切换，以前只能右键。
- Swiftwolf 的狂飙之风会向携带它的玩家播放起飞与降落反馈：飞行开始时是一声音调更高的鸣响，结束时是失衡音效。这刻意独立于戒指的模式以及由哪个模组提供飞行，因此飞行意外结束（EMC 耗尽、输入被吞、其他模组的装备被卸下）总是能被察觉。音效只发给受影响的玩家；当戒指自身的模式切换已经播报过同一次转换时——EMC 耗尽会同时关闭戒指并掉飞行——只播放那次模式音效，所以同一次转换不会被播报两遍。
- 当安装了 Better Combat 时，红物质短刃和红物质钉头锤现在能接入它的近战系统：两者分别附带武器属性数据文件（短刃用 `bettercombat:claw`，钉头锤用 `bettercombat:mace`，与 Better Combat 原本为锤类挑选的预设一致），因此会用对应的动画与判定框挥击，而不是退回原版挥手动作。Better Combat 分配武器预设的来源是逐物品数据文件，找不到时退回一张内置的回退表，该表用正则匹配物品 id——表里有 `sword` 和 `hammer` 条目，这就是剑和锤早就能正常工作的原因，但没有 `katar` 或 `morning_star`。现在两个物品都有数据文件覆盖，即使玩家改动了回退表也依然有效；未安装 Better Combat 时这些文件不生效。
- EMC 管理器现在可以开关「EMC 漏洞」警告。映射器会为每一次原料成本与固定或自定义 EMC 值不符的转换打一条警告，而映射是在首个玩家加入时惰性运行的，因此过去会在客户端连入的那一刻往服务器日志里倾倒几十条警告，每次 Apply 再来一遍。新的开关写入 `config/projecte/mapping-server.toml` 的 `logFoundExploits`，设置可跨重启保留，并且与 Apply 受同一套权限控制（set、remove、reset，在无权限模组的服务器上是 OP 等级 2）。重新打开警告只对打算重新映射的人有意义，所以它从不影响已在运行的映射：该值在下一轮映射开始时读取。

### 变更

- Swiftwolf 的狂飙之风现在只在选中某个飞行模式（`Flight` 或 `Flight + Shield`）时才提供飞行，不再因为玩家恰好离地就自动进入飞行模式。右键循环四个模式（`Off` → `Flight` → `Shield` → `Flight + Shield`），提示文本显示当前模式，切换时在聊天栏播报新模式。用其他模组的飞行装备飞行不再会把戒指拖进飞行模式并悄悄为一段它并未实际提供的飞行抽 EMC。注意：如果在别的模组提供飞行期间主动选中了飞行模式，戒指仍会扣 EMC——Fabric 没有按来源区分的飞行标记，所有模组共享同一个 `mayfly` 布尔值，唯一能判断别的装备是否单独够用的办法是撤掉戒指的贡献，看飞行是否还在。

## [1.3.3] - 2026-10-08

### 新增

- EMC 管理器新增「重置手持模组」按钮，位于「手持物品」旁边。它一次性清除主手物品所属模组的全部自定义 EMC 值，等同于逐个重置每个基础物品；之后点击 Apply 重新计算并同步。需要 reset 权限。
- 转换桌与转换平板界面现在支持滚轮翻页，不必再点上一页/下一页按钮。

### 修复

- 禁制火把渲染成实心不透明的绿色团块。NeoForge 通过模型 JSON 的 `render_type` 字段选择方块的渲染层，而 Fabric 没有等价机制并会静默忽略该字段，于是镂空（cutout）模型退回了实心层，其带 mipmap 的图集混合会把透明像素与邻居平均掉。现在方块及其墙变体都显式注册到 cutout 渲染层。

## [1.3.2] - 2026-10-02

### 新增

- 游戏内 EMC 管理器，按 F8（可重绑）或从暂停菜单打开。可按名称或注册 ID 搜索基础物品，选中手持物品的基础类型，无需命令即可保存、移除或重置自定义 EMC 值。
- 服务端权限检查、并发编辑冲突检测、安全持久化，以及一个 Apply 按钮，用于重新计算并同步已保存的 EMC 变更到整个服务端，包括启用预生成值的情况。
- 管理器的简体中文与英文翻译，以及渲染顺序与翻译一致性检查。

### 修复

- EMC 管理器的标题、数值与状态文字被第二次背景渲染覆盖的问题。窗口过小时现在显示缩放提示，而不是控件重叠。
- Jade 的 EMC 显示，通过注册 Fabric 插件入口点修复。

### 变更

- 经典 Avaritia 物品的内置 EMC 值默认关闭。自定义覆写与配方推导出的值不受影响。
- 更新发布与更新检查元数据至 1.3.2。

## [1.2.3] - 2026-08-10

### 修复

- 新星催化剂与新星灾变重新按各自的正确威力爆炸（16 和 48），而不是原版 TNT 的爆炸威力（4.0）。`PrimedTnt#explode` 在 NeoForge 中是 `protected`，在原版中是 `private`，所以移植时用 `tick()` 覆写替换了 `explode()` 覆写，而它从未真正接管引爆——`super.tick()` 早已执行过原版爆炸，`getExplosionPower()` 与整条 `NovaExplosion` 路径成了死代码。现在该方法通过访问拓宽器放开，并照旧被覆写。
- 新星催化剂与新星灾变被点燃后不再变成原版 TNT。Fabric 没有 `TntBlock.onCaughtFire` 钩子（NeoForge 版本会把所有点燃路径都汇聚到它），因此任何未显式覆写的路径都会落到静态方法 `TntBlock.explode` 并生成普通 `PrimedTnt`。现在 `ProjectETNT` 覆写了全部六个与点燃相关的 `TntBlock` 方法，另有两个 mixin 覆盖直接调用静态方法的路径：
  - `TntBlockMixin` 拦截 `TntBlock.explode`，覆盖所有方块仍在原位的调用方——最典型的是原版打火石发射器行为，它是先点燃、后移除方块。
  - `FireBlockMixin` 重定向 `FireBlock.checkBurnOut` 里的调用。该路径是先移除方块、后点燃，因此必须提前缓存 `BlockState`。
- 火焰蔓延现在真的能点燃新星方块。它们从未被注册为可燃，`FireBlock.checkBurnOut` 根本不会走到它们身上。

## [1.2.2] - 2026-08-04

### 修复

- 贤者之石作为合成原料时不再被消耗，其 EMC 也不会被计入产物。`Item#getCraftingRemainingItem` 在 1.21.1 中是 `final`，因此现在通过访问拓宽器把贤者之石注册为自身的合成剩余物。这也让它在 `BaseRecipeTypeMapper` 中的净贡献归零——那里会从原料成本里减去剩余物的 EMC。
- 将 Cardinal Components API 降级到 6.1.2。6.1.3 版本列在 Ladysnake 的 Maven 元数据里，但其构件实际无法解析，导致 CI 构建失败。

### 变更

- 更名为 **ProjectEF Neo**（中文名：等价交换Neo）。

## [1.2.1] - 2026-07-31

### 修复

- 佩戴的饰品不再丢失被动能力。Trinkets 把已装备的堆叠放在 Cardinal Components 的物品栏里而不是玩家的物品栏，因此原版从不为它们调用 `Item#inventoryTick`；现在 `ProjectETrinket` 会把饰品 tick 转发给它。这同时堵上了一个白嫖飞行的漏洞：佩戴的 Swiftwolf 狂飙之风提供飞行却从不消耗 EMC。
- 饰品栏位标签现在真的能限制什么物品放进哪里。随包发布的 `trinkets:all` 标签让每个 ProjectE 饰品都能装备到任意栏位——包括其他模组新增的栏位——因为 Trinkets 的 `trinkets:tag` 校验器接受物品位于该栏位标签或位于 `trinkets:all` 中。
- 时间手表注册为 Trinkets 饰品。它列在 `chest/necklace` 标签里，却从未传给 `TrinketsApi.registerTrinket`。
- 虚空戒指列入 `hand/ring` 标签。它此前只依赖 `trinkets:all`，否则将变得无法装备。
- 在饰品栏位中被原地修改的堆叠现在会同步到客户端。抽取佩戴的克莱因之星换取 EMC 或修理佩戴的物品会绕过 `insertItem`/`extractItem`，从不通知 Trinkets 物品栏，于是客户端一直显示旧值。
- `.gitignore` 不再忽略 Trinkets 集成源码与数据包。不带前导斜杠的 `trinkets/` 模式会匹配任意深度的同名目录，导致其中新增的文件被静默漏跟踪。

### 变更

- `IItemHandler` 新增 `markSlotChanged(int)`，这是给那些原地修改 `getStackInSlot` 返回堆叠的消费者用的默认空实现。`ItemStackHandler` 把它桥接到已有的 `protected onContentsChanged(int)` 钩子上，这同时覆盖了修理护身符的炼金袋与箱子路径。
- 发布说明现在列出每个发布构件的体积与用途，清晰说明玩家该装哪个 jar、哪些是给附加模组开发用的。

### 移除

- `IExposesCurioAttributes` 与 `ProjectETrinket.getModifiers` 覆写。唯一的实现者没有贡献任何修饰符，整条路径等同于 Trinkets 内置的默认饰品。

## [1.2.0] - 2026-07-29

### 新增

- 通过共享的 `RecipeViewerHelper` 抽象接入 REI（Roughly Enough Items）配方查看器，包含能量收集器燃料升级与世界转换展示分类及工作站注册。
- Trinkets 饰品集成，取代被移除的 NeoForge Curios 支持。
  - 19 个 ProjectE 物品（戒指、护身符、符咒、克莱因之星）注册为 Trinkets，可装备到 `hand/ring` 与 `chest/necklace` 栏位。
  - Trinkets 物品栏以 `IItemHandler` 暴露给 ProjectE 的轮询式系统（燃料消耗、修理护身符、快捷栏或饰品能力检查）。
  - 奥术戒指的飞行在佩戴时生效：`InternalAbilities.shouldPlayerFly` 通过 `PlayerHelper.checkHotbarCurios` 轮询 Trinkets 物品栏。
  - 数据驱动的栏位分配：`data/trinkets/entities/projecte.json` 为玩家授予 `hand/ring` 与 `chest/necklace` 栏位。
  - 物品标签：`trinkets:hand/ring`、`trinkets:chest/necklace` 与 `trinkets:all`，确保物品能被 Trinkets 的栏位校验器接受。
- `fabric.mod.json` 中为 REI 插件新增 `rei_client` 入口点。
- 为开发环境测试新增 `trinkets` 的 `modLocalRuntime` 依赖。

### 变更

- `PETags` 的 curios 标签常量更新为 Trinkets 的 `group/slot` 格式（`hand/ring`、`chest/necklace`、`legs/belt`）。
- `PEItemTagsProvider` 的 datagen 标签分配对齐新的 Trinkets 标签路径。
- 恢复 `buildSrc` 目录（此前曾被删除）。
- `.gitignore` 更新，正确跟踪 Trinkets 集成文件。

### 移除

- 过时的 datagen 生成的 Curios 标签文件（`data/curios/tags/item/`）。
- 平台文档中 NeoForge Curios 的占位引用。

### 已知限制

- 尚未实现玩家模型上的 Trinkets 饰品渲染（`TrinketRenderer`）。
- 尚未移植 REI 的物品子类型比较（按模式或存储 EMC 区分物品）。
- 戒指的被动 tick 效果仍受 `hotBarOrOffHand` 物品栏栏位检查限制，与现有物品设计一致。

## [1.1.0] - 2026-07-21

### Fabric 移植

本次发布推出 ProjectE 的 Fabric 1.21.1 版本，名为 **ProjectEF**。其中的 `F` 代表 Fabric。

### 新增

- 支持 Minecraft 1.21.1 的 Fabric Loader。
- 适用于 Fabric 的公共 API 原语、注册表、事件、网络、能力与数据附加。
- EMC 映射、配方映射、自定义转换、世界转换，以及服务端到客户端的同步。
- ProjectE 的物品、方块、实体、配方、容器、方块实体与客户端渲染。
- 能量收集器、反物质中继器、能量凝聚器、暗物质熔炉、红物质熔炉及相关自动化支持。
- 转换桌、转换平板、贤者之石、炼金袋、基座与永恒密度功能。
- JEI 与 EMI 配方查看器集成。
- 安装了对应模组时可选的 Jade 与 WTHIT 集成。
- 经典 Avaritia 物品的内置 EMC 值。
- 兼容 Fabric 的配方加载条件，用于生成的配方与进度。
- 扩充的简体中文翻译与更新后的本地化元数据。

### 恢复与修复

- 创造模式飞行与戒指提供的飞行行为。
- GEM 盔甲能力，包括头盔夜视与靴子踏步辅助。
- 暗物质与红物质工具的区域模式，包括 3x3 类模式。
- 贤者之石的世界转换、转换循环、选择范围渲染与配方交互。
- ProjectE 燃料在暗物质与红物质熔炉中的燃料处理。
- 火山岩护身符的岩浆放置与弹射物行为。
- 基座上戒指及其他受支持物品的渲染。
- `/project` 客户端诊断命令的注册与执行。
- 编辑 EMC 配置或映射数据后的 EMC 重载行为。
- 红物质剑与红物质短刃对高生命值实体的秒杀模式。
- Minecraft 1.21.1 下的 Fabric 权限兼容与 Xaero 地图集成。

### 平台与兼容性变更

- 用 Fabric Loom 1.10.5、Mojang 映射与 Parchment 映射替换 NeoForge 构建工具。
- 新增 Fabric 访问拓宽器、mixin 配置与 Fabric 客户端/服务端入口点。
- 新增 Forge Config API Port 作为必需的运行时依赖。
- 内置兼容 Minecraft 1.21.1 的 `fabric-permissions-api` 0.3.1 发行版。
- 保留内部模组 ID `projecte` 与既有配置目录 `config/ProjectE`。
- 移除或推迟没有兼容 Fabric 1.21.1 实现的 NeoForge 专属集成，包括 CraftTweaker 与 The One Probe。
- 构件名称使用 `ProjectEF-1.21.1-PE1.1.0.jar` 的格式。

### 环境要求

- Minecraft 1.21.1。
- Fabric Loader 0.16.9 或更高版本。
- Minecraft 1.21.1 的 Fabric API。
- Forge Config API Port 21.1.0 或更高版本。
- Java 21。

### 已知限制

- 本版本仅面向 Fabric；NeoForge 专属集成不可用。
- REI 与 Trinkets 集成自 1.2.0 版起可用。
- 旧的 NeoForge 测试框架与数据生成器不属于当前 Fabric 构建的一部分。
