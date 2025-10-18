package com.flwolfy.paytp.command;

import com.flwolfy.paytp.PayTpMod;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import org.slf4j.Logger;


public class PayTpHomeManager {

  private static final Logger LOGGER = PayTpMod.LOGGER;
  private static final String PERSISTENT_STATE_ID = "paytp_home_state";

  private static PayTpHomeManager instance;
  private PayTpHomeManager() {}

  public static PayTpHomeManager getInstance() {
    if (instance == null) {
      instance = new PayTpHomeManager();
    }
    return instance;
  }

  public record PayTpHomeData(Vec3d pos, RegistryKey<World> dimension) {}

  private PayTpHomeState getState(ServerWorld world) {
    PersistentStateManager manager = world.getPersistentStateManager();
    return manager.getOrCreate(PayTpHomeState.TYPE, PERSISTENT_STATE_ID);
  }

  public void setHome(ServerPlayerEntity player) {
    MinecraftServer server = player.getServer();
    if (server != null) {
      ServerWorld overworld = player.getServer().getOverworld();
      getState(overworld).setHome(player.getUuid(), player.getPos(), player.getServerWorld().getRegistryKey());
    } else {
      LOGGER.warn("Failed to set home state, server is null");
    }
  }

  public PayTpHomeData getHome(ServerPlayerEntity player) {
    MinecraftServer server = player.getServer();
    if (server != null) {
      ServerWorld overworld = player.getServer().getOverworld();
      return getState(overworld).getHome(player.getUuid());
    } else {
      LOGGER.warn("Failed to set home state, server is null");
      return null;
    }
  }

  public boolean hasHome(ServerPlayerEntity player) {
    MinecraftServer server = player.getServer();
    if (server != null) {
      ServerWorld overworld = player.getServer().getOverworld();
      return getState(overworld).hasHome(player.getUuid());
    } else {
      LOGGER.warn("Failed to set home state, server is null");
      return false;
    }
  }
}
