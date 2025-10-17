package com.flwolfy.paytp.config;

public record PayTpConfigData(
    // Attributes
    String commandName,
    String currencyItem,

    // Prices
    int minPrice,
    int maxPrice,
    double baseRadius,
    double rate,

    // Settings
    boolean allowEnderChest,
    boolean prioritizeEnderChest,
    boolean allowShulkerBox,
    boolean prioritizeShulkerBox
) {

  /**
   * Returns a default config instance with preset values.
   */
  public static final PayTpConfigData DEFAULT = new PayTpConfigData(
      "ptp",
      "minecraft:diamond",
      1,
      64,
      10.0,
      0.5,
      true,
      true,
      true,
      true
  );

  /**
   * Get the setting flags of the config data.
   */
  public int flags() {
    int f = 0;
    if (allowEnderChest)      f |= PayTpConfigFlag.ALLOW_ENDER_CHEST.getBit();
    if (prioritizeEnderChest) f |= PayTpConfigFlag.PRIORITIZE_ENDER_CHEST.getBit();
    if (allowShulkerBox)      f |= PayTpConfigFlag.ALLOW_SHULKER_BOX.getBit();
    if (prioritizeShulkerBox) f |= PayTpConfigFlag.PRIORITIZE_SHULKER_BOX.getBit();
    return f;
  }
}
