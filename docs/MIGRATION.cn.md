# 迁移指南

## 背景

从 Minecraft **26.1** 开始，游戏修改了世界存档数据（SavedData）的存储位置：

| 版本          | 存储位置                                                   |
|:--------------|:-----------------------------------------------------------|
| **26.1 之前** | `<world>/data/`（直接存放）                                |
| **26.1+**     | `<world>/dimensions/minecraft/overworld/data/<namespace>/` |

原版（Vanilla）的世界升级器**只会迁移原版数据文件**，不会迁移模组的数据文件，因此模组保存的数据仍然会留在旧目录中。

本指南将说明如何将已有的 Warp/Home 数据迁移到新的存储格式。

------------------------------------------------------------------------

## 手动迁移

如果你不希望修改源码，可以在启动服务器之前，将旧的数据文件复制到新的目录。

### 旧位置

```text
<world>/data/paytp_warp_state.dat
<world>/data/paytp_home_state.dat
```

### 新位置

如果目录不存在，请先创建：

```text
<world>/dimensions/minecraft/overworld/data/paytp/
```

然后将以下文件复制过去：

```text
paytp_warp_state.dat
paytp_home_state.dat
```

最终目录结构如下：

```text
<world>/
├── data/
│   ├── paytp_warp_state.dat (旧文件)
│   └── paytp_home_state.dat (旧文件)
└── dimensions/
    └── minecraft/
        └── overworld/
            └── data/
                └── paytp/
                    ├── paytp_warp_state.dat
                    └── paytp_home_state.dat
```

> [!NOTE]
> 你也可以直接移动（Move）这些文件，而不是复制（Copy）。
> 模组不会删除旧文件，因此保留旧文件可以方便以后降级到旧版本。

------------------------------------------------------------------------

## 自动迁移（开发者）

如果希望自动完成迁移，可以添加一个迁移器（Migrator），在其中实现迁移逻辑。下面给出了示例代码。

```java
public static void migrate(MinecraftServer server) {
    SavedDataStorage storage = server.overworld().getDataStorage();
    Path legacyDir = server.getWorldPath(LevelResource.DATA);

    // 迁移 Warp 数据
    migrateState(storage, PayTpWarpState.TYPE, legacyDir.resolve("paytp_warp_state.dat"));

    // 迁移 Home 数据
    migrateState(storage, PayTpHomeState.TYPE, legacyDir.resolve("paytp_home_state.dat"));

    // 将来新增数据类型时，可继续在这里调用 migrateState
}

private static <T extends SavedData> void migrateState(
        SavedDataStorage storage,
        SavedDataType<T> type,
        Path legacyFile
) {
    if (!Files.exists(legacyFile) || storage.get(type) != null) {
        return;
    }

    try {
        CompoundTag tag = NbtIo.readCompressed(legacyFile, NbtAccounter.unlimitedHeap());
        type.codec()
                .parse(NbtOps.INSTANCE, tag.get("data"))
                .resultOrPartial(error -> LOGGER.error("Failed to parse legacy saved data {}: {}", legacyFile, error))
                .ifPresent(state -> {
                    storage.set(type, state);
                    LOGGER.info("Imported legacy saved data from {}", legacyFile);
                });
    } catch (IOException e) {
        LOGGER.error("Failed to read legacy saved data {}", legacyFile, e);
    }
}
```

> [!NOTE]
> 应当在服务器初始化时调用迁移器一次，例如：
>
> `ServerLifecycleEvents.SERVER_STARTED.register(PayTpLegacyDataMigrator::migrate);`

### 迁移流程

对于每一种 SavedData（`paytp_warp_state.dat` 和 `paytp_home_state.dat`）：

1. 检查旧格式数据文件是否存在。
2. 检查新格式的 `SavedData` 是否已经存在。
3. 如果尚未迁移：
   - 读取旧版 NBT 数据文件。
   - 使用当前 `SavedDataType` 的 Codec 进行解析。
   - 将解析后的数据写入新的 `SavedDataStorage`。
4. 保留原始旧文件，不进行删除或修改。

该迁移逻辑可以安全地在**每次服务器启动时执行**，因为只有在新格式数据不存在时，才会导入旧数据。
