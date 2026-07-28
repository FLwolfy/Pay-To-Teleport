# PayTp 模组使用文档

**PayTp** 是一款轻量但功能齐全的“付费传送”模组，让玩家在服务器或单人世界中使用物品作为货币进行传送。

See the English document [here](./README.md).

> [!WARNING]
> - *由于从 26.1+ 开始与以往的版本有非常多的 API 修改，并且 Mojang 不再使用**代码混淆**，从 **v1.2.0** 的后续版本（不包括 v1.2.0）将不再支持低于 **26.1** 的 Minecraft 版本。*
> - *如果需要进行**数据迁移**，请查看[这里](./docs/MIGRATION.cn.md)。*

---

## 功能与特性

- **可自定义的指令名**
- 跨维度的指定坐标传送
- 玩家间传送请求系统
- 家（Home）与回溯（Back）功能
- 信标传送点（Warp）功能
- 可完全自定义的 JEXL 传送价格与距离算法
- 末影箱 / 潜影盒 支付支持
- **Cloth Config** 图形化配置界面（客户端）
- 可作为 **纯服务器端模组** 使用（Server-Side Only）

大部分功能可通过将其对应指令名称设置为**空字符串**来**禁用**。
比方说：将配置文件中的 `teleport.coordinateCommand` 从 `ptp` 修改为**空**，可以禁用坐标传送功能。
游戏内帮助文档将会自动适配。

---

## 可用命令总览

*显示均为默认配置下指令名，其中<>表示必填参数，()表示可选参数*

| 命令                      | 功能                   |
|-------------------------|----------------------|
| `/ptphelp`              | 指令指南。                |
| `/ptp (维度) <x> <y> <z>` | 传送到（指定维度的）指定坐标。      |
| `/ptpto <玩家>`           | 请求传送到指定玩家。           |
| `/ptphere <玩家>`         | 请求对方传送至自己当前位置。       |
| `/ptpaccept (玩家)`       | 接受（指定玩家的）传送请求。       |
| `/ptpdeny (玩家)`         | 拒绝（指定玩家的）传送请求。       |
| `/ptpcancel (玩家)`       | 取消（指定玩家的）传送请求。       |
| `/ptpback`              | 回到上一次传送点。            |
| `/ptphome`              | 回家。                  |
| `/ptphome set`          | 设置家为当前位置。            |
| `/ptpwarp <名称>`         | 前往指定名称的全服传送点。        |
| `/ptpwarp create <名称>`  | 创建新的全服传送点（需在激活信标光束内） |
| `/ptpwarp delete <名称>`  | 删除指定名称的全服传送点         |
| `/ptpwarp list (页数)`    | 查看全服所有的传送点           |

---

## 配置文件结构

### 配置文件路径：

```
~/config/paytp.json
```

### 示例配置：

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

## 配置部分详解

### 通用设置

| 字段名          | 类型       | 说明                                           |
|--------------|----------|----------------------------------------------|
| `language`   | `string` | 语言文件（如 `zh_cn`, `en_us`, `zh_tw`），影响提示与帮助信息。 |
| `helpCommand` | `string` | 显示 PayTp 指令指南的命令（默认 `/ptphelp`）。              |

---

### 坐标传送

| 字段名                 | 类型        | 说明                                             |
|---------------------|-----------|------------------------------------------------|
| `coordinateCommand` | `string`  | 坐标传送命令（默认 `/ptp`）。                            |
| `allowCrossDim`     | `boolean` | 是否允许所有传送方式跨越维度。关闭时不会注册或显示坐标命令的维度参数。         |

---

### 传送请求系统

#### 请求命令

| 字段名             | 类型       | 说明                                 |
|-----------------|----------|------------------------------------|
| `toCommand`     | `string` | 请求传送至对方的命令（默认 `/ptpto`）。          |
| `hereCommand`   | `string` | 请求对方传送至自己当前位置的命令（默认 `/ptphere`）。 |
| `acceptCommand` | `string` | 接受请求的命令（默认 `/ptpaccept`）           |
| `denyCommand`   | `string` | 拒绝请求的命令（默认 `/ptpdeny`）             |
| `cancelCommand` | `string` | 取消自己发出的请求（默认 `/ptpcancel`）         |

#### 配置

| 字段名          | 类型    | 说明          |
|--------------|-------|-------------|
| `expireTime` | `int` | 传送请求超时时间（秒） |

---

### 家系统

| 字段名           | 类型       | 说明                  |
|---------------|----------|---------------------|
| `homeCommand` | `string` | 回家命令（默认 `/ptphome`）。 |

---

### 回溯系统

| 字段名            | 类型       | 说明                        |
|----------------|----------|---------------------------|
| `backCommand`  | `string` | 回到上一个位置的命令（默认 `/ptpback`）。 |
| `maxBackStack` | `int`    | 最多可保存的历史位置数量。             |

---

### 传送点系统

| 字段名                | 类型       | 说明                            |
|--------------------|----------|-------------------------------|
| `warpCommand`      | `string` | 前往传送点的命令（默认 `/ptpwarp`）       |
| `maxInactiveTicks` | `int`    | 当绑定的传送点对应信标失效后，该传送点被删除前的冷却时间。 |
| `checkPeriodTicks` | `int`    | 传送点与信标的匹配检查间隔时间。              |

