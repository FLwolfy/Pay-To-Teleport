package com.flwolfy.paytp.command;

import com.flwolfy.paytp.data.PayTpData;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;

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

  public PayTpWarpState() {
    this.warpMap = new HashMap<>();
    this.beaconMap = new HashMap<>();
  }

  private PayTpWarpState(Map<String, PayTpData> warpMap, Map<String, PayTpData> beaconMap) {
    this.warpMap = new HashMap<>(warpMap);
    this.beaconMap = new HashMap<>(beaconMap);
  }

  // ====================================== //
  // ============= Warp API =============== //
  // ====================================== //

  public boolean setWarp(String name, PayTpData warpData, PayTpData beaconData) {
    if (warpMap.containsKey(name)) return false;
    for (PayTpData existingBeacon : beaconMap.values()) {
      if (existingBeacon.equals(beaconData)) return false;
    }
    warpMap.put(name, warpData);
    if (beaconData != null) beaconMap.put(name, beaconData);
    setDirty();
    return true;
  }

  public boolean removeWarp(String name) {
    if (!warpMap.containsKey(name)) return false;
    warpMap.remove(name);
    beaconMap.remove(name);
    setDirty();
    return true;
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

  public Map<String, PayTpData> getAllWarps() {
    return warpMap;
  }

  public Map<String, PayTpData> getAllBeacons() {
    return beaconMap;
  }
}
