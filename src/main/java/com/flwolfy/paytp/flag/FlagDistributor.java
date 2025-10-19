package com.flwolfy.paytp.flag;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlagDistributor {

  private static final Map<Flags, Integer> BIT_CACHE = new ConcurrentHashMap<>();

  public static synchronized int getBit(Flags flag) {
    Integer cached = BIT_CACHE.get(flag);
    if (cached != null) return cached;

    Class<?> clazz = flag.getClass();
    if (!clazz.isAnnotationPresent(AutoBitFlags.class))
      throw new IllegalStateException("Enum not annotated with @AutoBitFlags: " + clazz);

    Object[] constants = clazz.getEnumConstants();
    if (constants == null || constants.length == 0)
      throw new IllegalStateException("Enum constants not loaded yet: " + clazz);

    for (int i = 0; i < constants.length; i++) {
      BIT_CACHE.put((Flags) constants[i], 1 << i);
    }

    return BIT_CACHE.get(flag);
  }

}
