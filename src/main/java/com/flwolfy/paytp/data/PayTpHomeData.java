package com.flwolfy.paytp.data;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public record PayTpHomeData(
    Vec3d pos,
    RegistryKey<World> dimension
) {
  /**
   * Another constructor support for serialization
   */
  public PayTpHomeData(String dimensionId, double x, double y, double z) {
    this(new Vec3d(x, y, z), RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(dimensionId)));
  }
}
