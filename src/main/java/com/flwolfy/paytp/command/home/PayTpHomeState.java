package com.flwolfy.paytp.command.home;

import com.flwolfy.paytp.data.PayTpData;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PayTpHomeState extends SavedData {

  private static final String PERSISTENT_STATE_ID = "paytp_home_state";
  private static final Codec<PayTpHomeState> HOME_CODEC = RecordCodecBuilder.create(instance ->
      instance.group(
          Codec.unboundedMap(
              Codec.STRING,   // UUID as string
              PayTpData.CODEC
          ).fieldOf("homes").forGetter(state ->
              state.homeMap.entrySet().stream()
                  .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue))
          )
      ).apply(instance, PayTpHomeState::new)
  );

  public static final SavedDataType<PayTpHomeState> TYPE = new SavedDataType<>(
      Identifier.withDefaultNamespace(PERSISTENT_STATE_ID),
      PayTpHomeState::new,
      HOME_CODEC,
      DataFixTypes.PLAYER
  );

  private final Map<UUID, PayTpData> homeMap;

  public PayTpHomeState() {
    this.homeMap = new HashMap<>();
  }

  private PayTpHomeState(Map<String, PayTpData> map) {
    this.homeMap = new HashMap<>();
    map.forEach((k, v) -> this.homeMap.put(UUID.fromString(k), v));
  }

  // ====================================== //
  // ============= Home Setting =========== //
  // ====================================== //
  public void setHome(UUID player, Vec3 pos, ResourceKey<Level> dimension) {
    homeMap.put(player, new PayTpData(dimension, pos));
    setDirty();
  }

  public PayTpData getHome(UUID player) {
    return homeMap.get(player);
  }

  public boolean hasHome(UUID player) {
    return homeMap.containsKey(player);
  }
}
