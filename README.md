# PayTp Mod Documentation

**PayTp** is a lightweight Fabric mod for Minecraft that allows players to teleport by paying a certain amount of in-game currency (such as items or balance). It supports flexible teleportation modes, multi-language localization, and fully customizable cost rules.

中文文档请看[这里](./README.cn.md)

> [!WARNING]
> - *Due to huge API changes in 26.1+ and the fact that Minecraft is no longer using **obfuscation** for source codes, patches later than **v1.2.0** will NOT support versions below **26.1**.*
> - *For **data migration**, please check [here](./docs/MIGRATION.md).*

---

## Features

- **Editable command names**
- Cross-dimension teleport to a specified location
- Player teleport request system
- Home and Back
- Beacon waypoint (Warp) feature
- Fully customizable JEXL teleportation price and distance algorithm
- Ender Chest / Shulker Box payment support
- **Cloth Config** API support (client-side)
- Can be used as a **server-side only** mod

Most features can be **disabled** by setting their corresponding command names to **an empty string**. 
For example, changing `teleport.coordinateCommand` in the config file from `ptp` to **empty** will disable the coordinate teleport function.
The in-game help guide will automatically adapt.

---

## Commands

*All displays show the default command names, where <> indicates required parameters and () indicates optional parameters*

| Command                        | Description                                                  |
|--------------------------------|--------------------------------------------------------------|
| `/ptphelp`                     | Get command guide for PayTp                                  |
| `/ptp (dimension) <x> <y> <z>` | Teleport to specified coordinates (in a specific dimension)  |
| `/ptpto <player>`              | Send request to teleport to a player                         |
| `/ptphere <player>`            | Send request to a player to teleport to you                  |
| `/ptpaccept (player)`          | Accept a teleport request (from a specific player)           |
| `/ptpdeny (player)`            | Deny a teleport request (from a specific player)             |
| `/ptpcancel (player)`          | Cancel a pending teleport request (to a specific player)     |
| `/ptpback`                     | Return to the previous location                              |
| `/ptphome`                     | Teleport to your home (if configured)                        |
| `/ptphome set`                 | Set your home to your current position                       |
| `/ptpwarp <name>`              | Teleport to the specified waypoint                           |
| `/ptpwarp create <name>`       | Create a new waypoint (must be within an active beacon beam) |
| `/ptpwarp delete <name>`       | Delete the specified waypoint                                |
| `/ptpwarp list (page)`         | View all waypoints on the server                             |

---

## Configuration

### Configuration File Location:

```
~/config/paytp.json
```

### Example Structure:

```json
{
  "general": {
    "language": "en_us",
    "helpCommand": "ptphelp",
    "effect": {
      "particleEffect": true,
      "soundEffect": true
    }
  },
  "teleport": {
    "coordinateCommand": "ptp",
    "allowCrossDim": true
  },
  "request": {
    "requestCommand": {
      "toCommand": "ptpto",
      "hereCommand": "ptphere",
      "acceptCommand": "ptpaccept",
      "denyCommand": "ptpdeny",
      "cancelCommand": "ptpcancel"
    },
    "expireTime": 10
  },
  "home": {
    "homeCommand": "ptphome"
  },
  "back": {
    "backCommand": "ptpback",
    "maxBackStack": 10
  },
  "warp": {
    "warpCommand": "ptpwarp",
    "maxInactiveTicks": 100,
    "checkPeriodTicks": 20
  },
  "price": {
    "currencyItem": "minecraft:diamond",
    "minPrice": 1,
    "maxPrice": 64,
    "algorithm": "10",
    "deduction": {
      "allowEnderChest": true,
      "prioritizeEnderChest": true,
      "allowShulkerBox": false,
      "prioritizeShulkerBox": false
    }
  }
}
```

---

## Configuration Details

### General Settings

