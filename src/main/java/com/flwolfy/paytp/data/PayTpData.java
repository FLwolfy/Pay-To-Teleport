package com.flwolfy.paytp.data;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.Objects;
import java.util.Locale;

/**
 * Represents a teleport point: (world, position).
 * Two PayTpData are equal if they point to the same world and have nearly identical coordinates.
 */
public record PayTpData(
    RegistryKey<World> world,
    Vec3d pos
) {
  /**
   * Another constructor support for serialization
   */
  public PayTpData(String dimensionId, double x, double y, double z) {
    this(RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(dimensionId)), new Vec3d(x, y, z));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof PayTpData(RegistryKey<World> world1, Vec3d pos1)))
      return false;

    // Compare world registry keys instead of instance references
    if (!Objects.equals(world, world1)) return false;

    // Compare position with tolerance
    return MathHelper.approximatelyEquals(this.pos.x, pos1.x)
        && MathHelper.approximatelyEquals(this.pos.y, pos1.y)
        && MathHelper.approximatelyEquals(this.pos.z, pos1.z);
  }

  @Override
  public String toString() {
    return String.format(
        Locale.ROOT,
        "<%s, (%.2f, %.2f, %.2f)>",
        world.getValue(),
        pos.x, pos.y, pos.z
    );
  }
}

