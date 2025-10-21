package com.flwolfy.paytp.flag;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static utility class for working with bitwise flag enums.
 * <p>
 * Works with any enum annotated with {@link AutoBitFlags}.
 */
public final class Flags {

  private static final Map<Object, Integer> BIT_CACHE = new ConcurrentHashMap<>();
  public static final int NO_FLAG = 0;

  private Flags() {}

  private static synchronized int getBit(Object flag) {
    if (flag == null)
      throw new IllegalArgumentException("Flag cannot be null.");

    Integer cached = BIT_CACHE.get(flag);
    if (cached != null)
      return cached;

    Class<?> clazz = flag.getClass();
    if (clazz.isEnum() && clazz.isAnnotationPresent(AutoBitFlags.class)) {
      Enum<?> e = (Enum<?>) flag;
      int bit = 1 << e.ordinal();
      BIT_CACHE.put(flag, bit);
      return bit;
    }

    throw new IllegalStateException("Unsupported flag type: " + clazz + " (must be an enum annotated with @AutoBitFlags)");
  }

  // -------------------------------------------------
  // Public APIs
  // -------------------------------------------------

  /**
   * Get the bit mask value of a specific flag.
   */
  public static <T> int bit(T flag) {
    return getBit(flag);
  }

  /**
   * Combine multiple flags into one int bitmask.
   */
  @SafeVarargs
  public static <T> int combine(T... flags) {
    int result = 0;
    if (flags == null) return 0;
    for (T flag : flags) {
      if (flag == null) continue;
      result |= getBit(flag);
    }
    return result;
  }

  /**
   * Check if given bitmask contains *all* the specified flags.
   */
  @SafeVarargs
  public static <T> boolean check(int flags, T... toCheck) {
    if (toCheck == null) return false;
    for (T flag : toCheck) {
      if ((flags & getBit(flag)) == 0)
        return false;
    }
    return true;
  }

  /**
   * Check if bitmask is exactly equivalent to given flags.
   */
  @SafeVarargs
  public static <T> boolean equivalent(int flags, T... toCheck) {
    int combined = combine(toCheck);
    return flags == combined;
  }

  /**
   * Describe a bitmask by joining all flag names (e.g. "HOME | BACK").
   */
  public static String describe(int flags, Class<? extends Enum<?>> flagClass) {
    if (!flagClass.isAnnotationPresent(AutoBitFlags.class))
      throw new IllegalArgumentException(flagClass + " is not annotated with @AutoBitFlags.");

    StringBuilder sb = new StringBuilder();
    for (Enum<?> constant : flagClass.getEnumConstants()) {
      int bit = getBit(constant);
      if ((flags & bit) != 0) {
        if (!sb.isEmpty()) sb.append(" | ");
        sb.append(constant.name());
      }
    }
    return !sb.isEmpty() ? sb.toString() : "NONE";
  }
}
