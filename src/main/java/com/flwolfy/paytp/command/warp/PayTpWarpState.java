package com.flwolfy.paytp.command.warp;

import com.flwolfy.paytp.data.PayTpData;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PayTpWarpState extends SavedData {

  private static final String PERSISTENT_STATE_ID = "paytp_warp_state";
  private static final Codec<PayTpWarpState> WARP_CODEC = RecordCodecBuilder.create(instance ->
      instance.group(
          Codec.unboundedMap(
              Codec.STRING,
              PayTpData.CODEC
          ).fieldOf("warps").forGetter(state ->
              state.warpMap
          ),
          Codec.unboundedMap(
              Codec.STRING,
              PayTpData.CODEC
          ).fieldOf("beacons").forGetter(state ->
              state.beaconMap
          ),
          Codec.unboundedMap(
              Codec.STRING,
              Codec.STRING
          ).optionalFieldOf("owners", Map.of()).forGetter(state ->
              state.ownerMap
          ),
          Codec.unboundedMap(
              Codec.STRING,
              Codec.BOOL
          ).optionalFieldOf("visibility", Map.of()).forGetter(state ->
              state.publicMap
          ),
          Codec.unboundedMap(
              Codec.STRING,
              Codec.STRING.listOf()
          ).optionalFieldOf("invites", Map.of()).forGetter(state ->
              state.invitedMap
          ),
          Codec.unboundedMap(
              Codec.STRING,
              Codec.BOOL
          ).optionalFieldOf("server", Map.of()).forGetter(state ->
              state.serverMap
          )
      ).apply(instance, PayTpWarpState::new)
  );

  public static final SavedDataType<PayTpWarpState> TYPE = new SavedDataType<>(
      Identifier.withDefaultNamespace(PERSISTENT_STATE_ID),
      PayTpWarpState::new,
      WARP_CODEC,
      DataFixTypes.LEVEL
  );

  private final Map<String, PayTpData> warpMap;
  private final Map<String, PayTpData> beaconMap;
  private final Map<String, String> ownerMap;
  private final Map<String, Boolean> publicMap;
  private final Map<String, List<String>> invitedMap;
  private final Map<String, Boolean> serverMap;

  public PayTpWarpState() {
    this.warpMap = new HashMap<>();
    this.beaconMap = new HashMap<>();
    this.ownerMap = new HashMap<>();
    this.publicMap = new HashMap<>();
    this.invitedMap = new HashMap<>();
    this.serverMap = new HashMap<>();
  }

  private PayTpWarpState(
      Map<String, PayTpData> warpMap,
      Map<String, PayTpData> beaconMap,
      Map<String, String> ownerMap,
      Map<String, Boolean> publicMap,
      Map<String, List<String>> invitedMap,
      Map<String, Boolean> serverMap
  ) {
    this.warpMap = new HashMap<>(warpMap);
    this.beaconMap = new HashMap<>(beaconMap);
    this.ownerMap = new HashMap<>(ownerMap);
    this.publicMap = new HashMap<>(publicMap);
    this.invitedMap = new HashMap<>();
    invitedMap.forEach((name, invited) ->
        this.invitedMap.put(name, List.copyOf(invited))
    );
    this.serverMap = new HashMap<>(serverMap);
  }

  // ====================================== //
  // ============= Warp API =============== //
  // ====================================== //

  public boolean setWarp(
      String name,
      PayTpData warpData,
      PayTpData beaconData,
      UUID owner,
      boolean publicWarp
  ) {
    if (warpMap.containsKey(name)) return false;
    for (PayTpData existingBeacon : beaconMap.values()) {
      if (existingBeacon.equals(beaconData)) return false;
    }
    warpMap.put(name, warpData);
    if (beaconData != null) beaconMap.put(name, beaconData);
    ownerMap.put(name, owner.toString());
    publicMap.put(name, publicWarp);
    invitedMap.put(name, List.of());
    setDirty();
    return true;
  }

  public boolean setServerWarp(String name, PayTpData warpData) {
    if (warpMap.containsKey(name)) return false;
    warpMap.put(name, warpData);
    serverMap.put(name, true);
    setDirty();
    return true;
  }

  public boolean removeWarp(String name) {
    if (!warpMap.containsKey(name)) return false;
    warpMap.remove(name);
    beaconMap.remove(name);
    ownerMap.remove(name);
    publicMap.remove(name);
    invitedMap.remove(name);
    serverMap.remove(name);
    setDirty();
    return true;
  }

  public boolean renameWarp(String name, String newName) {
    if (!warpMap.containsKey(name) || warpMap.containsKey(newName)) return false;
    move(warpMap, name, newName);
    move(beaconMap, name, newName);
    move(ownerMap, name, newName);
    move(publicMap, name, newName);
    move(invitedMap, name, newName);
    move(serverMap, name, newName);
    setDirty();
    return true;
  }

  private static <T> void move(Map<String, T> map, String name, String newName) {
    T value = map.remove(name);
    if (value != null) map.put(newName, value);
  }

  public boolean hasWarp(String name) {
    return warpMap.containsKey(name);
  }

  public PayTpData getWarp(String name) {
    return warpMap.get(name);
  }

  public PayTpData getBeacon(String name) {
    return beaconMap.get(name);
  }

  public boolean hasBeacon(PayTpData beaconData) {
    return beaconMap.containsValue(beaconData);
  }

  public boolean isOwner(String name, UUID playerId) {
    return playerId.toString().equals(ownerMap.get(name));
  }

  public UUID getOwnerId(String name) {
    String ownerId = ownerMap.get(name);
    if (ownerId == null) return null;
    try {
      return UUID.fromString(ownerId);
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  public boolean isPublic(String name) {
    return !isServer(name) && publicMap.getOrDefault(name, true);
  }

  public boolean isServer(String name) {
    return serverMap.getOrDefault(name, false);
  }

  public boolean isInvited(String name, UUID playerId) {
    return invitedMap.getOrDefault(name, List.of())
        .contains(playerId.toString());
  }

  public boolean canAccess(String name, UUID playerId) {
    return isServer(name)
        || isPublic(name)
        || isOwner(name, playerId)
        || isInvited(name, playerId);
  }

  public boolean invite(String name, UUID playerId) {
    if (isInvited(name, playerId)) return false;
    List<String> invited = new java.util.ArrayList<>(
        invitedMap.getOrDefault(name, List.of())
    );
    invited.add(playerId.toString());
    invitedMap.put(name, List.copyOf(invited));
    setDirty();
    return true;
  }

  public boolean exclude(String name, UUID playerId) {
    if (!isInvited(name, playerId)) return false;
    List<String> invited = new java.util.ArrayList<>(
        invitedMap.getOrDefault(name, List.of())
    );
    invited.remove(playerId.toString());
    invitedMap.put(name, List.copyOf(invited));
    setDirty();
    return true;
  }

  public Map<String, PayTpData> getAllWarps() {
    return warpMap;
  }

  public Map<String, PayTpData> getAllBeacons() {
    return beaconMap;
  }
}