| Field         | Type     | Description                                                                      |
|---------------|----------|----------------------------------------------------------------------------------|
| `language`    | `string` | Language file (e.g., `zh_cn`, `en_us`, `zh_tw`), affects messages and help text. |
| `helpCommand` | `string` | Command used to display the PayTp guide (default `/ptphelp`).                    |

#### Teleport Effects (`general.effect`)

| Field            | Type      | Description                                   |
|------------------|-----------|-----------------------------------------------|
| `particleEffect` | `boolean` | Enable teleport particles; defaults to `true`. |
| `soundEffect`    | `boolean` | Enable teleport sounds; defaults to `true`.    |

---

### Coordinate Teleport

| Field               | Type      | Description                                                                                                             |
|---------------------|-----------|-------------------------------------------------------------------------------------------------------------------------|
| `coordinateCommand` | `string`  | Coordinate teleport command (default `/ptp`).                                                                           |
| `allowCrossDim`     | `boolean` | Whether any teleport method may cross dimensions; defaults to `true`. When disabled, the dimension argument is not registered or displayed. |

---

### Teleport Request System

#### Request Commands

| Field           | Type     | Description                                                                             |
|-----------------|----------|-----------------------------------------------------------------------------------------|
| `toCommand`     | `string` | Command to request teleporting to the target player (default `/ptpto`).                 |
| `hereCommand`   | `string` | Command to request the target player to teleport to your location (default `/ptphere`). |
| `acceptCommand` | `string` | Command to accept a request (default `/ptpaccept`).                                     |
| `denyCommand`   | `string` | Command to deny a request (default `/ptpdeny`).                                         |
| `cancelCommand` | `string` | Command to cancel a sent request (default `/ptpcancel`).                                |

#### Configuration

| Field        | Type   |  Description                                 |
|--------------|--------|----------------------------------------------|
| `expireTime` | `int`  | Request expiration time in seconds; defaults to `10` and must be non-negative. |

---

### Home System

| Field         | Type     | Description                                    |
|---------------|----------|------------------------------------------------|
| `homeCommand` | `string` | Command to teleport home (default `/ptphome`). |

---

### Back System

| Field          | Type     | Description                                                  |
|----------------|----------|--------------------------------------------------------------|
| `backCommand`  | `string` | Command to return to previous location (default `/ptpback`). |
| `maxBackStack` | `int`    | Maximum saved historical positions; defaults to `10` and must be greater than zero. |

---

### Waypoint System

| Field              | Type     | Description                                                                                |
|--------------------|----------|--------------------------------------------------------------------------------------------|
| `warpCommand`      | `string` | Command name to teleport to a waypoint (default `/ptpwarp`).                               |
| `maxInactiveTicks` | `int`    | Ticks before deleting a waypoint after beacon deactivation; defaults to `100` and must be non-negative. |
| `checkPeriodTicks` | `int`    | Waypoint-to-beacon check interval in ticks; defaults to `20` and must be greater than zero.             |
 
---

### Cost Calculation Settings

#### Currency

| Field          | Type     | Description                                              |
|----------------|----------|----------------------------------------------------------|
| `currencyItem` | `string` | A valid currency item ID; defaults to `minecraft:diamond`. |

#### Price Range and Algorithm

| Field       | Type     | Description                                                                                          |
|-------------|----------|------------------------------------------------------------------------------------------------------|
| `minPrice`  | `int`    | Final lower bound for non-negative prices; defaults to `1` and must satisfy `0 <= minPrice <= maxPrice`. |
| `maxPrice`  | `int`    | Final upper bound; defaults to `64`. Setting both `minPrice` and `maxPrice` to `0` skips the algorithm and fixes the price at `0`. |
| `algorithm` | `string` | A JEXL script that calculates distance and raw price and must return an `int`.                       |

#### Payment Deduction (`price.deduction`)

