# Schematica（MITE 移植版）

[English](README.md) | 简体中文

这是 Schematica 在 MITE / FishModLoader 环境下的移植版本。

当前仓库包含：
- `.schematic` 的读取、保存、粘贴、投影功能
- 游戏内木棍选区（`右键` 设 Pos1，`Shift+右键` 设 Pos2）
- 可供其他模组参考的遗迹生成包：`com.github.lunatrius.worldgen`

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

产物在 `build/libs/`。

## schematic 文件目录

- 游戏运行目录：`<game_dir>/schematics`
- 本工程开发默认目录：`run/schematics`

## 指令列表

- `schematica help`
- `schematica list`
- `schematica load <name>`
- `schematica unload`
- `schematica status`
- `schematica origin here`
- `schematica move <x> <y> <z>`
- `schematica nudge <dx> <dy> <dz>`
- `schematica rotate <90|180|270>`
- `schematica mirror <x|z>`
- `schematica paste [replace|solid|nonair]`
- `schematica undo`
- `schematica save <x1> <y1> <z1> <x2> <y2> <z2> <name>`
- `schematica create <name>`
- `schematica sel status`
- `schematica sel clear`
- `schematica menu`

旧版下划线写法（如 `schematica_load`）仍可继续使用。

## 快速测试流程

1. 将 `test1.schematic` 放入 `run/schematics/`。
2. 进入存档后执行 `schematica load test1`。
3. 执行 `schematica status` 检查尺寸和原点。
4. 执行 `schematica origin here`（或 `schematica move`）调整位置。
5. 执行 `schematica paste solid` 粘贴。
6. 需要回滚时执行 `schematica undo`。

## GUI 与快捷键

- 游戏中按 `M` 打开 Schematica 控制面板。
- 或执行 `schematica menu`。
- GUI 内快捷键：
`[` / `]` 切换文件，`L` 加载，`P` 替换粘贴，`O` 实体粘贴，
`U` 回退，`H` 原点到当前位置，`1/2/3` 旋转，`W/A/S/D/Q/E` 微调，`K` 卸载。

## 使用 `.schematic` 做世界遗迹生成

参考包位置：`src/main/java/com/github/lunatrius/worldgen`

1. 把结构文件放到：
`src/main/resources/assets/<你的modid>/structures/<文件名>.schematic`
2. 新建生成器类，继承 `SchematicStructureGenerator`。
3. 在构造器里调用 `super("你的资源路径")`。
4. 在 `SchematicWorldgenRegistration` 里注册这个生成器。

### 如何修改生成维度

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

也可以一起调整：
- `WEIGHT`：注册权重
- `CHANCE`：触发概率分母（`1/chance`）

## 安全限制

- `paste/save` 最大体积：`8,000,000` 方块
- `paste/undo` 的 Y 轴限制：`0..255`

## 许可证

本项目基于 Schematica / LunatriusCore，按 MIT 条款分发。  
详见 [LICENSE](LICENSE)。
