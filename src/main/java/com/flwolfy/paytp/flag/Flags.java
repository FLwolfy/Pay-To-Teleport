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
  /**
   * Represents an empty bit mask.
   */
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
   * Returns the bit assigned to an annotated enum constant.
   *
   * @param flag the flag constant
   * @param <T> the annotated enum type
   * @return the single-bit mask assigned from the constant's ordinal
   */
  public static <T> int bit(T flag) {
    return getBit(flag);
  }

  /**
   * Combines multiple flag constants into one bit mask.
   *
   * @param flags the constants to combine; null entries are ignored
   * @param <T> the annotated enum type
   * @return the combined mask, or {@link #NO_FLAG} for a null array
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
   * Checks whether a mask contains all specified flags.
   *
   * @param flags the mask to inspect
   * @param toCheck the required flag constants
   * @param <T> the annotated enum type
   * @return {@code true} when every requested flag is present
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
   * Checks whether a mask contains exactly the specified flags.
   *
   * @param flags the mask to compare
   * @param toCheck the expected flag constants
   * @param <T> the annotated enum type
   * @return {@code true} when the masks are equal
   */
  @SafeVarargs
  public static <T> boolean equivalent(int flags, T... toCheck) {
    int combined = combine(toCheck);
    return flags == combined;
  }

  /**
   * Describes a mask by joining the names of all present enum constants.
   *
   * @param flags the mask to describe
   * @param flagClass the annotated enum class that defines the bits
   * @return joined flag names, or {@code NONE} when no bits are present
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
