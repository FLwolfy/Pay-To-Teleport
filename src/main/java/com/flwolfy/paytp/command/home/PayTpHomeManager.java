package com.flwolfy.paytp.command.home;

import com.flwolfy.paytp.data.PayTpData;
import com.flwolfy.paytp.util.PayTpMessageSender;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.LevelData;
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
   * @param setRespawnPoint whether the player's respawn point should follow the home
   */
  public void setHome(ServerPlayer player, boolean setRespawnPoint) {
    ServerLevel overworld = player.level().getServer().overworld();
    PayTpData home = new PayTpData(player.level().dimension(), player.position());
    getState(overworld).setHome(player.getUUID(), home.pos(), home.world());

    if (setRespawnPoint) {
      player.setRespawnPosition(
          new ServerPlayer.RespawnConfig(
              LevelData.RespawnData.of(
                  home.world(),
                  BlockPos.containing(home.pos()),
                  player.getYRot(),
                  player.getXRot()
              ),
              true
            ),
          false
      );
    }
  }

  /**
   * Applies vanilla bed stand-up position resolution when a player respawns at their Home.
   */
  public void handleRespawn(
      ServerPlayer oldPlayer,
      ServerPlayer newPlayer,
      boolean alive
  ) {
    if (alive) return;

    PayTpData home = getHome(oldPlayer);
    ServerPlayer.RespawnConfig respawnConfig = oldPlayer.getRespawnConfig();
    if (home == null
        || respawnConfig == null
        || !respawnConfig.respawnData().dimension().equals(home.world())
        || !respawnConfig.respawnData().pos().equals(BlockPos.containing(home.pos()))) {
      return;
    }

    ServerLevel homeWorld = newPlayer.level().getServer().getLevel(home.world());
    Vec3 bedLikeRespawn = homeWorld == null
        ? null
        : BedBlock.findStandUpPosition(
            EntityTypes.PLAYER,
            homeWorld,
            BlockPos.containing(home.pos()),
            Direction.fromYRot(respawnConfig.respawnData().yaw()),
            respawnConfig.respawnData().yaw()
        ).orElse(null);

    if (bedLikeRespawn == null) {
      newPlayer.setRespawnPosition(null, false);
      newPlayer.teleport(TeleportTransition.createDefault(
          newPlayer,
          TeleportTransition.DO_NOTHING
      ));
      PayTpMessageSender.msgHomeRespawnUnsafe(newPlayer);
      return;
    }

    newPlayer.teleport(new TeleportTransition(
        homeWorld,
        bedLikeRespawn,
        Vec3.ZERO,
        respawnConfig.respawnData().yaw(),
        respawnConfig.respawnData().pitch(),
        TeleportTransition.DO_NOTHING
    ));
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
