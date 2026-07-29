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

所有 `<玩家>` 参数只接受在线玩家名称，命令建议不会显示或接受 `@a`、`@p`、`@s` 等实体选择器。
`/ptp` 的 `<x> <y> <z>` 仅作为不可选择的参数格式提示，不会自动补全当前坐标；仍支持 `~` 和 `^` 相对坐标。

| 命令                                                     | 功能                                                  |
|----------------------------------------------------------|-------------------------------------------------------|
| `/ptphelp`                                               | 指令指南。                                            |
| `/ptp (维度) <x> <y> <z>`                                | 传送到（指定维度的）指定坐标。                        |
| `/ptpto <玩家>`                                          | 请求传送到指定玩家。                                  |
| `/ptphere <玩家>`                                        | 请求对方传送至自己当前位置。                          |
| `/ptpaccept (玩家)`                                      | 接受（指定玩家的）传送请求。                          |
| `/ptpdeny (玩家)`                                        | 拒绝（指定玩家的）传送请求。                          |
| `/ptpcancel (玩家)`                                      | 取消（指定玩家的）传送请求。                          |
| `/ptpback`                                               | 回到上一次传送点。                                    |
| `/ptphome`                                               | 回家。                                                |
| `/ptphome set`                                           | 设置家为当前位置。                                    |
| `/ptpwarp <名称>`                                        | 前往指定名称的全服传送点。                            |
| `/ptpwarp create <名称> (public/private/server)`         | 创建传送点，默认 `private`；`server` 受配置权限控制。 |
| `/ptpwarp delete <名称> (forced)`                        | 删除自己的传送点；`forced` 受配置权限控制。           |
| `/ptpwarp rename <名称> <新名称>`                        | 重命名自己创建的传送点。                              |
| `/ptpwarp invite <名称> <玩家>`                          | 邀请玩家使用自己的私人传送点。                        |
| `/ptpwarp exclude <名称> <玩家>`                         | 移除私人传送点中已邀请的玩家。                        |
| `/ptpwarp list (all/public/owned/invited/server) (页数)` | 筛选并分页查看全服传送点。                            |

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
    "homeCommand": "ptphome",
    "setRespawnPoint": false
  },
  "back": {
    "backCommand": "ptpback",
    "maxBackStack": 10
  },
  "warp": {
    "warpCommand": "ptpwarp",
    "serverWarpPermission": 2,
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

## 配置部分详解

### 通用设置

| 字段名          | 类型     | 说明                                                            |
|-----------------|----------|-----------------------------------------------------------------|
| `language`      | `string` | 语言文件 （如 `zh_cn`, `en_us`, `zh_tw`），影响提示与帮助信息。 |
| `helpCommand`   | `string` | 显示 PayTp 指令指南的命令（默认 `/ptphelp`）。                  |

#### 传送效果（`general.effect`）

| 字段名           | 类型      | 说明                                    |
|------------------|-----------|-----------------------------------------|
| `particleEffect` | `boolean` | 是否启用传送时的粒子效果，默认 `true`。 |
| `soundEffect`    | `boolean` | 是否启用传送时的声音效果，默认 `true`。 |

---

### 坐标传送

| 字段名              | 类型      | 说明                                                                                |
|---------------------|-----------|-------------------------------------------------------------------------------------|
| `coordinateCommand` | `string`  | 坐标传送命令（默认 `/ptp`）。                                                       |
| `allowCrossDim`     | `boolean` | 是否允许所有传送方式跨越维度，默认 `true`。关闭时不会注册或显示坐标命令的维度参数。 |

---

### 传送请求系统

#### 请求命令

| 字段名          | 类型     | 说明                                                  |
|-----------------|----------|-------------------------------------------------------|
| `toCommand`     | `string` | 请求传送至对方的命令（默认 `/ptpto`）。               |
| `hereCommand`   | `string` | 请求对方传送至自己当前位置的命令（默认 `/ptphere`）。 |
| `acceptCommand` | `string` | 接受请求的命令（默认 `/ptpaccept`）                   |
| `denyCommand`   | `string` | 拒绝请求的命令（默认 `/ptpdeny`）                     |
| `cancelCommand` | `string` | 取消自己发出的请求（默认 `/ptpcancel`）               |

#### 配置

| 字段名         | 类型    | 说明                                              |
|----------------|---------|---------------------------------------------------|
| `expireTime`   | `int`   | 传送请求超时时间（秒），默认 `10`，不得小于 `0`。 |

---

### 家系统

| 字段名             | 类型      | 说明                                               |
|--------------------|-----------|----------------------------------------------------|
| `homeCommand`      | `string`  | 回家命令（默认 `/ptphome`）。                      |
| `setRespawnPoint`  | `boolean` | 设置家时同时将玩家重生点移动到家，默认为 `false`。 |

---

### 回溯系统

| 字段名         | 类型       | 说明                                                |
|----------------|------------|-----------------------------------------------------|
| `backCommand`  | `string`   | 回到上一个位置的命令（默认 `/ptpback`）。           |
| `maxBackStack` | `int`      | 最多可保存的历史位置数量，默认 `10`，必须大于 `0`。 |

---

### 传送点系统

| 字段名                 | 类型     | 说明                                                                                     |
|------------------------|----------|------------------------------------------------------------------------------------------|
| `warpCommand`          | `string` | 前往传送点的命令（默认 `/ptpwarp`）                                                      |
| `serverWarpPermission` | `int`    | 创建服务器传送点及强制删除传送点所需的 Minecraft 权限等级，范围为 `0`～`4`，默认为 `2`。 |
| `maxInactiveTicks`     | `int`    | 信标失效后删除传送点前的等待 tick 数，默认 `100`，不得小于 `0`。                         |
| `checkPeriodTicks`     | `int`    | 传送点与信标的检查间隔 tick 数，默认 `20`，必须大于 `0`。                                |

---


### 花费计算设置

#### 货币

| 字段名           | 类型     | 说明                                                |
|------------------|----------|-----------------------------------------------------|
| `currencyItem`   | `string` | 支付货币的有效物品 ID，默认为 `minecraft:diamond`。 |

#### 价格范围与算法

| 字段名         | 类型     | 说明                                                                                                 |
|----------------|----------|------------------------------------------------------------------------------------------------------|
| `minPrice`     | `int`    | 非负价格的最终下限，默认 `1`，必须满足 `0 <= minPrice <= maxPrice`。                                 |
| `maxPrice`     | `int`    | 非负价格的最终上限，默认 `64`；将 `minPrice` 和 `maxPrice` 都设为 `0` 时不执行算法且价格固定为 `0`。 |
| `algorithm`    | `string` | 同时计算距离和原始价格的 JEXL 脚本，必须返回 `int`。                                                 |

#### 扣款设置（`price.deduction`）

| 字段名                 | 类型      | 说明                                                                     |
|------------------------|-----------|--------------------------------------------------------------------------|
| `allowEnderChest`      | `boolean` | 是否允许使用末影箱中的货币，默认 `true`。                                |
| `prioritizeEnderChest` | `boolean` | 是否优先从末影箱扣款，默认 `true`；启用前必须先启用 `allowEnderChest`。  |
| `allowShulkerBox`      | `boolean` | 是否允许使用潜影盒中的货币，默认 `false`。                               |
| `prioritizeShulkerBox` | `boolean` | 是否优先从潜影盒扣款，默认 `false`；启用前必须先启用 `allowShulkerBox`。 |

---

## JEXL 价格算法

`price.algorithm` 长文本同时负责距离和价格计算，Java 端不再提供固定的距离公式或传送倍率。脚本可以简单到只写 `10`，此时原始价格固定返回 10。

### 可用参数

| 参数名            | 类型       | 说明                                                         |
|-------------------|------------|--------------------------------------------------------------|
| `from`            | `position` | 传送起点 record，提供 `x()`、`y()`、`z()` 和 `dimension()`。 |
| `to`              | `position` | 传送终点 record，提供 `x()`、`y()`、`z()` 和 `dimension()`。 |
| `teleportContext` | `teleport` | 下表所述的传送类型专属上下文。                               |
| `player`          | `player`   | 被传送玩家 record，提供 `uuid()` 和 `name()`。               |

`teleportContext` 内含五个可空的嵌套 record。每次执行时恰好一个方法返回对应 record，
其余四个均返回 `null`：

| 方法                           | Record 内容                                                                                                                    |
|--------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| `teleportContext.coordinate()` | 坐标指令上下文；目前没有额外字段。                                                                                             |
| `teleportContext.home()`       | Home 上下文；目前没有额外字段。                                                                                                |
| `teleportContext.back()`       | Back 上下文；目前没有额外字段。                                                                                                |
| `teleportContext.request()`    | `otherPlayer()` 返回包含 `uuid()` 与 `name()` 的对方玩家；<br/>`isRequester()` 为 `bool`，表示当前 `player` 是否为请求发起者。 |
| `teleportContext.warp()`       | `name()`、`accessType()` 和 `owner()`；创建者提供 `uuid()` 与 `name()`。                                                       |

当当前 `player` 是请求发起者时，`isRequester()` 为 `true`；当其接受请求并传送到发起者身边时为
`false`。Warp 的 `accessType()` 为 `owned`、`invited`、`server` 或 `public`；
没有创建者的服务器传送点，其 `owner()` 为 `null`。

脚本不会收到 `crossDimension` 参数，需要现场判断：

```jexl
var crossDimension = from.dimension() != to.dimension();
```

### 返回值与严格模式

- 最后一个表达式必须返回 `int`。整数常量（例如 `10`）本身就是有效返回值。
- 返回负整数会取消扣款和传送。算法或 Minecraft 命令结果可以通过这种方式主动停止操作，而不需要制造脚本错误。
- JEXL 使用严格模式，未定义变量和无效表达式都会被视为错误。

### Math 方法

> [!TIP]
> `math` 适合直接在价格算法中完成距离、倍率、取整和边界计算，不需要调用外部程序。

`math` 命名空间提供 Java 内置的 `Math` 方法。

| 方法                       | 返回类型  | 说明                       |
|----------------------------|-----------|----------------------------|
| `math:sqrt(value)`         | `double`  | 计算数值的平方根。         |
| `math:max(first, second)`  | 数值类型  | 返回两个数值中的较大值。   |
| `math:min(first, second)`  | 数值类型  | 返回两个数值中的较小值。   |
| `math:round(value)`        | `long`    | 返回最接近的整数。         |
| `math:pow(base, exponent)` | `double`  | 计算指定底数的幂。         |
| `math:abs(value)`          | 数值类型  | 返回绝对值。               |

部分方法返回的数值类型不是 `int`，必要时需要显式转换最终结果：

```jexl
math:round(rawPrice).intValue();
```

### Minecraft 命令

> [!WARNING]
> `minecraft` 命名空间可以执行服务端注册的全部命令，包括其他模组添加的命令。命令以服务器控制台作为执行者，并拥有 `ALL_PERMISSIONS` 权限，因此也能执行 `op`、`ban`、`data` 和 `stop` 等具有破坏性或管理性质的操作。请只使用完全可信的算法。

| 方法                         | 返回类型 | 说明                                                              |
|------------------------------|----------|-------------------------------------------------------------------|
| `minecraft:execute(command)` | `int`    | 执行 Minecraft 命令；开头的 `/` 可省略，且不会显示命令反馈。      |

命令执行源是服务器而不是被传送玩家，因此不能使用 `@s` 指代玩家。需要目标玩家时请使用算法提供的 `player.name()`；涉及位置或维度的命令应显式指定目标、坐标和维度，不要依赖玩家执行上下文：

```jexl
minecraft:execute("scoreboard players add " + player.name() + " teleport_count 1");
minecraft:execute("effect give " + player.name() + " minecraft:regeneration 5 0");
10;
```

配置校验期间，`minecraft:execute(...)` 不会真正执行命令，只会返回 `0`，因此加载或编辑算法不会改变服务器状态。

### Shell 命令

> [!CAUTION]
> `shell` 命名空间会以 Minecraft 服务端进程所拥有的操作系统权限执行任意命令。请只使用完全可信的算法。Shell 命令为同步执行，在结束前会阻塞服务端线程；算法校验时也会执行命令，包括加载配置、编辑和导入算法时。

| 方法                     | 返回类型      | 说明                                                                 |
|--------------------------|---------------|----------------------------------------------------------------------|
| `shell:execute(command)` | `ShellResult` | 类 Unix 系统通过 `/bin/sh -c` 执行，Windows 通过 `cmd.exe /c` 执行。 |
| `shell:run(command)`     | `string`      | 返回标准输出；退出码不为 0 时抛出错误。                              |
| `shell:runInt(command)`  | `int`         | 要求标准输出只包含一个有效整数。                                     |

`ShellResult` 提供 `exitCode`、`stdout` 和 `stderr`：

```jexl
var result = shell:execute("python3 /opt/paytp/price.py");
result.exitCode == 0 ? result.stdoutInt() : 0;
```

价格算法通常可以直接使用 `runInt`：

```jexl
shell:runInt("python3 /opt/paytp/price.py '" + player.name() + "'");
```

### 算法示例

```jexl
// Available variables:
// from, to: positions with .x(), .y(), .z(), and .dimension()
// teleportContext: exactly one of coordinate(), home(), back(), request(), or warp()
// player: the teleported player, with .uuid() and .name()
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

var deltaX = from.x() - to.x();
var deltaY = from.y() - to.y();
var deltaZ = from.z() - to.z();
var distance = math:sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
var multiplier = from.dimension() != to.dimension() ? crossDimensionMultiplier : 1.0;

if (teleportContext.home() != null) {
  multiplier = multiplier * homeMultiplier;
} else if (teleportContext.back() != null) {
  multiplier = multiplier * backMultiplier;
} else if (teleportContext.warp() != null) {
  multiplier = multiplier * warpMultiplier;
}

var distanceBeyondBase = math:max(0, distance - baseRadius);
math:round((basePrice + distanceBeyondBase * pricePerBlock) * multiplier).intValue();
```

默认算法还包含下界坐标缩放与末地距离处理。新建配置文件时会写入完整默认算法，可直接以其为模板修改。

### 校验与最终价格

1. 当 `maxPrice` 为 `0` 时不会执行算法，所有传送价格均为 `0`。
2. 脚本返回负数时会取消扣款和传送，并且不会进行价格范围限制。
3. 其他情况下，脚本结果会被强制限制在 `[minPrice, maxPrice]`（包含边界）内。
4. 配置校验时会编译并测试执行脚本。在 Mod Menu 中，无效输入会标红并阻止保存。
5. 算法在实际传送中执行失败时，`calculatePrice` 会返回 `-1`；PayTp 会记录错误、取消传送，并提示玩家扣款流程出现错误。

---

## Cloth Config 支持

如果安装了 **Cloth Config API**，可以在游戏内通过 Mod Menu 图形界面直接调整所有配置项。价格算法支持在独立的多行编辑页面中修改，也可以导入 `.jexl` 文件；确认编辑或导入后会立即校验编译与整数输出，无效算法无法保存。（需重启世界）。

---

## 兼容性与部署

| 类型                     | 支持                                   |
|--------------------------|----------------------------------------|
| Fabric Loader            | ✅                                     |
| Server Only              | ✅                                     |
| 客户端 UI (Cloth Config) | ✅                                     |
| 多语言支持               | en_us / zh_cn / zh_tw                  |
| Minecraft 版本           | 26.1+<br/>1.21.4 ~ 1.21.11（不再更新） |

---

## 致谢

本模组灵感来自早期经济型传送插件，请求逻辑参考**Teleport Command**模组，传送点系统参考**Beacon Waypoint**模组。使用 Fabric API 编写，兼容原版存档。
欢迎在 GitHub 上提交 Issue 或 PR 改进配置逻辑与算法。
