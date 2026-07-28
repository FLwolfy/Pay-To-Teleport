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
    "helpCommand": "ptphelp"
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
    "algorithm": "10"
  },
  "setting": {
    "effect": {
      "particleEffect": true,
      "soundEffect": true
    },
    "flag": {
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

---

### Coordinate Teleport

| Field               | Type      | Description                                                                                                             |
|---------------------|-----------|-------------------------------------------------------------------------------------------------------------------------|
| `coordinateCommand` | `string`  | Coordinate teleport command (default `/ptp`).                                                                          |
| `allowCrossDim`     | `boolean` | Whether any teleport method may cross dimensions. When disabled, the dimension argument is not registered or displayed. |

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
| `expireTime` | `int`  | Teleport request expiration time in seconds. |

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
| `maxBackStack` | `int`    | Maximum number of saved historical positions.                |

---

### Waypoint System

| Field              | Type     | Description                                                                                |
|--------------------|----------|--------------------------------------------------------------------------------------------|
| `warpCommand`      | `string` | Command name to teleport to a waypoint (default `/ptpwarp`).                               |
| `maxInactiveTicks` | `int`    | The cooldown time before a waypoint is deleted after its associated beacon is deactivated. |
| `checkPeriodTicks` | `int`    | The interval for checking waypoint-to-beacon matching.                                     |
 
---

### Cost Calculation Settings

#### Currency

| Field          | Type     | Description                                              |
|----------------|----------|----------------------------------------------------------|
| `currencyItem` | `string` | The item ID used as currency, e.g., `minecraft:diamond`. |

#### Price Range and Algorithm

| Field       | Type     | Description                                                                                         |
|-------------|----------|-----------------------------------------------------------------------------------------------------|
| `minPrice`  | `int`    | Forced final lower bound. Must be non-negative.                                                      |
| `maxPrice`  | `int`    | Forced final upper bound. Must be at least `minPrice`. Setting it to `0` disables price calculation. |
| `algorithm` | `string` | A JEXL script that calculates distance and raw price and must return an `int`.                        |
 
---

### Settings

#### Effects

| Field            | Type      | Description                                   |
|------------------|-----------|-----------------------------------------------|
| `particleEffect` | `boolean` | Enable particle effects during teleportation. |
| `soundEffect`    | `boolean` | Enable sound effects during teleportation.    |

#### Feature Flags

| Field                  | Type      | Description                              |
|------------------------|-----------|------------------------------------------|
| `allowEnderChest`      | `boolean` | Allow using currency from Ender Chests.  |
| `prioritizeEnderChest` | `boolean` | Prioritize deduction from Ender Chests.  |
| `allowShulkerBox`      | `boolean` | Allow using currency from Shulker Boxes. |
| `prioritizeShulkerBox` | `boolean` | Prioritize deduction from Shulker Boxes. |

---

## JEXL Price Algorithm

The `price.algorithm` string contains both the distance calculation and the price calculation. There are no fixed Java-side distance formulas or teleport multipliers. A script may be as simple as `10`, which returns a fixed price of 10.

### Available Variables

| Variable        | Type     | Description                                                                 |
|-----------------|----------|-----------------------------------------------------------------------------|
| `fromX`         | `double` | X coordinate before teleportation.                                          |
| `fromY`         | `double` | Y coordinate before teleportation.                                          |
| `fromZ`         | `double` | Z coordinate before teleportation.                                          |
| `fromDimension` | `string` | Source dimension ID, e.g., `minecraft:overworld`.                           |
| `toX`           | `double` | Destination X coordinate.                                                   |
| `toY`           | `double` | Destination Y coordinate.                                                   |
| `toZ`           | `double` | Destination Z coordinate.                                                   |
| `toDimension`   | `string` | Destination dimension ID.                                                   |
| `teleportType`  | `string` | One of `coordinate`, `request`, `home`, `back`, or `warp`.                  |
| `player`        | `string` | Name of the player being teleported.                                        |
| `otherPlayer`   | `string` | Name of the other player involved in a request, or an empty string if none. |

`crossDimension` is intentionally not provided. Determine it inside the script:

```jexl
var crossDimension = fromDimension != toDimension;
```

### Return Value and Built-in Methods

- The last expression must return an `int`. Integer literals such as `10` already satisfy this requirement.
- Java's built-in `Math` methods are available through the `math` namespace, for example `math:sqrt(...)`, `math:max(...)`, and `math:round(...)`.
- JEXL runs in strict mode, so undefined variables and invalid expressions are treated as errors.
- Some methods return another numeric type. Convert it explicitly when necessary:

```jexl
math:round(rawPrice).intValue();
```

### Example Algorithm

```jexl
var basePrice = 1;
var baseRadius = 10.0;
var pricePerBlock = 0.01;
var crossDimensionMultiplier = 1.5;
var homeMultiplier = 0.5;
var backMultiplier = 0.8;
var warpMultiplier = 0.5;

var deltaX = fromX - toX;
var deltaY = fromY - toY;
var deltaZ = fromZ - toZ;
var distance = math:sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
var multiplier = fromDimension != toDimension ? crossDimensionMultiplier : 1.0;

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
2. Otherwise, the script result is forcibly clamped to the inclusive range `[minPrice, maxPrice]`.
3. The script is compiled and test-executed when the configuration is validated. In Mod Menu, invalid input is marked red and prevents saving.
4. If a custom algorithm fails during an actual teleport, PayTp logs the error and evaluates the default algorithm instead.

---

## Cloth Config Support

If the **Cloth Config API** is installed, all settings can be adjusted directly through the in-game Mod Menu GUI. The price algorithm can be edited in a dedicated multi-line editor or imported from a `.jexl` file. Confirming an edit or importing a file immediately validates its compilation and integer output; invalid algorithms cannot be saved. (World restart may be required.)

---

## Compatibility & Deployment

| Type                     | Supported                            |
|--------------------------|--------------------------------------|
| Fabric Loader            | ✅                                    |
| Server Only              | ✅                                    |
| Client UI (Cloth Config) | ✅                                    |
| Multi-language Support   | en_us / zh_cn / zh_tw                |
| Minecraft Version        | 26.1+<br/>1.21.4 ~ 1.21.11 (legacy)  |

---

## Credits

This mod is inspired by early economy-style teleport plugins. The request logic references the **Teleport Command** mod. The waypoint logic references the **Beacon Waypoint** mod.   
Developed using Fabric API and fully compatible with vanilla saves.  
Feel free to submit issues or pull requests on GitHub to improve configuration and calculation algorithms.
