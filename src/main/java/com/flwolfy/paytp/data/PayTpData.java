package com.flwolfy.paytp.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.Locale;

/**
 * Represents a teleport point: (world, position).
 * Two PayTpData are equal if they point to the same world and have nearly identical coordinates.
 */
public record PayTpData(
    ResourceKey<Level> world,
    Vec3 pos
) {

  /**
   * Codec used by the persistent states; serializes as dimension id plus coordinates.
   */
  public static final Codec<PayTpData> CODEC = RecordCodecBuilder.create(instance ->
      instance.group(
          Codec.STRING.fieldOf("dimension").forGetter(pd -> pd.world().identifier().toString()),
          Codec.DOUBLE.fieldOf("x").forGetter(pd -> pd.pos().x),
          Codec.DOUBLE.fieldOf("y").forGetter(pd -> pd.pos().y),
          Codec.DOUBLE.fieldOf("z").forGetter(pd -> pd.pos().z)
      ).apply(instance, PayTpData::new)
  );

  /**
   * Another constructor support for serialization
   */
  public PayTpData(String dimensionId, double x, double y, double z) {
    this(ResourceKey.create(
        Registries.DIMENSION,
        Objects.requireNonNull(Identifier.tryParse(dimensionId))),
        new Vec3(x, y, z)
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof PayTpData(ResourceKey<Level> world1, Vec3 pos1)))
      return false;

    // Compare world registry keys instead of instance references
    if (!Objects.equals(world, world1)) return false;

    // Compare position with tolerance
    return Mth.equal(this.pos.x, pos1.x)
        && Mth.equal(this.pos.y, pos1.y)
        && Mth.equal(this.pos.z, pos1.z);
  }

  @Override
  public String toString() {
    return String.format(
        Locale.ROOT,
        "<%s, (%.2f, %.2f, %.2f)>",
        world.identifier(),
        pos.x, pos.y, pos.z
    );
  }
}

