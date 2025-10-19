package com.flwolfy.paytp.flag;

public enum PayTpSettingFlag {
  ALLOW_ENDER_CHEST(1),
  PRIORITIZE_ENDER_CHEST(1 << 1),
  ALLOW_SHULKER_BOX(1 << 2),
  PRIORITIZE_SHULKER_BOX(1 << 3);

  private final int bit;

  PayTpSettingFlag(int bit) {
    this.bit = bit;
  }

  public int getBit() {
    return bit;
  }

  /**
   * Check whether the given flags contain flagsToCheck.
   */
  public static boolean check(int flags, PayTpSettingFlag... flagsToCheck) {
    for (PayTpSettingFlag flag : flagsToCheck) {
      if ((flags & flag.getBit()) == 0) return false;
    }
    return true;
  }

  /**
   * Check whether the given flags is completely equivalent to flagsToCheck.
   */
  public static boolean equivalent(int flags, PayTpSettingFlag... flagsToCheck) {
    int result = 0;
    for (PayTpSettingFlag flag : flagsToCheck) {
      result |= flag.getBit();
    }
    return flags == result;
  }
}
