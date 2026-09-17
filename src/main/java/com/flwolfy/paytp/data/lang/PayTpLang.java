package com.flwolfy.paytp.data.lang;

import java.util.Set;

/**
 * Provides compatibility access to dynamically discovered PayTp languages.
 */
public final class PayTpLang {

  private PayTpLang() {}

  /**
   * Returns every language locale discovered from bundled JSON resources.
   *
   * @return immutable available locale keys
   */
  public static Set<String> availableLocales() {
    return PayTpLangManager.getInstance().availableLocales();
  }

  /**
   * Returns the display name declared by a language JSON file.
   *
   * @param locale the language locale
   * @return the discovered language name, or the normalized locale
   */
  public static String name(String locale) {
    return PayTpLangManager.getInstance().languageName(locale);
  }
}
