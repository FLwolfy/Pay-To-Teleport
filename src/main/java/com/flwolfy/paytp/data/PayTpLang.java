package com.flwolfy.paytp.data;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum PayTpLang {
  ENGLISH("en_us"),
  SIMPLIFIED_CHINESE("zh_cn"),
  TRADITIONAL_CHINESE("zh_tw");

  PayTpLang(String langKey) {
    key = langKey;
  }

  private final String key;
  public String getLangKey() { return key; }

  @Override
  public String toString() {
    return Arrays.stream(this.name().split("_"))
        .map(s -> s.substring(0,1).toUpperCase() + s.substring(1).toLowerCase())
        .collect(Collectors.joining(" "));
  }
}
