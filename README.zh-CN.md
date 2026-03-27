# Schematica（MITE 移植版）

[English](README.md) | 简体中文

这是 Schematica 在 MITE / FishModLoader 环境下的移植版本。

## 版本

当前版本：`0.2.0`

## 功能

- 支持 `.schematic` 的加载、保存、粘贴、投影
- 支持游戏内木棍选区（右键 Pos1，`Shift+右键` Pos2）
- 支持投影生物数据的保存、粘贴与世界生成
- 修复并支持容器方块内容（例如箱子物品）跟随投影/生成
- 提供世界生成参考包：`com.github.lunatrius.worldgen`
- 支持六级标记战利品箱（用于世界生成）

## 环境要求

- JDK 17
- PowerShell（或可执行 `gradlew` 的任意终端）

## 开发环境运行

```powershell
.\gradlew runClient
```

可选用户名：

```powershell
.\gradlew runClient -Pusername=Dev
```

## 构建

```powershell
.\gradlew build
```

产物位于 `build/libs/`。

## Schematic 文件目录

- 游戏运行目录：`<game_dir>/schematics`
- 本工程开发默认目录：`run/schematics`

## 指令列表

所有指令均使用带 `/` 的形式：

- `/schematica help`
- `/schematica list`
- `/schematica load <name>`
- `/schematica unload`
- `/schematica status`
- `/schematica origin here`
- `/schematica move <x> <y> <z>`
- `/schematica nudge <dx> <dy> <dz>`
- `/schematica rotate <90|180|270>`
- `/schematica mirror <x|z>`
- `/schematica paste [replace|solid|nonair]`
- `/schematica undo`
- `/schematica save <x1> <y1> <z1> <x2> <y2> <z2> <name>`
- `/schematica create <name>`
- `/schematica sel status`
- `/schematica sel clear`
- `/schematica menu`

旧的下划线别名指令已移除。

## 快速测试

1. 将结构文件（例如 `test1.schematic`）放入 `run/schematics/`。
2. 进入世界后执行 `/schematica load test1`。
3. 执行 `/schematica status` 检查尺寸与原点。
4. 执行 `/schematica origin here`（或 `/schematica move ...`）调整位置。
5. 执行 `/schematica paste solid`。
6. 需要回滚时执行 `/schematica undo`。

## GUI 与快捷键

- 游戏内按 `M` 打开 Schematica 控制面板。
- 或执行 `/schematica menu`。
- GUI 快捷键：
  - `[` / `]` 切换文件
  - `L` 加载
  - `P` 替换粘贴
  - `O` 实体粘贴
  - `U` 撤销
  - `H` 原点到当前位置
  - `1/2/3` 旋转
  - `W/A/S/D/Q/E` 微调
  - `K` 卸载

## 使用 `.schematic` 做世界生成

参考包位置：
`src/main/java/com/github/lunatrius/worldgen`

1. 把结构文件放到：  
   `src/main/resources/assets/<你的modid>/structures/<name>.schematic`
2. 新建生成器类并继承 `SchematicStructureGenerator`。
3. 在构造器中调用 `super("你的资源路径")`。
4. 在 `SchematicWorldgenRegistration` 中注册该生成器。

### 六级标记战利品箱

在世界生成时，如果某个箱子内“只有一个非空物品堆”，则该箱子会被当作战利品标记箱并按等级重置内容。

标记映射如下：

- 1级：木棍
- 2级：燧石
- 3级：煤炭
- 4级：铁锭
- 5级：金锭
- 6级：钻石

可编辑配置文件：
`src/main/java/com/github/lunatrius/worldgen/WeightedTreasurePieces.java`

你可以修改：

- `LOOT_TABLES`：各等级战利品权重表
- `MIN_ROLLS` / `MAX_ROLLS`：每级抽取次数区间

### 修改生成维度

修改以下常量：
`src/main/java/com/github/lunatrius/worldgen/SchematicWorldgenRegistration.java`

```java
private static final Dimension TARGET_DIMENSION = Dimension.OVERWORLD;
```

可选维度：

- `Dimension.OVERWORLD`
- `Dimension.NETHER`
- `Dimension.END`
- `Dimension.UNDERWORLD`

也可一起调整：

- `WEIGHT`：注册权重
- `CHANCE`：触发概率分母（`1/chance`）

## 安全限制

- `paste/save` 最大体积：`8,000,000` 方块
- `paste/undo` 的 Y 轴限制：`0..255`

## 许可

本项目基于 Schematica / LunatriusCore，并按 MIT 条款分发。  
详见 [LICENSE](LICENSE)。
