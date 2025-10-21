package com.flwolfy.paytp.command;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.PayTpData;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentStateManager;

import org.slf4j.Logger;

public class PayTpHomeManager {

  private static final Logger LOGGER = PayTpMod.LOGGER;

  private static PayTpHomeManager instance;
  private PayTpHomeManager() {}

  public static PayTpHomeManager getInstance() {
    if (instance == null) {
      instance = new PayTpHomeManager();
    }
    return instance;
  }

  private PayTpHomeState getState(ServerWorld world) {
    PersistentStateManager manager = world.getPersistentStateManager();
    return manager.getOrCreate(PayTpHomeState.TYPE);
  }

  public void setHome(ServerPlayerEntity player) {
    MinecraftServer server = player.getServer();
    if (server != null) {
      ServerWorld overworld = player.getServer().getOverworld();
      getState(overworld).setHome(player.getUuid(), player.getPos(), player.getWorld().getRegistryKey());
    } else {
      LOGGER.warn("Failed to set home state, server is null");
    }
  }

  public PayTpData getHome(ServerPlayerEntity player) {
    MinecraftServer server = player.getServer();
    if (server != null) {
      ServerWorld overworld = player.getServer().getOverworld();
      return getState(overworld).getHome(player.getUuid());
    } else {
      LOGGER.warn("Failed to get home state, server is null");
      return null;
    }
  }

  public boolean hasHome(ServerPlayerEntity player) {
    MinecraftServer server = player.getServer();
    if (server != null) {
      ServerWorld overworld = player.getServer().getOverworld();
      return getState(overworld).hasHome(player.getUuid());
    } else {
      LOGGER.warn("Failed to check home state, server is null");
      return false;
    }
  }
}
