package com.flwolfy.paytp.command.warp;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;

/**
 * Manages persistent server warps and monitors their associated beacon beams.
 */
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

  // =============================================
  // Data types for warping management
  // =============================================

  public enum OperationResult {
    SUCCESS,
    NOT_FOUND,
    NOT_OWNER,
    ALREADY_EXISTS,
    PUBLIC_WARP,
    ALREADY_INVITED,
    NOT_INVITED,
    BEACON_INACTIVE,
    BEACON_OCCUPIED,
    SELF_EXCLUDE,
    SERVER_WARP
  }

  public enum AccessType {
    OWNED,
    INVITED,
    SERVER,
    PUBLIC,
    LOCKED
  }

  public record WarpView(
      String name,
      PayTpData destination,
      String ownerName,
      AccessType accessType
  ) {
    public boolean accessible() {
      return accessType != AccessType.LOCKED;
    }
  }

  // =============================================

  /**
   * Sets how often beacon-backed warps are checked.
   *
   * @param checkPeriodTicks the interval in server ticks
   */
  public void setCheckPeriodTicks(int checkPeriodTicks) {
    this.checkPeriodTicks = checkPeriodTicks;
  }

  /**
   * Sets how long an inactive beacon may remain before its warp is removed.
   *
   * @param maxInactiveTicks the inactivity limit in server ticks
   */
  public void setMaxInactiveTicks(int maxInactiveTicks) {
    this.maxInactiveTicks = maxInactiveTicks;
  }

  /**
   * Clears the beacon check interval and all accumulated inactivity durations.
   */
  public void resetTimers() {
    tickCounter = 0;
    warpTimers.clear();
  }

  private PayTpWarpState getState(ServerLevel world) {
    SavedDataStorage manager = world.getDataStorage();
    return manager.computeIfAbsent(PayTpWarpState.TYPE);
  }

  // =================== //
  // ====== Warp ======= //
  // =================== //

  /**
   * Advances beacon monitoring and removes warps whose beacon is missing or inactive too long.
   *
   * @param server the active Minecraft server
   * @param onRemove callback invoked with each removed warp name
   */
  public void checkWarpState(
      MinecraftServer server,
      Consumer<String> onRemove
  ) {
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

  /**
   * Creates a warp at the player's position when the player stands in an active beacon beam.
   *
   * @param player the player creating the warp
   * @param name the globally unique warp name
   * @param publicWarp whether every player may access the warp
   * @return the creation result, including distinct beacon failure reasons
   */
  public OperationResult createWarp(
      ServerPlayer player,
      String name,
      boolean publicWarp
  ) {
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

    if (beaconEntity == null) {
      return OperationResult.BEACON_INACTIVE;
    }

    PayTpData warpData = new PayTpData(world.dimension(), player.position());
    PayTpData beaconData = new PayTpData(
        world.dimension(),
        new Vec3(beaconPos.getX(), beaconPos.getY(), beaconPos.getZ())
    );

    PayTpWarpState state = getState(world);
    if (state.hasBeacon(beaconData)) {
      return OperationResult.BEACON_OCCUPIED;
    }
    if (beaconEntity.getBeamSections().isEmpty()) {
      return OperationResult.BEACON_INACTIVE;
    }

    boolean created = state.setWarp(
        name,
        warpData,
        beaconData,
        player.getUUID(),
        publicWarp
    );
    return created ? OperationResult.SUCCESS : OperationResult.ALREADY_EXISTS;
  }

  /**
   * Creates a warp at the player's current position without binding a beacon.
   *
   * <p>Permission checks are enforced by the command tree. Since no beacon data is stored,
   * beacon monitoring intentionally ignores this warp.</p>
   *
   * @param player the administrator creating the warp
   * @param name the globally unique warp name
   * @return {@link OperationResult#SUCCESS}, or {@link OperationResult#ALREADY_EXISTS}
   */
  public OperationResult createServerWarp(ServerPlayer player, String name) {
    PayTpWarpState state = getState(player.level().getServer().overworld());
    PayTpData warpData = new PayTpData(
        player.level().dimension(),
        player.position()
    );
    boolean created = state.setServerWarp(name, warpData);
    return created ? OperationResult.SUCCESS : OperationResult.ALREADY_EXISTS;
  }

  /**
   * Checks whether a named global warp exists.
   *
   * @param player a player used to resolve the current server
   * @param name the warp name
   * @return {@code true} when the warp exists
   */
  public boolean hasWarp(ServerPlayer player, String name) {
    MinecraftServer server = player.level().getServer();
    ServerLevel overworld = server.overworld();
    return getState(overworld).hasWarp(name);
  }

  /**
   * Deletes a named global warp.
   *
   * @param player a player used to resolve the current server
   * @param name the warp name
   * @return {@code true} if an existing warp was removed
   */
  public boolean deleteWarp(ServerPlayer player, String name) {
    MinecraftServer server = player.level().getServer();
    ServerLevel overworld = server.overworld();
    PayTpWarpState state = getState(overworld);
    return state.isOwner(name, player.getUUID()) && state.removeWarp(name);
  }

  /**
   * Deletes a warp without checking its owner.
   *
   * @param player an administrator used to resolve the current server
   * @param name the globally unique warp name
   * @return {@code true} when an existing warp was removed
   */
  public boolean deleteWarpForced(ServerPlayer player, String name) {
    PayTpWarpState state = getState(player.level().getServer().overworld());
    boolean removed = state.removeWarp(name);
    if (removed) warpTimers.remove(name);
    return removed;
  }

  public OperationResult renameWarp(
      ServerPlayer player,
      String name,
      String newName
  ) {
    PayTpWarpState state = getState(player.level().getServer().overworld());
    if (!state.hasWarp(name)) return OperationResult.NOT_FOUND;
    if (state.isServer(name)) return OperationResult.SERVER_WARP;
    if (!state.isOwner(name, player.getUUID())) {
      return state.canAccess(name, player.getUUID())
          ? OperationResult.NOT_OWNER
          : OperationResult.NOT_FOUND;
    }
    if (state.hasWarp(newName)) return OperationResult.ALREADY_EXISTS;
    if (!state.renameWarp(name, newName)) return OperationResult.NOT_FOUND;

    Integer timer = warpTimers.remove(name);
    if (timer != null) warpTimers.put(newName, timer);
    return OperationResult.SUCCESS;
  }

  public OperationResult invite(
      ServerPlayer owner,
      String name,
      ServerPlayer invitedPlayer
  ) {
    PayTpWarpState state = getState(owner.level().getServer().overworld());
    if (!state.hasWarp(name)) return OperationResult.NOT_FOUND;
    if (!state.isOwner(name, owner.getUUID())) {
      return state.canAccess(name, owner.getUUID())
          ? OperationResult.NOT_OWNER
          : OperationResult.NOT_FOUND;
    }
    if (state.isPublic(name)) return OperationResult.PUBLIC_WARP;
    if (state.isOwner(name, invitedPlayer.getUUID())
        || state.isInvited(name, invitedPlayer.getUUID())) {
      return OperationResult.ALREADY_INVITED;
    }
    state.invite(name, invitedPlayer.getUUID());
    return OperationResult.SUCCESS;
  }

  public OperationResult exclude(
      ServerPlayer owner,
      String name,
      ServerPlayer excludedPlayer
  ) {
    PayTpWarpState state = getState(owner.level().getServer().overworld());
    if (!state.hasWarp(name)) return OperationResult.NOT_FOUND;
    if (!state.isOwner(name, owner.getUUID())) {
      return state.canAccess(name, owner.getUUID())
          ? OperationResult.NOT_OWNER
          : OperationResult.NOT_FOUND;
    }
    if (state.isPublic(name)) return OperationResult.PUBLIC_WARP;
    if (owner.getUUID().equals(excludedPlayer.getUUID())) {
      return OperationResult.SELF_EXCLUDE;
    }
    if (!state.isInvited(name, excludedPlayer.getUUID())) {
      return OperationResult.NOT_INVITED;
    }
    state.exclude(name, excludedPlayer.getUUID());
    return OperationResult.SUCCESS;
  }

  /**
   * Retrieves a named global warp destination.
   *
   * @param player a player used to resolve the current server
   * @param name the warp name
   * @return the warp destination, or {@code null} when it does not exist
   */
  public PayTpData getWarp(ServerPlayer player, String name) {
    MinecraftServer server = player.level().getServer();
    ServerLevel overworld = server.overworld();
    PayTpWarpState state = getState(overworld);
    return state.canAccess(name, player.getUUID())
        ? state.getWarp(name)
        : null;
  }

  /**
   * Returns all global warp destinations.
   *
   * @param player a player used to resolve the current server
   * @return the stored warp map
   */
  public Map<String, PayTpData> getAllWarps(ServerPlayer player) {
    MinecraftServer server = player.level().getServer();
    ServerLevel overworld = server.overworld();
    PayTpWarpState state = getState(overworld);
    Map<String, PayTpData> accessibleWarps = new HashMap<>();
    state.getAllWarps().forEach((name, destination) -> {
      if (state.canAccess(name, player.getUUID())) {
        accessibleWarps.put(name, destination);
      }
    });
    return accessibleWarps;
  }

  public List<WarpView> getVisibleWarps(
      ServerPlayer player,
      AccessType filter
  ) {
    PayTpWarpState state = getState(player.level().getServer().overworld());
    UUID playerId = player.getUUID();
    List<WarpView> warps = new ArrayList<>();

    state.getAllWarps().forEach((name, destination) -> {
      boolean publicWarp = state.isPublic(name);
      boolean serverWarp = state.isServer(name);
      boolean owned = state.isOwner(name, playerId);
      boolean invited = state.isInvited(name, playerId);

      if (filter == AccessType.PUBLIC && !publicWarp) return;
      if (filter == AccessType.SERVER && !serverWarp) return;
      if (filter == AccessType.OWNED && !owned) return;
      if (filter == AccessType.INVITED && !invited) return;

      AccessType accessType;
      if (serverWarp) {
        accessType = AccessType.SERVER;
      } else if (publicWarp) {
        accessType = AccessType.PUBLIC;
      } else if (owned) {
        accessType = AccessType.OWNED;
      } else if (invited) {
        accessType = AccessType.INVITED;
      } else {
        accessType = AccessType.LOCKED;
      }
      UUID ownerId = state.getOwnerId(name);
      String ownerName = ownerId == null ? "" :
          player.level().getServer().services().nameToIdCache()
              .get(ownerId)
              .map(profile -> profile.name())
              .orElse(ownerId.toString());
      warps.add(new WarpView(name, destination, ownerName, accessType));
    });

    warps.sort(
        Comparator.comparing(WarpView::accessType)
            .thenComparing(WarpView::name, String.CASE_INSENSITIVE_ORDER)
    );
    return List.copyOf(warps);
  }

  public boolean isOwner(ServerPlayer player, String name) {
    PayTpWarpState state = getState(player.level().getServer().overworld());
    return state.isOwner(name, player.getUUID());
  }

  public boolean isPublic(ServerPlayer player, String name) {
    PayTpWarpState state = getState(player.level().getServer().overworld());
    return state.hasWarp(name) && state.isPublic(name);
  }

  public boolean isServer(ServerPlayer player, String name) {
    PayTpWarpState state = getState(player.level().getServer().overworld());
    return state.hasWarp(name) && state.isServer(name);
  }

  public List<String> getOwnedWarpNames(
      ServerPlayer player,
      boolean privateOnly
  ) {
    PayTpWarpState state = getState(player.level().getServer().overworld());
    return state.getAllWarps().keySet().stream()
        .filter(name -> state.isOwner(name, player.getUUID()))
        .filter(name -> !privateOnly || !state.isPublic(name))
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .toList();
  }

  public List<String> getAllWarpNames(ServerPlayer player) {
    PayTpWarpState state = getState(player.level().getServer().overworld());
    return state.getAllWarps().keySet().stream()
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .toList();
  }
}
