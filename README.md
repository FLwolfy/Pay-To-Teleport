# PayTp Mod Documentation

中文文档请看[这里](./README.cn.md)

---

## Overview

**PayTp** is a lightweight Fabric mod for Minecraft that allows players to teleport by paying a certain amount of in-game currency (such as items or balance).  
It supports flexible teleportation modes, multi-language localization, and fully customizable cost rules.

---

## Features
- Pay-to-teleport system
- Configurable teleport cost (item type, amount, etc.)
- Cross-dimension teleport support
- Support for `/back`, `/home`, and `/tpa`-like commands
- Multi-language support (English, Simplified Chinese, Traditional Chinese)
- Full configuration file via Cloth Config and ModMenu

---

## Commands

| Command                        | Description                                                 |
|--------------------------------|-------------------------------------------------------------|
| `/ptp `                        | Get command guide for PayTp.                                |
| `/ptp (dimension) <x> <y> <z>` | Teleport to specified coordinates (in a specific dimension) |
| `/ptpto <player>`              | Send request to teleport to a player                        |
| `/ptphere <player>`            | Send request to a player to teleport to you                 |
| `/ptpaccept (player)`          | Accept a teleport request (from a specific player)          |
| `/ptpdeny (player)`            | Deny a teleport request (from a specific player)            |
| `/ptpcancel (player)`          | Cancel a pending teleport request (to a specific player)    |
| `/ptpback`                     | Return to the previous location                             |
| `/ptphome`                     | Teleport to your home (if configured)                       |
| `/ptphome set`                 | Set your home to your current position                      |

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
    "mainCommand": "ptp",
    "crossDimMultiplier": 1.5
  },
  "request": {
    "requestCommand": {
      "toCommand": "ptpto",
      "here": "ptphere",
      "acceptCommand": "ptpaccept",
      "denyCommand": "ptpdeny",
      "cancelCommand": "ptpcancel"
    },
    "expireTime": 10
  },
  "home": {
    "homeCommand": "ptphome",
    "homeMultiplier": 0.5
  },
  "back": {
    "backCommand": "ptpback",
    "maxBackStack": 10,
    "backMultiplier": 0.8
  },
  "price": {
    "currencyItem": "minecraft:diamond",
    "parameter": {
      "minPrice": 1,
      "maxPrice": 64,
      "baseRadius": 10.0,
      "rate": 0.01
    }
  },
  "setting": {
    "effect": {
      "particleEffect": true
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

| Field                | Type     | Description                                                                                |
|----------------------|----------|--------------------------------------------------------------------------------------------|
| `language`           | `string` | Language file (e.g., `zh_cn`, `en_us`, `zh_tw`), affects messages and help text.           |
| `mainCommand`        | `string` | Main command name, e.g., `/ptp`.                                                           |
| `crossDimMultiplier` | `double` | Price multiplier for cross-dimensional teleportation, e.g., 1.5 means 1.5× the base price. |

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

| Field            | Type     | Description                                           |
|------------------|----------|-------------------------------------------------------|
| `homeCommand`    | `string` | Command to teleport home (default `/ptphome`).        |
| `homeMultiplier` | `double` | Home teleport multiplier, e.g., 0.5 means half price. |

---

### Back System

| Field            | Type     | Description                                                  |
|------------------|----------|--------------------------------------------------------------|
| `backCommand`    | `string` | Command to return to previous location (default `/ptpback`). |
| `maxBackStack`   | `int`    | Maximum number of saved historical positions.                |
| `backMultiplier` | `double` | Back teleport multiplier, e.g., 0.8 means 20% discount.      |

---

### Cost Calculation Settings

#### Currency

| Field          | Type     | Description                                              |
|----------------|----------|----------------------------------------------------------|
| `currencyItem` | `string` | The item ID used as currency, e.g., `minecraft:diamond`. |

#### Parameters

| Field        | Type     | Description                                                |
|--------------|----------|------------------------------------------------------------|
| `minPrice`   | `int`    | Minimum teleport cost.                                     |
| `maxPrice`   | `int`    | Maximum cost per teleport.                                 |
| `baseRadius` | `double` | Radius within which teleportation costs the minimum price. |
| `rate`       | `double` | Distance rate multiplier for cost beyond base radius.      |

---

### Settings

#### Effects

| Field            | Type      | Description                                   |
|------------------|-----------|-----------------------------------------------|
| `particleEffect` | `boolean` | Enable particle effects during teleportation. |

#### Feature Flags

| Field                  | Type      | Description                              |
|------------------------|-----------|------------------------------------------|
| `allowEnderChest`      | `boolean` | Allow using currency from Ender Chests.  |
| `prioritizeEnderChest` | `boolean` | Prioritize deduction from Ender Chests.  |
| `allowShulkerBox`      | `boolean` | Allow using currency from Shulker Boxes. |
| `prioritizeShulkerBox` | `boolean` | Prioritize deduction from Shulker Boxes. |

---

## Price Calculation Formula

### Calculation Logic

1. **Distance Calculation**:

    - Same dimension:
        - Use direct Euclidean distance.
    - Cross-dimension:
        - Overworld × 8 → Nether;
        - Nether × 0.125 → Overworld;
        - Entering/leaving The End: use the distance from whichever coordinate is in The End to the End center `(0,0,0)`.
    - Other dimensions:
        - Use Euclidean distance by default. Can customize in `PayTpCalculator#calculateDistance`.

2. **Formula**:

   ```
   distanceBeyondBase = max(0, distance - baseRadius)
   rawPrice = minPrice + distanceBeyondBase * increaseRate
   totalPrice = rawPrice * externalMultiplier
   ```

3. **Final Price**:

   ```
   price = min(totalPrice, maxPrice)
   ```

---

### Example Calculation

Default `price` configuration:

```json
{
   "minPrice": 1,
   "maxPrice": 64,
   "baseRadius": 10.0,
   "rate": 0.01,
   "crossDimMultiplier": 1.5
}
```


- Player A teleports to Player B (same world, distance 200 blocks):
  ```
  Extra distance = 200 - 10 = 190
  Price = (1 + 190 × 0.01) × 1.0 = 2.9 → rounded to 3
  ```
- Cross-dimensional teleport:
  ```
  Price = 3 × 1.5 = 4.5 → rounded to 5
  ```
- Price is capped at 64 automatically.

---

## Cloth Config Support

If the **Cloth Config API** is installed, all settings can be adjusted directly through the in-game Mod Menu GUI. (World restart may be required.)

---

## Compatibility & Deployment

| Type                     | Supported             |
|--------------------------|-----------------------|
| Fabric Loader            | ✅                     |
| Server Only              | ✅                     |
| Client UI (Cloth Config) | ✅                     |
| Multi-language Support   | en_us / zh_cn / zh_tw |
| Minecraft Version        | 1.21.4+               |

---

## Credits

This mod is inspired by early economy-style teleport plugins. The request logic references the **Teleport Command** mod.  
Developed using Fabric API and fully compatible with vanilla saves.  
Feel free to submit issues or pull requests on GitHub to improve configuration and calculation algorithms.

