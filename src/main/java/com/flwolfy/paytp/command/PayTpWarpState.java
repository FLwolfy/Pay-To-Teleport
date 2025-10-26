package com.flwolfy.paytp.command;

import com.flwolfy.paytp.data.PayTpData;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

/**
 * 世界级 Warp 存储状态（不与玩家绑定）
 */
public class PayTpWarpState extends PersistentState {

  public static final String STATE_ID = "paytp_warp_state";

  private final Map<String, PayTpData> warpMap = new HashMap<>();
  private final Map<String, PayTpData> beaconMap = new HashMap<>();

  public static final Type<PayTpWarpState> TYPE = new Type<>(
      PayTpWarpState::new,
      PayTpWarpState::fromNbt,
      null
  );

  public PayTpWarpState() {}

  public static PayTpWarpState fromNbt(NbtCompound nbt, WrapperLookup lookup) {
    PayTpWarpState state = new PayTpWarpState();
    NbtList list = nbt.getList("warps", NbtElement.COMPOUND_TYPE);
    for (int i = 0; i < list.size(); i++) {
      NbtCompound entry = list.getCompound(i);
      String name = entry.getString("name");

      // warp 坐标
      String dimStr = entry.getString("dimension");
      double x = entry.getDouble("x");
      double y = entry.getDouble("y");
      double z = entry.getDouble("z");
      RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(dimStr));
      PayTpData data = new PayTpData(dimKey, new Vec3d(x, y, z));

      // beacon 坐标
      if (entry.contains("beacon", NbtElement.COMPOUND_TYPE)) {
        NbtCompound beaconNbt = entry.getCompound("beacon");
        String bDimStr = beaconNbt.getString("dimension");
        double bx = beaconNbt.getDouble("x");
        double by = beaconNbt.getDouble("y");
        double bz = beaconNbt.getDouble("z");
        RegistryKey<World> bDimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(bDimStr));
        PayTpData beacon = new PayTpData(bDimKey, new Vec3d(bx, by, bz));
        state.beaconMap.put(name, beacon);
      }

      state.warpMap.put(name, data);
    }
    return state;
  }

  @Override
  public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup warpperLookup) {
    NbtList list = new NbtList();
    for (String name : warpMap.keySet()) {
      PayTpData data = warpMap.get(name);
      NbtCompound entry = new NbtCompound();
      entry.putString("name", name);
      entry.putString("dimension", data.world().getValue().toString());
      entry.putDouble("x", data.pos().x);
      entry.putDouble("y", data.pos().y);
      entry.putDouble("z", data.pos().z);

      PayTpData beacon = beaconMap.get(name);
      if (beacon != null) {
        NbtCompound beaconNbt = new NbtCompound();
        beaconNbt.putString("dimension", beacon.world().getValue().toString());
        beaconNbt.putDouble("x", beacon.pos().x);
        beaconNbt.putDouble("y", beacon.pos().y);
        beaconNbt.putDouble("z", beacon.pos().z);
        entry.put("beacon", beaconNbt);
      }

      list.add(entry);
    }
    nbt.put("warps", list);
    return nbt;
  }

  // -----------------------------
  // warp API
  // -----------------------------

  public boolean setWarp(String name, PayTpData warpData, PayTpData beaconData) {
    if (warpMap.containsKey(name)) {
      return false;
    }

    for (PayTpData existingBeacon : beaconMap.values()) {
      if (existingBeacon.equals(beaconData)) {
        return false;
      }
    }

    warpMap.put(name, warpData);
    beaconMap.put(name, beaconData);
    markDirty();
    return true;
  }

  public boolean removeWarp(String name) {
    if (warpMap.containsKey(name)) {
      warpMap.remove(name);
      beaconMap.remove(name);
      markDirty();
      return true;
    }

    return false;
  }

  public boolean hasWarp(String name) {
    return warpMap.containsKey(name);
  }

  public PayTpData getWarp(String name) {
    return warpMap.get(name);
  }

  public Map<String, PayTpData> getAllWarps() {
    return warpMap;
  }

  public PayTpData getBeacon(String name) {
    return beaconMap.get(name);
  }
}
