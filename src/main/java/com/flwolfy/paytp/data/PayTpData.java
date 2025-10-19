package com.flwolfy.paytp.data;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.Objects;

/**
 * Represents a teleport point: (world, position).
 * Two PayTpData are equal if they point to the same world and have nearly identical coordinates.
 */
public record PayTpData(
    ServerWorld world,
    Vec3d pos
) {
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof PayTpData(ServerWorld world1, Vec3d pos1)))
      return false;

    // Compare world registry keys instead of instance references
    RegistryKey<World> thisKey = this.world.getRegistryKey();
    RegistryKey<World> otherKey = world1.getRegistryKey();
    if (!Objects.equals(thisKey, otherKey)) return false;

    // Compare position with tolerance
    return MathHelper.approximatelyEquals(this.pos.x, pos1.x)
        && MathHelper.approximatelyEquals(this.pos.y, pos1.y)
        && MathHelper.approximatelyEquals(this.pos.z, pos1.z);
  }
}