| Field                  | Type      | Description                              |
|------------------------|-----------|------------------------------------------|
| `allowEnderChest`      | `boolean` | Allow Ender Chest currency; defaults to `true`. |
| `prioritizeEnderChest` | `boolean` | Prioritize Ender Chest deduction; defaults to `true` and requires `allowEnderChest`. |
| `allowShulkerBox`      | `boolean` | Allow Shulker Box currency; defaults to `false`. |
| `prioritizeShulkerBox` | `boolean` | Prioritize Shulker Box deduction; defaults to `false` and requires `allowShulkerBox`. |

---

## JEXL Price Algorithm

The `price.algorithm` string contains both the distance calculation and the price calculation. There are no fixed Java-side distance formulas or teleport multipliers. A script may be as simple as `10`, which returns a fixed price of 10.

### Available Variables

| Variable       | Type       | Description                                                                 |
|----------------|------------|-----------------------------------------------------------------------------|
| `from`         | `position` | Source position with `.x`, `.y`, `.z`, and `.dimension` properties.         |
| `to`           | `position` | Destination position with `.x`, `.y`, `.z`, and `.dimension` properties.    |
| `teleportType` | `string`   | One of `coordinate`, `request`, `home`, `back`, or `warp`.                  |
| `player`       | `string`   | Name of the player being teleported.                                        |
| `otherPlayer`  | `string`   | Name of the other player involved in a request, or an empty string if none. |

`crossDimension` is intentionally not provided. Determine it inside the script:

```jexl
var crossDimension = from.dimension != to.dimension;
```

### Return Value and Strict Mode

- The last expression must return an `int`. Integer literals such as `10` already satisfy this requirement.
- Returning a negative integer cancels the payment and teleport. This allows an algorithm or
  Minecraft command result to deliberately stop the operation without causing a script error.
- JEXL runs in strict mode, so undefined variables and invalid expressions are treated as errors.

### Math Methods

> [!TIP]
> Use `math` for distance, multiplier, rounding, and boundary calculations directly inside the price algorithm without invoking an external process.

The `math` namespace exposes Java's built-in `Math` methods.

| Method                       | Return type | Description                                      |
|------------------------------|-------------|--------------------------------------------------|
| `math:sqrt(value)`           | `double`    | Returns the square root of a value.              |
| `math:max(first, second)`    | numeric     | Returns the greater of two values.               |
| `math:min(first, second)`    | numeric     | Returns the smaller of two values.               |
| `math:round(value)`          | `long`      | Returns the closest integer value.               |
| `math:pow(base, exponent)`   | `double`    | Raises `base` to the supplied power.             |
| `math:abs(value)`            | numeric     | Returns the absolute value.                      |

Methods may return a numeric type other than `int`. Convert the final result explicitly when
necessary:

```jexl
math:round(rawPrice).intValue();
```

### Minecraft Commands

> [!WARNING]
> The `minecraft` namespace can execute every command registered with the server, including commands added by other mods. Commands run as the player being teleported with `ALL_PERMISSIONS`, so they can perform destructive or administrative operations such as `op`, `ban`, `data`, and `stop`. Only use algorithms from fully trusted sources.

| Method                       | Return type | Description                                                                 |
|------------------------------|-------------|-----------------------------------------------------------------------------|
| `minecraft:execute(command)` | `int`       | Executes a Minecraft command. The leading `/` is optional and command feedback is suppressed. |

The current player is the command source, so `@s`, relative coordinates, and the current dimension
work as expected:

```jexl
minecraft:execute("scoreboard players add " + player + " teleport_count 1");
minecraft:execute("tp @s ~ ~1 ~");
10;
```

During configuration validation, `minecraft:execute(...)` returns `0` without running the command,
so loading or editing an algorithm cannot modify server state.

### Shell Commands

> [!CAUTION]
> The `shell` namespace executes arbitrary commands with the same operating-system permissions as the Minecraft server. Only use algorithms from fully trusted sources. Shell commands are synchronous and therefore block the server thread until they finish. They are also executed during algorithm validation, including config loading, editing, and importing.

