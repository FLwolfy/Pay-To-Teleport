package com.flwolfy.paytp.command;

import com.flwolfy.paytp.data.PayTpData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

public class PayTpHomeState extends PersistentState {
  private final Map<UUID, PayTpData> homeMap = new HashMap<>();
  public static final Type<PayTpHomeState> TYPE = new Type<>(
      PayTpHomeState::new,
      PayTpHomeState::fromNbt,
      null
  );

  public PayTpHomeState() {}

  public static PayTpHomeState fromNbt(NbtCompound nbt, WrapperLookup lookup) {
    PayTpHomeState state = new PayTpHomeState();
    NbtList list = nbt.getList("homes", NbtElement.COMPOUND_TYPE);
    for (int i = 0; i < list.size(); i++) {
      NbtCompound entry = list.getCompound(i);
      UUID uuid = entry.getUuid("uuid");
      String dimStr = entry.getString("dimension");
      double x = entry.getDouble("x");
      double y = entry.getDouble("y");
      double z = entry.getDouble("z");

      Vec3d pos = new Vec3d(x, y, z);
      Identifier dimId = Identifier.tryParse(dimStr);
      RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, dimId);

      state.homeMap.put(uuid, new PayTpData(dimKey, pos));
    }
    return state;
  }

  @Override
  public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup wrapperLookup) {
    NbtList list = new NbtList();
    for (Map.Entry<UUID, PayTpData> e : homeMap.entrySet()) {
      NbtCompound compound = new NbtCompound();
      compound.putUuid("uuid", e.getKey());
      compound.putString("dimension", e.getValue().world().getValue().toString());
      compound.putDouble("x", e.getValue().pos().x);
      compound.putDouble("y", e.getValue().pos().y);
      compound.putDouble("z", e.getValue().pos().z);
      list.add(compound);
    }
    nbt.put("homes", list);
    return nbt;
  }

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
