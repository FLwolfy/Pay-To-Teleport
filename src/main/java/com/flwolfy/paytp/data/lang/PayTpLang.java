package com.flwolfy.paytp.data.lang;

public enum PayTpLang {
  ENGLISH("en_us", "English"),
  SIMPLIFIED_CHINESE("zh_cn", "简体中文"),
  TRADITIONAL_CHINESE("zh_tw", "繁體中文");

  PayTpLang(String langKey, String langName) {
    key = langKey;
    name = langName;
  }

  private final String key;
  private final String name;

  public static PayTpLang fromKey(String key) {
    if (key == null) return ENGLISH;
    for (PayTpLang lang : values()) {
      if (lang.key.equalsIgnoreCase(key)) {
        return lang;
      }
    }
    return ENGLISH;
  }

  public String getLangKey() {
    return key;
  }

  @Override
  public String toString() {
    return name;
  }
}