---


### 花费计算设置

#### 货币

| 字段名            | 类型       | 说明                                 |
|----------------|----------|------------------------------------|
| `currencyItem` | `string` | 支付货币的物品 ID，例如 `minecraft:diamond`。 |

#### 价格范围与算法

| 字段名          | 类型       | 说明                                      |
|--------------|----------|-----------------------------------------|
| `minPrice`   | `int`    | 强制控制的最终价格下限，必须为非负整数。                   |
| `maxPrice`   | `int`    | 强制控制的最终价格上限，必须不小于 `minPrice`；设为 `0` 禁用价格计算。 |
| `algorithm`  | `string` | 同时计算距离和原始价格的 JEXL 脚本，必须返回 `int`。        |

---

### 设置项

#### 效果

| 字段名              | 类型        | 说明            |
|------------------|-----------|---------------|
| `particleEffect` | `boolean` | 是否启用传送时的粒子效果。 |
| `soundEffect`    | `boolean` | 是否启用传送时的声音效果。 |

#### 特性开关

| 字段名                    | 类型        | 说明             |
|------------------------|-----------|----------------|
| `allowEnderChest`      | `boolean` | 是否允许使用末影箱中的货币。 |
| `prioritizeEnderChest` | `boolean` | 是否优先从末影箱扣款。    |
| `allowShulkerBox`      | `boolean` | 是否允许使用潜影盒中的货币。 |
| `prioritizeShulkerBox` | `boolean` | 是否优先从潜影盒扣款。    |

---

## JEXL 价格算法

`price.algorithm` 长文本同时负责距离和价格计算，Java 端不再提供固定的距离公式或传送倍率。脚本可以简单到只写 `10`，此时原始价格固定返回 10。

### 可用参数

| 参数名             | 类型       | 说明                                                   |
|-----------------|----------|------------------------------------------------------|
| `fromX`         | `double` | 传送前的 X 坐标。                                           |
| `fromY`         | `double` | 传送前的 Y 坐标。                                           |
| `fromZ`         | `double` | 传送前的 Z 坐标。                                           |
| `fromDimension` | `string` | 起点维度 ID，例如 `minecraft:overworld`。                    |
| `toX`           | `double` | 目标 X 坐标。                                             |
| `toY`           | `double` | 目标 Y 坐标。                                             |
| `toZ`           | `double` | 目标 Z 坐标。                                             |
| `toDimension`   | `string` | 目标维度 ID。                                             |
| `teleportType`  | `string` | `coordinate`、`request`、`home`、`back` 或 `warp`。       |
| `player`        | `string` | 被传送玩家的名称。                                           |
| `otherPlayer`   | `string` | 请求中另一位玩家的名称；不存在时为空字符串。                              |

脚本不会收到 `crossDimension` 参数，需要现场判断：

```jexl
var crossDimension = fromDimension != toDimension;
```

### 返回值与内置方法

- 最后一个表达式必须返回 `int`。整数常量（例如 `10`）本身就是有效返回值。
- 可以通过 `math` 命名空间直接使用 Java 内置 `Math` 方法，例如 `math:sqrt(...)`、`math:max(...)` 和 `math:round(...)`。
- JEXL 使用严格模式，未定义变量和无效表达式都会被视为错误。
- 某些方法会返回其他数值类型，必要时需要显式转换：

```jexl
math:round(rawPrice).intValue();
```

### 算法示例

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

默认算法还包含下界坐标缩放与末地距离处理。新建配置文件时会写入完整默认算法，可直接以其为模板修改。

### 校验与最终价格

1. 当 `maxPrice` 为 `0` 时不会执行算法，所有传送价格均为 `0`。
2. 其他情况下，脚本结果会被强制限制在 `[minPrice, maxPrice]`（包含边界）内。
3. 配置校验时会编译并测试执行脚本。在 Mod Menu 中，无效输入会标红并阻止保存。
4. 自定义算法在实际传送中执行失败时，PayTp 会记录错误并改用默认算法计算。

---

## Cloth Config 支持

如果安装了 **Cloth Config API**，可以在游戏内通过 Mod Menu 图形界面直接调整所有配置项。价格算法支持在独立的多行编辑页面中修改，也可以导入 `.jexl` 文件；确认编辑或导入后会立即校验编译与整数输出，无效算法无法保存。（需重启世界）。

---

## 兼容性与部署

| 类型                    | 支持                               |
|-----------------------|----------------------------------|
| Fabric Loader         | ✅                                |
| Server Only           | ✅                                |
| 客户端 UI (Cloth Config) | ✅                                |
| 多语言支持                 | en_us / zh_cn / zh_tw            |
| Minecraft 版本          | 26.1+<br/>1.21.4 ~ 1.21.11（不再更新） |

---

## 致谢

本模组灵感来自早期经济型传送插件，请求逻辑参考**Teleport Command**模组，传送点系统参考**Beacon Waypoint**模组。使用 Fabric API 编写，兼容原版存档。
欢迎在 GitHub 上提交 Issue 或 PR 改进配置逻辑与算法。
