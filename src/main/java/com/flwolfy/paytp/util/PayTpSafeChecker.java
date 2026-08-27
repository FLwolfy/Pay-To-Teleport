package com.flwolfy.paytp.util;

import com.flwolfy.paytp.data.PayTpData;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Resolves a requested teleport destination to a nearby safe player position.
 */
public final class PayTpSafeChecker {

  private static final double POSITION_EPSILON = 1.0E-5;

  private PayTpSafeChecker() {}

  /**
   * Returns the requested destination when it is already safe, otherwise the nearest safe
   * destination within the search radius.
   *
   * @param player the player that will be teleported
   * @param world the destination world
   * @param destination the requested destination
   * @param range maximum horizontal and vertical search distance
   * @return a safe destination, or {@code null} when none is available nearby
   */
  public static PayTpData findSafeDestination(
      ServerPlayer player,
      ServerLevel world,
      PayTpData destination,
      int range
  ) {
    Vec3 requested = destination.pos();
    BlockPos origin = BlockPos.containing(requested);
    if (isRequestedPositionSafe(player, world, origin, requested)) {
      return destination;
    }

    Vec3 nearest = null;
    double nearestDistance = Double.POSITIVE_INFINITY;
    for (int y = -range; y <= range; y++) {
      for (int x = -range; x <= range; x++) {
        for (int z = -range; z <= range; z++) {
          Vec3 candidate = DismountHelper.findSafeDismountLocation(
              EntityTypes.PLAYER,
              world,
              origin.offset(x, y, z),
              true
          );
          if (candidate == null) continue;

          double distance = candidate.distanceToSqr(requested);
          if (distance < nearestDistance) {
            nearest = candidate;
            nearestDistance = distance;
          }
        }
      }
    }

    return nearest == null
        ? null
        : new PayTpData(destination.world(), nearest);
  }

  private static boolean isRequestedPositionSafe(
      ServerPlayer player,
      ServerLevel world,
      BlockPos blockPos,
      Vec3 requested
  ) {
    Vec3 safeBlockPosition = DismountHelper.findSafeDismountLocation(
        EntityTypes.PLAYER,
        world,
        blockPos,
        true
    );
    if (safeBlockPosition == null
        || Math.abs(safeBlockPosition.y - requested.y) > POSITION_EPSILON) {
      return false;
    }

    AABB bounds = player.getDimensions(Pose.STANDING).makeBoundingBox(requested);
    return world.noBlockCollision(player, bounds)
        && world.getWorldBorder().isWithinBounds(bounds);
  }
}
