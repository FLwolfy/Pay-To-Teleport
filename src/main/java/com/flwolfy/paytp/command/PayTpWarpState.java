package com.flwolfy.paytp.command;

import com.flwolfy.paytp.data.PayTpData;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.HashMap;
import java.util.Map;

public class PayTpWarpState extends PersistentState {

  private static final String PERSISTENT_STATE_ID = "paytp_warp_state";

  public static final Codec<PayTpData> WARP_CODEC = RecordCodecBuilder.create(instance ->
      instance.group(
          Codec.STRING.fieldOf("dimension").forGetter(pd -> pd.world().getValue().toString()),
          Codec.DOUBLE.fieldOf("x").forGetter(pd -> pd.pos().x),
          Codec.DOUBLE.fieldOf("y").forGetter(pd -> pd.pos().y),
          Codec.DOUBLE.fieldOf("z").forGetter(pd -> pd.pos().z)
      ).apply(instance, PayTpData::new)
  );

  public static final Codec<PayTpWarpState> CODEC = RecordCodecBuilder.create(instance ->
      instance.group(
          Codec.unboundedMap(
              Codec.STRING,
              WARP_CODEC
          ).fieldOf("warps").forGetter(state ->
              state.warpMap
          ),
          Codec.unboundedMap(
              Codec.STRING,
              WARP_CODEC
          ).fieldOf("beacons").forGetter(state ->
              state.beaconMap
          )
      ).apply(instance, PayTpWarpState::new)
  );

  public static final PersistentStateType<PayTpWarpState> TYPE =
      new PersistentStateType<>(PERSISTENT_STATE_ID, PayTpWarpState::new, CODEC, null);

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
    markDirty();
    return true;
  }

  public boolean removeWarp(String name) {
    if (!warpMap.containsKey(name)) return false;
    warpMap.remove(name);
    beaconMap.remove(name);
    markDirty();
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
