package com.flwolfy.paytp.command;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.PayTpData;
import java.util.function.Consumer;
import net.minecraft.block.BeaconBlock;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentStateManager;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class PayTpWarpManager {

  private static final Logger LOGGER = PayTpMod.LOGGER;
  private static final int DEFAULT_MAX_INACTIVE_TICKS = 100;
  private static final int DEFAULT_CHECK_PERIOD_TICKS = 20;

  private final Map<String, Integer> warpTimers = new HashMap<>();

  private int maxInactiveTicks;
  private int checkPeriodTicks;
  private int tickCounter;

  private static PayTpWarpManager instance;
  private PayTpWarpManager() {}

  public static PayTpWarpManager getInstance() {
    if (instance == null) {
      instance = new PayTpWarpManager();
      instance.maxInactiveTicks = DEFAULT_MAX_INACTIVE_TICKS;
      instance.checkPeriodTicks = DEFAULT_CHECK_PERIOD_TICKS;
    }
    return instance;
  }

  public void setCheckPeriodTicks(int checkPeriodTicks) {
    this.checkPeriodTicks = checkPeriodTicks;
  }

  public void setMaxInactiveTicks(int maxInactiveTicks) {
    this.maxInactiveTicks = maxInactiveTicks;
  }

  private PayTpWarpState getState(ServerWorld world) {
    PersistentStateManager manager = world.getPersistentStateManager();
    return manager.getOrCreate(PayTpWarpState.TYPE, PayTpWarpState.STATE_ID);
  }

  // =================== //
  // ====== Warp ======= //
  // =================== //

  public void checkWarpState(MinecraftServer server, Consumer<String> onRemove) {
    tickCounter++;
    if (tickCounter % checkPeriodTicks != 0) {
      return;
    } else {
      tickCounter = 0;
    }

    ServerWorld storageWorld = server.getOverworld();
    Map<String, PayTpData> warps = new HashMap<>(getState(storageWorld).getAllWarps());

    for (Map.Entry<String, PayTpData> entry : warps.entrySet()) {
      String name = entry.getKey();
      PayTpData beaconData = getState(storageWorld).getBeacon(name);
      if (beaconData == null) continue;

      ServerWorld warpWorld = storageWorld.getServer().getWorld(beaconData.world());
      if (warpWorld == null) continue;

      BlockPos pos = new BlockPos(
          (int) Math.round(beaconData.pos().x),
          (int) Math.round(beaconData.pos().y),
          (int) Math.round(beaconData.pos().z)
      );

      if (!warpWorld.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
        continue;
      }

      boolean hasBeam = false;
      if (warpWorld.getBlockState(pos).getBlock() instanceof BeaconBlock) {
        BeaconBlockEntity beaconEntity = (BeaconBlockEntity) warpWorld.getBlockEntity(pos);
        if (beaconEntity != null && !beaconEntity.getBeamSegments().isEmpty()) {
          hasBeam = true;
        }
      } else {
        LOGGER.info("Warp {} removed: beacon missing.", name);
        getState(storageWorld).removeWarp(name);
        warpTimers.remove(name);
        onRemove.accept(name);
      }

      if (hasBeam) {
        warpTimers.put(name, 0);
      } else {
        int ticks = warpTimers.getOrDefault(name, 0) + checkPeriodTicks;
        if (ticks >= maxInactiveTicks) {
          LOGGER.info("Warp {} removed: beacon inactive > {}s.", name, maxInactiveTicks / 20);
          getState(storageWorld).removeWarp(name);
          warpTimers.remove(name);
          onRemove.accept(name);
        } else {
          warpTimers.put(name, ticks);
        }
      }
    }
  }

  public boolean createWarp(ServerPlayerEntity player, String name) {
    MinecraftServer server = player.getServer();
    if (server == null) {
      LOGGER.error("Create warp: Server is null.");
      return false;
    }

    ServerWorld world = server.getOverworld();
    BlockPos playerPos = player.getBlockPos();

    BeaconBlockEntity beaconEntity = null;
    BlockPos beaconPos = null;

    for (int y = playerPos.getY() - 1; y >= world.getBottomY(); y--) {
      BlockPos pos = new BlockPos(playerPos.getX(), y, playerPos.getZ());
      if (world.getBlockState(pos).getBlock() instanceof BeaconBlock) {
        beaconEntity = (BeaconBlockEntity) world.getBlockEntity(pos);
        beaconPos = pos;
        break;
      }
    }

    if (beaconEntity == null || beaconEntity.getBeamSegments().isEmpty()) {
      return false;
    }

    PayTpData warpData = new PayTpData(world.getRegistryKey(), player.getPos());
    PayTpData beaconData = new PayTpData(
        world.getRegistryKey(),
        new Vec3d(beaconPos.getX(), beaconPos.getY(), beaconPos.getZ())
    );

    return getState(world).setWarp(name, warpData, beaconData);
  }

  public boolean hasWarp(ServerPlayerEntity player, String name) {
    MinecraftServer server = player.getServer();
    if (server != null) {
      ServerWorld overworld = server.getOverworld();
      return getState(overworld).hasWarp(name);
    } else {
      LOGGER.warn("Failed to check warp, server is null");
    }
    return false;
  }

  public boolean deleteWarp(ServerPlayerEntity player, String name) {
    MinecraftServer server = player.getServer();
    if (server != null) {
      ServerWorld overworld = server.getOverworld();
      return getState(overworld).removeWarp(name);
    } else {
      LOGGER.warn("Failed to delete warp, server is null");
    }
    return false;
  }

  public PayTpData getWarp(ServerPlayerEntity player, String name) {
    MinecraftServer server = player.getServer();
    if (server != null) {
      ServerWorld overworld = server.getOverworld();
      return getState(overworld).getWarp(name);
    } else {
      LOGGER.warn("Failed to get warp, server is null");
      return null;
    }
  }

  public Map<String, PayTpData> getAllWarps(ServerPlayerEntity player) {
    MinecraftServer server = player.getServer();
    if (server != null) {
      ServerWorld overworld = server.getOverworld();
      return getState(overworld).getAllWarps();
    } else {
      LOGGER.warn("Failed to get all warps, server is null");
      return null;
    }
  }
}
