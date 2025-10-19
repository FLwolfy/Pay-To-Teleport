package com.flwolfy.paytp.flag;

/**
 * Base interface for bitwise flag enums.
 *
 * Each enum implementing this should provide its bit mask value via {@link #bit()}.
 */
public interface Flags {

  int NO_FLAG = 0;

  /**
   * @return bit value of this flag (e.g., 1 << n)
   */
  default int bit() {
    return FlagDistributor.getBit(this);
  }

  // ==============================
  // == Default Utility Methods ==
  // ==============================

  /**
   * Combine multiple flags into one int bitmask.
   */
  @SafeVarargs
  static <T extends Flags> int combine(T... flags) {
    int result = 0;
    for (T flag : flags) {
      if (flag != null) result |= flag.bit();
    }
    return result;
  }

  /**
   * Check if given bitmask contains *all* the specified flags.
   */
  @SafeVarargs
  static <T extends Flags> boolean check(int flags, T... toCheck) {
    for (T flag : toCheck) {
      if ((flags & flag.bit()) == 0)
        return false;
    }
    return true;
  }

  /**
   * Check if bitmask is exactly equivalent to given flags.
   */
  @SafeVarargs
  static <T extends Flags> boolean equivalent(int flags, T... toCheck) {
    int combined = combine(toCheck);
    return flags == combined;
  }
}
