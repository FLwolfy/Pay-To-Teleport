package com.flwolfy.paytp.command;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.PayTpData;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

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

  private PayTpWarpState getState(ServerLevel world) {
    SavedDataStorage manager = world.getDataStorage();
    return manager.computeIfAbsent(PayTpWarpState.TYPE);
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

    ServerLevel storageWorld = server.overworld();
    Map<String, PayTpData> warps = new HashMap<>(getState(storageWorld).getAllWarps());

    for (Map.Entry<String, PayTpData> entry : warps.entrySet()) {
      String name = entry.getKey();
      PayTpData beaconData = getState(storageWorld).getBeacon(name);
      if (beaconData == null) continue;

      ServerLevel warpWorld = storageWorld.getServer().getLevel(beaconData.world());
      if (warpWorld == null) continue;

      BlockPos pos = new BlockPos(
          (int) Math.round(beaconData.pos().x),
          (int) Math.round(beaconData.pos().y),
          (int) Math.round(beaconData.pos().z)
      );

      if (!warpWorld.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
        continue;
      }

      boolean hasBeam = false;
      if (warpWorld.getBlockState(pos).getBlock() instanceof BeaconBlock) {
        BeaconBlockEntity beaconEntity = (BeaconBlockEntity) warpWorld.getBlockEntity(pos);
        if (beaconEntity != null && !beaconEntity.getBeamSections().isEmpty()) {
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

  public boolean createWarp(ServerPlayer player, String name) {
    MinecraftServer server = player.level().getServer();
    ServerLevel world = server.overworld();
    BlockPos playerPos = player.blockPosition();

    BeaconBlockEntity beaconEntity = null;
    BlockPos beaconPos = null;

    for (int y = playerPos.getY() - 1; y >= world.getMinY(); y--) {
      BlockPos pos = new BlockPos(playerPos.getX(), y, playerPos.getZ());
      if (world.getBlockState(pos).getBlock() instanceof BeaconBlock) {
        beaconEntity = (BeaconBlockEntity) world.getBlockEntity(pos);
        beaconPos = pos;
        break;
      }
    }

    if (beaconEntity == null || beaconEntity.getBeamSections().isEmpty()) {
      return false;
    }

    PayTpData warpData = new PayTpData(world.dimension(), player.position());
    PayTpData beaconData = new PayTpData(
        world.dimension(),
        new Vec3(beaconPos.getX(), beaconPos.getY(), beaconPos.getZ())
    );

    return getState(world).setWarp(name, warpData, beaconData);
  }

  public boolean hasWarp(ServerPlayer player, String name) {
    MinecraftServer server = player.level().getServer();
    ServerLevel overworld = server.overworld();
    return getState(overworld).hasWarp(name);
  }

  public boolean deleteWarp(ServerPlayer player, String name) {
    MinecraftServer server = player.level().getServer();
    ServerLevel overworld = server.overworld();
    return getState(overworld).removeWarp(name);
  }

  public PayTpData getWarp(ServerPlayer player, String name) {
    MinecraftServer server = player.level().getServer();
    ServerLevel overworld = server.overworld();
    return getState(overworld).getWarp(name);
  }

  public Map<String, PayTpData> getAllWarps(ServerPlayer player) {
    MinecraftServer server = player.level().getServer();
    ServerLevel overworld = server.overworld();
    return getState(overworld).getAllWarps();
  }
}
