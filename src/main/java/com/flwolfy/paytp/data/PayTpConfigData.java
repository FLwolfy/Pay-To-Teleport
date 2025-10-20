package com.flwolfy.paytp.data;

import com.flwolfy.paytp.flag.Flags;
import com.flwolfy.paytp.flag.PayTpMultiplierFlags;
import com.flwolfy.paytp.flag.PayTpSettingFlags;

public record PayTpConfigData(
    Commands commands,
    Attributes attributes,
    Prices prices,
    Multipliers multipliers,
    Settings settings
) {

  // =============================
  // ====== Internal record ======
  // =============================

  public record Commands(
      String commandName,
      String acceptName,
      String denyName,
      String cancelName,
      String homeName,
      String backName
  ) {}

  public record Attributes(
      String language,
      int expireTime,
      int maxBackStack,
      boolean particleEffect
  ) {}

  public record Prices(
      String currencyItem,
      int minPrice,
      int maxPrice,
      double baseRadius,
      double rate
  ) {}

  public record Multipliers(
      double crossDimMultiplier,
      double homeMultiplier,
      double backMultiplier
  ) {}

  public record Settings(
      boolean allowEnderChest,
      boolean prioritizeEnderChest,
      boolean allowShulkerBox,
      boolean prioritizeShulkerBox
  ) {}

  // ===============================
  // ====== Default Data ===========
  // ===============================

  public static final PayTpConfigData DEFAULT = new PayTpConfigData(
      new Commands(
          "ptp",
          "ptpaccept",
          "ptpdeny",
          "ptpcancel",
          "ptphome",
          "ptpback"
      ),
      new Attributes(
          PayTpLang.ENGLISH.getLangKey(),
          10,
          10,
          true
      ),
      new Prices(
          "minecraft:diamond",
          1,
          64,
          10.0,
          0.01
      ),
      new Multipliers(
          1.5,
          0.5,
          0.8
      ),
      new Settings(
          true,
          true,
          false,
          false
      )
  );

  // ===============================
  // ====== Tool Methods ===========
  // ===============================

  /**
   * Get the setting flags of the config data.
   */
  public int combineSettingFlags() {
    return Flags.combine(
        settings.allowEnderChest() ? PayTpSettingFlags.ALLOW_ENDER_CHEST : null,
        settings.prioritizeEnderChest() ? PayTpSettingFlags.PRIORITIZE_ENDER_CHEST : null,
        settings.allowShulkerBox() ? PayTpSettingFlags.ALLOW_SHULKER_BOX : null,
        settings.prioritizeShulkerBox() ? PayTpSettingFlags.PRIORITIZE_SHULKER_BOX : null
    );
  }

  /**
   * Get calculated multiplier with given multiplier flags.
   */
  public double calculateMultiplier(int multiplierFlags) {
    double multiplier = 1.0;
    if (Flags.check(multiplierFlags, PayTpMultiplierFlags.CROSS_DIMENSION))
      multiplier *= multipliers.crossDimMultiplier();
    if (Flags.check(multiplierFlags, PayTpMultiplierFlags.HOME))
      multiplier *= multipliers.homeMultiplier();
    if (Flags.check(multiplierFlags, PayTpMultiplierFlags.BACK))
      multiplier *= multipliers.backMultiplier();
    return multiplier;
  }
}
