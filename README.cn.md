# PayTp 模组使用文档

See the English document [here](./README.md).

---

## 简介

**PayTp** 是一款轻量但功能齐全的“付费传送”模组，让玩家在服务器或单人世界中使用物品作为货币进行传送。  
支持以下特性：

- 玩家间传送请求系统
- 家（Home）与回溯（Back）功能
- 自定义传送价格算法
- 末影箱 / 潜影盒 支付支持
- Cloth Config 图形化配置界面（客户端）
- 可作为 **纯服务器端模组** 使用（Server-Side Only）

---

## 可用命令总览

*指令中<>表示必填参数，()表示可选参数*

| 命令                      | 功能              |
|-------------------------|-----------------|
| `/ptp`                  | 指令指南。           |
| `/ptp (维度) <x> <y> <z>` | 传送到（指定维度的）指定坐标。 |
| `/ptpto <玩家>`           | 请求传送到指定玩家。      |
| `/ptphere <玩家>`         | 请求对方传送至自己当前位置。  |
| `/ptpaccept (玩家)`       | 接受（指定玩家的）传送请求。  |
| `/ptpdeny (玩家)`         | 拒绝（指定玩家的）传送请求。  |
| `/ptpcancel (玩家)`       | 取消（指定玩家的）传送请求。  |
| `/ptpback`              | 回到上一次传送点。       |
| `/ptphome`              | 回家。             |
| `/ptphome set`          | 设置家为当前位置。       |

---

## 配置文件结构

### 配置文件路径：

```
~/config/paytp.json
```

### 默认配置：

```json
{
  "general": {
    "language": "en_us",
    "mainCommand": "ptp",
    "crossDimMultiplier": 1.5
  },
  "request": {
    "requestCommand": {
      "toommand": "ptpto",
      "hereCommand": "ptphere",
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

## 配置部分详解

### 通用设置

| 字段名                  | 类型       | 说明                                           |
|----------------------|----------|----------------------------------------------|
| `language`           | `string` | 语言文件（如 `zh_cn`, `en_us`, `zh_tw`），影响提示与帮助信息。 |
| `mainCommand`        | `string` | 主命令名，例如 `/ptp`。                              |
| `crossDimMultiplier` | `double` | 跨维度传送的价格倍率，例如 1.5 表示多付 1.5 倍价格。              |

---

### 传送请求系统

#### 请求命令

| 字段名             | 类型       | 说明                                 |
|-----------------|----------|------------------------------------|
| `toCommand`     | `string` | 请求传送至对方的命令（默认 `/ptpaccept`）        |
| `hereCommand`   | `string` | 请求对方传送至自己当前位置的命令（默认 `/ptpaccept` ） |
| `acceptCommand` | `string` | 接受请求的命令（默认 `/ptpaccept`）           |
| `denyCommand`   | `string` | 拒绝请求的命令（默认 `/ptpdeny`）             |
| `cancelCommand` | `string` | 取消自己发出的请求（默认 `/ptpcancel`）         |

#### 配置

| 字段名          | 类型    | 说明          |
|--------------|-------|-------------|
| `expireTime` | `int` | 传送请求超时时间（秒） |

---

### 家系统

| 字段名              | 类型       | 说明                   |
|------------------|----------|----------------------|
| `homeCommand`    | `string` | 回家命令（默认 `/ptphome`）  |
| `homeMultiplier` | `double` | 回家传送的倍率，例如 0.5 表示半价。 |

---

### 回溯系统

| 字段名              | 类型       | 说明                        |
|------------------|----------|---------------------------|
| `backCommand`    | `string` | 回到上一个位置的命令（默认 `/ptpback`） |
| `maxBackStack`   | `int`    | 最多可保存的历史位置数量。             |
| `backMultiplier` | `double` | 回溯倍率，例如 0.8 表示 8 折价格。     |

---


### 花费计算设置

#### 货币

| 字段名            | 类型       | 说明                                 |
|----------------|----------|------------------------------------|
| `currencyItem` | `string` | 支付货币的物品 ID，例如 `minecraft:diamond`。 |

#### 金额参数

| 字段名          | 类型       | 说明             |
|--------------|----------|----------------|
| `minPrice`   | `int`    | 最低价格。          |
| `maxPrice`   | `int`    | 单次传送最高上限。      |
| `baseRadius` | `double` | 在此半径内传送为最低价。   |
| `rate`       | `double` | 超出基础半径后的距离增长率。 |

---

### 设置项

#### 效果

| 字段名              | 类型        | 说明            |
|------------------|-----------|---------------|
| `particleEffect` | `boolean` | 是否启用传送时的粒子效果。 |

#### 特性开关

| 字段名                    | 类型        | 说明             |
|------------------------|-----------|----------------|
| `allowEnderChest`      | `boolean` | 是否允许使用末影箱中的货币。 |
| `prioritizeEnderChest` | `boolean` | 是否优先从末影箱扣款。    |
| `allowShulkerBox`      | `boolean` | 是否允许使用潜影盒中的货币。 |
| `prioritizeShulkerBox` | `boolean` | 是否优先从潜影盒扣款。    |

---

## 价格计算公式

### 计算逻辑说明

1. **计算距离**：

    - 同一维度：
        - 直接使用欧氏距离。
    - 维度间传送：
        - 从主世界 × 8 → 下界；
        - 从下界 × 1.25 → 主世界；
        - 进入/离开末地：使用其中位于末地的坐标到末地中心 `(0,0,0)` 的距离。
    - 其他维度：
        - 暂时仅使用欧氏距离计算，如果你要魔改源码，可以在`PayTpCalculator`中的`calculatePrice`进行设置。

2. **计算公式**：

   ```
   distanceBeyondBase = max(0, distance - baseRadius)
   rawPrice = minPrice + distanceBeyondBase * increaseRate
   totalPrice = rawPrice * externalMultiplier
   ```

3. **最终价格**：

   ```
   price = min(totalPrice, maxPrice)
   ```

---

### 价格计算示例

假设为默认`price`配置：

```json
{
   "minPrice": 1,
   "maxPrice": 64,
   "baseRadius": 10.0,
   "rate": 0.01,
   "crossDimMultiplier": 1.5
}
```

- 玩家 A 传送至玩家 B（同一世界，距离 200 格）：
  ```
  超出距离 = 200 - 10 = 190
  价格 = (1 + 190 × 0.01) × 1.0 = 2.9 → 四舍五入为 3
  ```
- 跨维度传送：
  ```
  价格 = 3 × 1.5 = 4.5 → 四舍五入为 5
  ```
- 价格上限自动封顶为 64。

---

## Cloth Config 支持

如果安装了 **Cloth Config API**，可以在游戏内通过 Mod Menu 图形界面直接调整所有配置项。（需重启世界）。

---

## 兼容性与部署

| 类型                    | 支持                     |
|-----------------------|------------------------|
| Fabric Loader         | ✅                      |
| Server Only           | ✅                      |
| 客户端 UI (Cloth Config) | ✅                      |
| 多语言支持                 | en_us / zh_cn / zh_tw  |
| Minecraft 版本          | 1.21.4+                |

---

## 致谢

本模组灵感来自早期经济型传送插件，请求逻辑参考**Teleport Command**模组，使用 Fabric API 编写，兼容原版存档。
欢迎在 GitHub 上提交 Issue 或 PR 改进配置逻辑与算法。