| Method                   | Return type   | Description                                                                    |
|--------------------------|---------------|--------------------------------------------------------------------------------|
| `shell:execute(command)` | `ShellResult` | Executes through `/bin/sh -c` on Unix-like systems or `cmd.exe /c` on Windows. |
| `shell:run(command)`     | `string`      | Returns standard output, or throws an error when the exit code is non-zero.    |
| `shell:runInt(command)`  | `int`         | Requires standard output to contain exactly one valid integer.                 |

`ShellResult` exposes `exitCode`, `stdout`, and `stderr`:

```jexl
var result = shell:execute("python3 /opt/paytp/price.py");
result.exitCode == 0 ? result.stdoutInt() : 0;
```

For a price algorithm, `runInt` is usually the simplest form:

```jexl
shell:runInt("python3 /opt/paytp/price.py '" + player + "'");
```

### Example Algorithm

```jexl
// Available variables:
// from, to: positions with .x, .y, .z, and .dimension
// teleportType: "coordinate", "request", "home", "back", or "warp"
// player: name of the player being teleported
// otherPlayer: name of the other request player, or an empty string
//
// Java's built-in Math methods are available through the "math" namespace.
// Minecraft commands are available through minecraft:execute("command").
// Full system shell access is available through the "shell" namespace.

var basePrice = 1;
var baseRadius = 10.0;
var pricePerBlock = 0.01;
var crossDimensionMultiplier = 1.5;
var homeMultiplier = 0.5;
var backMultiplier = 0.8;
var warpMultiplier = 0.5;

var deltaX = from.x - to.x;
var deltaY = from.y - to.y;
var deltaZ = from.z - to.z;
var distance = math:sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
var multiplier = from.dimension != to.dimension ? crossDimensionMultiplier : 1.0;

if (teleportType == "home") {
  multiplier = multiplier * homeMultiplier;
} else if (teleportType == "back") {
  multiplier = multiplier * backMultiplier;
} else if (teleportType == "warp") {
  multiplier = multiplier * warpMultiplier;
}

var distanceBeyondBase = math:max(0, distance - baseRadius);
math:round((basePrice + distanceBeyondBase * pricePerBlock) * multiplier).intValue();
```

The default algorithm additionally handles Nether coordinate scaling and The End. It is written into a newly generated configuration file and can be used as a complete starting template.

### Validation and Final Price

1. If `maxPrice` is `0`, the algorithm is not executed and every teleport costs `0`.
2. A negative script result cancels the payment and teleport without being clamped.
3. Otherwise, the script result is forcibly clamped to the inclusive range `[minPrice, maxPrice]`.
4. The script is compiled and test-executed when the configuration is validated. In Mod Menu, invalid input is marked red and prevents saving.
5. If an algorithm fails during an actual teleport, `calculatePrice` returns `-1`; PayTp logs the error, cancels the teleport, and notifies the player that the payment process failed.

---

## Cloth Config Support

If the **Cloth Config API** is installed, all settings can be adjusted directly through the in-game Mod Menu GUI. The price algorithm can be edited in a dedicated multi-line editor or imported from a `.jexl` file. Confirming an edit or importing a file immediately validates its compilation and integer output; invalid algorithms cannot be saved. (World restart may be required.)

---

## Compatibility & Deployment

| Type                     | Supported                           |
|--------------------------|-------------------------------------|
| Fabric Loader            | ✅                                  |
| Server Only              | ✅                                  |
| Client UI (Cloth Config) | ✅                                  |
| Multi-language Support   | en_us / zh_cn / zh_tw               |
| Minecraft Version        | 26.1+<br/>1.21.4 ~ 1.21.11 (legacy) |

---

## Credits

This mod is inspired by early economy-style teleport plugins. The request logic references the **Teleport Command** mod. The waypoint logic references the **Beacon Waypoint** mod.   
Developed using Fabric API and fully compatible with vanilla saves.  
Feel free to submit issues or pull requests on GitHub to improve configuration and calculation algorithms.
