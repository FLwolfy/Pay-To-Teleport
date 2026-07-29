package com.flwolfy.paytp.command.home;

import com.flwolfy.paytp.data.PayTpData;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.SavedDataStorage;

/**
 * Provides player-oriented access to persistent home data stored in the overworld.
 */
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

  /**
   * Stores the player's current position and dimension as their home.
   *
   * @param player the player whose home is updated
   */
  public void setHome(ServerPlayer player) {
    ServerLevel overworld = player.level().getServer().overworld();
    getState(overworld).setHome(player.getUUID(), player.position(), player.level().dimension());
  }

  /**
   * Retrieves a player's stored home.
   *
   * @param player the player whose home is requested
   * @return the stored home, or {@code null} when none exists
   */
  public PayTpData getHome(ServerPlayer player) {
    ServerLevel overworld = player.level().getServer().overworld();
    return getState(overworld).getHome(player.getUUID());
  }

  /**
   * Checks whether a player has a stored home.
   *
   * @param player the player to inspect
   * @return {@code true} when a home exists
   */
  public boolean hasHome(ServerPlayer player) {
    ServerLevel overworld = player.level().getServer().overworld();
    return getState(overworld).hasHome(player.getUUID());
  }
}
