package com.flwolfy.paytp.config;

public record PayTpConfigData(
    String commandName,
    String currencyItem,
    int minPrice,
    int maxPrice,
    double baseRadius,
    double rate,
    boolean allowEnderChest,
    boolean prioritizeEnderChest
) {

  /**
   * Returns a default config instance with preset values.
   */
  public static final PayTpConfigData DEFAULT = new PayTpConfigData(
    "paytp",
    "minecraft:diamond",
    1,
    64,
    10.0,
    0.5,
    true,
    false
  );
}
