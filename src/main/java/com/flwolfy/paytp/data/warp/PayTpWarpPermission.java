package com.flwolfy.paytp.data.warp;

/**
 * Controls who may create server warps and force-delete warps.
 */
public enum PayTpWarpPermission {
  ALL(0),
  MODERATORS(1),
  GAMEMASTERS(2),
  ADMINS(3),
  OWNERS(4);

  private final int level;

  PayTpWarpPermission(int level) {
    this.level = level;
  }

  /**
   * Resolves a permission by its Minecraft command permission level.
   *
   * @param level the serialized permission level
   * @return the matching permission, or {@link #GAMEMASTERS} when unsupported
   */
  public static PayTpWarpPermission fromLevel(int level) {
    for (PayTpWarpPermission permission : values()) {
      if (permission.level == level) {
        return permission;
      }
    }
    return GAMEMASTERS;
  }

  /**
   * Returns the Minecraft command permission level.
   *
   * @return the serialized permission level from {@code 0} through {@code 4}
   */
  public int getLevel() {
    return level;
  }
}
