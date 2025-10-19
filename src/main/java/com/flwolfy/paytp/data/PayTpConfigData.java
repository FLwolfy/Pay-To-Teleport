package com.flwolfy.paytp.data;

import com.flwolfy.paytp.flag.Flags;
import com.flwolfy.paytp.flag.PayTpMultiplierFlags;
import com.flwolfy.paytp.flag.PayTpSettingFlags;

public record PayTpConfigData(
    // Commands
    String commandName,
    String acceptName,
    String denyName,
    String cancelName,
    String homeName,
    String backName,

    // Items
    String currencyItem,

    // Attributes
    String language,
    int expireTime,
    int maxBackStack,
    boolean particleEffect,

    // Prices
    int minPrice,
    int maxPrice,
    double baseRadius,
    double rate,

    // Multipliers
    double crossDimMultiplier,
    double homeMultiplier,
    double backMultiplier,

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
      "ptpaccept",
      "ptpdeny",
      "ptpcancel",
      "ptphome",
      "ptpback",

      "minecraft:diamond",

      "en_us",
      10,
      10,
      true,

      1,
      64,
      10.0,
      0.01,
      1.5,
      0.5,
      0.8,

      true,
      true,
      false,
      false
  );

  /**
   * Get the setting flags of the config data.
   */
  public int combineSettingFlags() {
    return Flags.combine(
        allowEnderChest      ? PayTpSettingFlags.ALLOW_ENDER_CHEST      : null,
        prioritizeEnderChest ? PayTpSettingFlags.PRIORITIZE_ENDER_CHEST : null,
        allowShulkerBox      ? PayTpSettingFlags.ALLOW_SHULKER_BOX      : null,
        prioritizeShulkerBox ? PayTpSettingFlags.PRIORITIZE_SHULKER_BOX : null
    );
  }

  /**
   * Get calculated multiplier with given multiplier flags.
   */
  public double calculateMultiplier(int multiplierFlags) {
    double multiplier = 1.0;
    if (Flags.check(multiplierFlags, PayTpMultiplierFlags.CROSS_DIMENSION)) multiplier *= crossDimMultiplier;
    if (Flags.check(multiplierFlags, PayTpMultiplierFlags.HOME)) multiplier *= homeMultiplier;
    if (Flags.check(multiplierFlags, PayTpMultiplierFlags.BACK)) multiplier *= backMultiplier;
    return multiplier;
  }
}
