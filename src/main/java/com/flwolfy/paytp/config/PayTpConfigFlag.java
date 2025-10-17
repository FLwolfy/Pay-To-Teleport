package com.flwolfy.paytp.config;

public enum PayTpConfigFlag {
  ALLOW_ENDER_CHEST(1 << 0),
  PRIORITIZE_ENDER_CHEST(1 << 1),
  ALLOW_SHULKER_BOX(1 << 2),
  PRIORITIZE_SHULKER_BOX(1 << 3);

  private final int bit;

  PayTpConfigFlag(int bit) {
    this.bit = bit;
  }

  public int getBit() {
    return bit;
  }

  /**
   * Check whether the given flags contain flagsToCheck.
   */
  public static boolean check(int flags, PayTpConfigFlag... flagsToCheck) {
    for (PayTpConfigFlag flag : flagsToCheck) {
      if ((flags & flag.getBit()) == 0) return false;
    }
    return true;
  }

  /**
   * Check whether the given flags is completely equivalent to flagsToCheck.
   */
  public static boolean equivalent(int flags, PayTpConfigFlag... flagsToCheck) {
    int result = 0;
    for (PayTpConfigFlag flag : flagsToCheck) {
      result |= flag.getBit();
    }
    return flags == result;
  }
}
