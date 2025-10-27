package com.flwolfy.paytp.command;

import com.flwolfy.paytp.data.PayTpData;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentStateManager;

public class PayTpHomeManager {

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

  // =================== //
  // ====== Home ======= //
  // =================== //

  public void setHome(ServerPlayerEntity player) {
    ServerWorld overworld = player.getEntityWorld().getServer().getOverworld();
    getState(overworld).setHome(player.getUuid(), player.getEntityPos(), player.getEntityWorld().getRegistryKey());
  }

  public PayTpData getHome(ServerPlayerEntity player) {
    ServerWorld overworld = player.getEntityWorld().getServer().getOverworld();
    return getState(overworld).getHome(player.getUuid());
  }

  public boolean hasHome(ServerPlayerEntity player) {
    ServerWorld overworld = player.getEntityWorld().getServer().getOverworld();
    return getState(overworld).hasHome(player.getUuid());
  }
}
