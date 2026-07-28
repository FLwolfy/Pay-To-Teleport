package com.flwolfy.paytp.data;

/**
 * Identifies the operation that initiated a priced teleport.
 *
 * <p>The serialized names returned by {@link #toString()} are exposed to JEXL as
 * {@code teleportType}.</p>
 */
public enum PayTpTeleportType {
  COORDINATE("coordinate"),
  REQUEST("request"),
  HOME("home"),
  BACK("back"),
  WARP("warp");

  private final String name;

  PayTpTeleportType(String name) {
    this.name = name;
  }

  /**
   * Returns the stable lowercase name exposed to scripts.
   *
   * @return the script-facing teleport type name
   */
  @Override
  public String toString() {
    return name;
  }
}
