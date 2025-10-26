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

public class PayTpWrapManager {

  private static final Logger LOGGER = PayTpMod.LOGGER;
  private static final int DEFAULT_MAX_INACTIVE_TICKS = 100;
  private static final int DEFAULT_CHECK_PERIOD_TICKS = 20;

  private final Map<String, Integer> wrapTimers = new HashMap<>();

  private int maxInactiveTicks;
  private int checkPeriodTicks;
  private int tickCounter;

  private static PayTpWrapManager instance;
  private PayTpWrapManager() {}

  public static PayTpWrapManager getInstance() {
    if (instance == null) {
      instance = new PayTpWrapManager();
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

  private PayTpWrapState getState(ServerWorld world) {
    PersistentStateManager manager = world.getPersistentStateManager();
    return manager.getOrCreate(PayTpWrapState.TYPE, PayTpWrapState.STATE_ID);
  }

  // =================== //
  // ====== Wrap ======= //
  // =================== //

  public void checkWrapState(MinecraftServer server, Consumer<String> onRemove) {
    tickCounter++;
    if (tickCounter % checkPeriodTicks != 0) {
      return;
    } else {
      tickCounter = 0;
    }

    ServerWorld storageWorld = server.getOverworld();
    Map<String, PayTpData> wraps = new HashMap<>(getState(storageWorld).getAllWraps());

    for (Map.Entry<String, PayTpData> entry : wraps.entrySet()) {
      String name = entry.getKey();
      PayTpData beaconData = getState(storageWorld).getBeacon(name);
      if (beaconData == null) continue;

      ServerWorld wrapWorld = storageWorld.getServer().getWorld(beaconData.world());
      if (wrapWorld == null) continue;

      BlockPos pos = new BlockPos(
          (int) Math.round(beaconData.pos().x),
          (int) Math.round(beaconData.pos().y),
          (int) Math.round(beaconData.pos().z)
      );

      if (!wrapWorld.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
        continue;
      }

      boolean hasBeam = false;
      if (wrapWorld.getBlockState(pos).getBlock() instanceof BeaconBlock) {
        BeaconBlockEntity beaconEntity = (BeaconBlockEntity) wrapWorld.getBlockEntity(pos);
        if (beaconEntity != null && !beaconEntity.getBeamSegments().isEmpty()) {
          hasBeam = true;
        }
      } else {
        LOGGER.info("Wrap {} removed: beacon missing.", name);
        getState(storageWorld).removeWrap(name);
        wrapTimers.remove(name);
        onRemove.accept(name);
      }

      if (hasBeam) {
        wrapTimers.put(name, 0);
      } else {
        int ticks = wrapTimers.getOrDefault(name, 0) + checkPeriodTicks;
        if (ticks >= maxInactiveTicks) {
          LOGGER.info("Wrap {} removed: beacon inactive > {}s.", name, maxInactiveTicks / 20);
          getState(storageWorld).removeWrap(name);
          wrapTimers.remove(name);
          onRemove.accept(name);
        } else {
          wrapTimers.put(name, ticks);
        }
      }
    }
  }

  public boolean createWrap(ServerPlayerEntity player, String name) {
    MinecraftServer server = player.getServer();
    if (server == null) {
      LOGGER.error("Create wrap: Server is null.");
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

    PayTpData wrapData = new PayTpData(world.getRegistryKey(), player.getPos());
    PayTpData beaconData = new PayTpData(
        world.getRegistryKey(),
        new Vec3d(beaconPos.getX(), beaconPos.getY(), beaconPos.getZ())
    );

    return getState(world).setWrap(name, wrapData, beaconData);
  }

  public boolean hasWrap(ServerPlayerEntity player, String name) {
    MinecraftServer server = player.getServer();
    if (server != null) {
      ServerWorld overworld = server.getOverworld();
      return getState(overworld).hasWrap(name);
    } else {
      LOGGER.warn("Failed to check wrap, server is null");
    }
    return false;
  }

  public boolean deleteWrap(ServerPlayerEntity player, String name) {
    MinecraftServer server = player.getServer();
    if (server != null) {
      ServerWorld overworld = server.getOverworld();
      return getState(overworld).removeWrap(name);
    } else {
      LOGGER.warn("Failed to delete wrap, server is null");
    }
    return false;
  }

  public PayTpData getWrap(ServerPlayerEntity player, String name) {
    MinecraftServer server = player.getServer();
    if (server != null) {
      ServerWorld overworld = server.getOverworld();
      return getState(overworld).getWrap(name);
    } else {
      LOGGER.warn("Failed to get wrap, server is null");
      return null;
    }
  }

  public Map<String, PayTpData> getAllWraps(ServerPlayerEntity player) {
    MinecraftServer server = player.getServer();
    if (server != null) {
      ServerWorld overworld = server.getOverworld();
      return getState(overworld).getAllWraps();
    } else {
      LOGGER.warn("Failed to get all wraps, server is null");
      return null;
    }
  }
}
