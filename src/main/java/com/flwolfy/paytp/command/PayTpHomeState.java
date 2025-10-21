package com.flwolfy.paytp.command;

import com.flwolfy.paytp.data.PayTpData;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PayTpHomeState extends PersistentState {

  private static final String PERSISTENT_STATE_ID = "paytp_home_state";

  public static final Codec<PayTpData> HOME_CODEC = RecordCodecBuilder.create(instance ->
      instance.group(
          Codec.STRING.fieldOf("dimension").forGetter(pd -> pd.world().getValue().toString()),
          Codec.DOUBLE.fieldOf("x").forGetter(pd -> pd.pos().x),
          Codec.DOUBLE.fieldOf("y").forGetter(pd -> pd.pos().y),
          Codec.DOUBLE.fieldOf("z").forGetter(pd -> pd.pos().z)
      ).apply(instance, PayTpData::new)
  );

  public static final Codec<PayTpHomeState> CODEC = RecordCodecBuilder.create(instance ->
      instance.group(
          Codec.unboundedMap(
              Codec.STRING,   // UUID as string
              HOME_CODEC
          ).fieldOf("homes").forGetter(state ->
              state.homeMap.entrySet().stream()
                  .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue))
          )
      ).apply(instance, PayTpHomeState::new)
  );

  public static final PersistentStateType<PayTpHomeState> TYPE =
      new PersistentStateType<>(PERSISTENT_STATE_ID, PayTpHomeState::new, CODEC, null);


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
  public void setHome(UUID player, Vec3d pos, RegistryKey<World> dimension) {
    homeMap.put(player, new PayTpData(dimension, pos));
    markDirty();
  }

  public PayTpData getHome(UUID player) {
    return homeMap.get(player);
  }

  public boolean hasHome(UUID player) {
    return homeMap.containsKey(player);
  }
}
