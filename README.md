# Schematica (MITE Port)

English | [简体中文](README.zh-CN.md)

Port of Schematica for MITE / FishModLoader.

## Version

Current version: `0.2.0`

## Features

- Load/save/paste/projection for `.schematic`
- In-game region selection with stick (`RightClick` = Pos1, `Shift+RightClick` = Pos2)
- Entity data save/paste/worldgen spawn from schematics
- Tile entity data preservation (e.g. chest inventory contents)
- Worldgen reference package: `com.github.lunatrius.worldgen`
- 6-level marker loot chest support for worldgen schematics

## Requirements

- JDK 17
- PowerShell (or any shell that can run `gradlew`)

## Run In Dev

```powershell
.\gradlew runClient
```

Optional username:

```powershell
.\gradlew runClient -Pusername=Dev
```

## Build

```powershell
.\gradlew build
```

Output jars are under `build/libs/`.

## Schematic Directory

- Runtime: `<game_dir>/schematics`
- Dev default: `run/schematics`

## Command Reference

All commands use slash form:

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

Legacy underscore command aliases are removed.

## Quick Test

1. Put a schematic (for example `test1.schematic`) in `run/schematics/`.
2. Enter a world and run `/schematica load test1`.
3. Run `/schematica status` to check size and origin.
4. Run `/schematica origin here` (or `/schematica move ...`).
5. Run `/schematica paste solid`.
6. Run `/schematica undo` if needed.

## GUI & Hotkeys

- Press `M` in-game to open Schematica control GUI.
- Or run `/schematica menu`.
- GUI hotkeys:
  - `[` / `]` file switch
  - `L` load
  - `P` paste replace
  - `O` paste solid
  - `U` undo
  - `H` origin here
  - `1/2/3` rotate
  - `W/A/S/D/Q/E` nudge
  - `K` unload

## Worldgen From `.schematic`

Reference package:
`src/main/java/com/github/lunatrius/worldgen`

1. Put structure file in:
   `src/main/resources/assets/<your_modid>/structures/<name>.schematic`
2. Create a generator class extending `SchematicStructureGenerator`.
3. In constructor, pass resource path to `super(...)`.
4. Register your generator in `SchematicWorldgenRegistration`.

### Marker Loot Chest (6 Levels)

For worldgen schematics, if a chest contains exactly one non-empty marker stack, it is treated as a loot chest marker and replaced with randomized loot.

Marker mapping:

- Level 1: `stick`
- Level 2: `flint`
- Level 3: `coal`
- Level 4: `iron ingot`
- Level 5: `gold ingot`
- Level 6: `diamond`

Editable config is in:
`src/main/java/com/github/lunatrius/worldgen/WeightedTreasurePieces.java`

You can edit:

- `LOOT_TABLES` for each level's weighted items
- `MIN_ROLLS` / `MAX_ROLLS` for number of loot rolls per level

### Change Target Dimension

Edit this constant:
`src/main/java/com/github/lunatrius/worldgen/SchematicWorldgenRegistration.java`

```java
private static final Dimension TARGET_DIMENSION = Dimension.OVERWORLD;
```

Available values:

- `Dimension.OVERWORLD`
- `Dimension.NETHER`
- `Dimension.END`
- `Dimension.UNDERWORLD`

You can also tune:

- `WEIGHT` (registration weight)
- `CHANCE` (spawn chance divisor, `1/chance`)

## Safety Limits

- `paste/save` max volume: `8,000,000` blocks
- `paste/undo` Y bound check: `0..255`

## License

This project is based on Schematica / LunatriusCore and uses MIT terms.
See [LICENSE](LICENSE).
