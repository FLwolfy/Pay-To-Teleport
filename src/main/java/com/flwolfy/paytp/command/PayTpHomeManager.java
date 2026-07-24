package com.flwolfy.paytp.command;

import com.flwolfy.paytp.data.PayTpData;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.SavedDataStorage;

public class PayTpHomeManager {

  private static PayTpHomeManager instance;
  private PayTpHomeManager() {}

  public static PayTpHomeManager getInstance() {
    if (instance == null) {
      instance = new PayTpHomeManager();
    }
    return instance;
  }

  private PayTpHomeState getState(ServerLevel world) {
    SavedDataStorage manager = world.getDataStorage();
    return manager.computeIfAbsent(PayTpHomeState.TYPE);
  }

  // =================== //
  // ====== Home ======= //
  // =================== //

  public void setHome(ServerPlayer player) {
    ServerLevel overworld = player.level().getServer().overworld();
    getState(overworld).setHome(player.getUUID(), player.position(), player.level().dimension());
  }

  public PayTpData getHome(ServerPlayer player) {
    ServerLevel overworld = player.level().getServer().overworld();
    return getState(overworld).getHome(player.getUUID());
  }

  public boolean hasHome(ServerPlayer player) {
    ServerLevel overworld = player.level().getServer().overworld();
    return getState(overworld).hasHome(player.getUUID());
  }
}
