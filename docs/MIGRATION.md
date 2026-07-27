# Migration Guide

## Background

Starting from Minecraft **26.1**, the game changed how world saved data is stored:

| Version | Storage Location |
| :----- | :--------------- |
| **Pre-26.1** | `<world>/data/` (directly) |
| **26.1+** | `<world>/dimensions/minecraft/overworld/data/<namespace>/` |

The vanilla world upgrader **only relocates vanilla files**, leaving mod files stranded in the old location. This guide explains how to migrate your existing warp/home data to the new format.

------------------------------------------------------------------------

## Manual Migration

If you do not want to modify the source code, copy the legacy files into
the new location before starting the server.

### Legacy files

``` text
<world>/data/paytp_warp_state.dat
<world>/data/paytp_home_state.dat
```

### New location

Create the directory if it does not already exist:

``` text
<world>/dimensions/minecraft/overworld/data/paytp/
```

Then copy the files:

``` text
paytp_warp_state.dat
paytp_home_state.dat
```

Result:

``` text
<world>/
├── data/
│   ├── paytp_warp_state.dat (legacy)
│   └── paytp_home_state.dat (legacy)
└── dimensions/
    └── minecraft/
        └── overworld/
            └── data/
                └── paytp/
                    ├── paytp_warp_state.dat
                    └── paytp_home_state.dat
```

> [!NOTE]
> You can also move the files instead of copying. The mod leaves legacy files intact, so downgrading remains possible.

------------------------------------------------------------------------

## Automatic Migration (For Developers)

To automate this process, add a migrator with migration methods. Sample codes are shown below.

``` java
public static void migrate(MinecraftServer server) {
    SavedDataStorage storage = server.overworld().getDataStorage();
    Path legacyDir = server.getWorldPath(LevelResource.DATA);

    // Migrate warp data
    migrateState(storage, PayTpWarpState.TYPE, legacyDir.resolve("paytp_warp_state.dat"));

    // Migrate home data
    migrateState(storage, PayTpHomeState.TYPE, legacyDir.resolve("paytp_home_state.dat"));

    // Add additional migrateState calls here for future data types
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

> [!Note]
> The migrator should be invoked once during server initialization, for
> example in the server started event `ServerLifecycleEvents.SERVER_STARTED.register(PayTpLegacyDataMigrator::migrate);`

### Migration behavior

For each saved data type (`paytp_warp_state.dat` and
`paytp_home_state.dat`):

1.  Check whether the legacy file exists.
2.  Check whether the new-format `SavedData` already exists.
3.  If no migrated data exists:
    -   Read the legacy NBT file.
    -   Decode it using the current `SavedDataType` codec.
    -   Store it into the new `SavedDataStorage`.
4.  Leave the original legacy file untouched.

This migration is safe to execute on every server startup because it
only imports data when the new-format data is absent.
