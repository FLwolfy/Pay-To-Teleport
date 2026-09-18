package com.flwolfy.paytp.data.lang;

import com.flwolfy.paytp.PayTpMod;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Loads bundled language files and creates server-side localized components.
 */
public final class PayTpLangManager {

  public static final String DEFAULT_LANGUAGE = PayTpLanguageLoader.DEFAULT_LOCALE;

  private final Map<String, PayTpLanguage> languages;
  private final Set<String> warnedMissingKeys = ConcurrentHashMap.newKeySet();
  private volatile String language = DEFAULT_LANGUAGE;

  private PayTpLangManager() {
    this(new PayTpLanguageLoader().load());
  }

  PayTpLangManager(Map<String, PayTpLanguage> languages) {
    this.languages = Collections.unmodifiableMap(new LinkedHashMap<>(languages));
  }

  public static PayTpLangManager getInstance() {
    return Holder.INSTANCE;
  }

  // =========================================== //
  // ============= Languages Methods =========== //
  // =========================================== //

  /**
   * Sets the language used when creating localized components.
   *
   * @param lang the requested language; unsupported values fall back to English
   */
  public void setLanguage(String lang) {
    String normalized = normalize(lang);
    language = languages.containsKey(normalized) ? normalized : DEFAULT_LANGUAGE;
  }

  public Component text(String key, Object... arguments) {
    String pattern = resolve(language, key);
    try {
      return Component.literal(pattern.formatted(arguments));
    } catch (RuntimeException ignored) {
      return Component.literal(pattern);
    }
  }

  public Component textFor(String locale, String key, Object... arguments) {
    String normalized = normalize(locale);
    String selected = languages.containsKey(normalized) ? normalized : language;
    String pattern = resolve(selected, key);
    try {
      return Component.literal(pattern.formatted(arguments));
    } catch (RuntimeException ignored) {
      return Component.literal(pattern);
    }
  }

  public Set<String> availableLocales() {
    return Collections.unmodifiableSet(new LinkedHashSet<>(new TreeSet<>(languages.keySet())));
  }

  public Set<String> coreLocales() {
    return languages.keySet();
  }

  public String languageName(String locale) {
    String normalized = normalize(locale);
    PayTpLanguage bundled = languages.get(normalized);
    return bundled == null ? normalized : bundled.name();
  }

  /**
   * Returns a localized {@link Component} for the given key.
   *
   * @param key the translation key
   * @return a mutable literal component containing the translated value, or the key when missing
   */
  public MutableComponent getText(String key) {
    return Component.literal(resolve(language, key));
  }

  private String resolve(String locale, String key) {
    String pattern = translation(locale, key);
    if (pattern != null) {
      return pattern;
    }
    warnMissingTranslation(locale, key);
    pattern = translation(DEFAULT_LANGUAGE, key);
    if (pattern != null) {
      return pattern;
    }
    if (warnedMissingKeys.add("all\0" + key)) {
      PayTpMod.LOGGER.warn("Missing language key: {}", key);
    }
    return key;
  }

  private String translation(String locale, String key) {
    PayTpLanguage bundled = languages.get(locale);
    return bundled == null ? null : bundled.translations().get(key);
  }

  private void warnMissingTranslation(String locale, String key) {
    if (!DEFAULT_LANGUAGE.equals(locale)
        && languages.containsKey(locale)
        && warnedMissingKeys.add("core\0" + locale + '\0' + key)) {
      PayTpMod.LOGGER.warn(
          "Missing {} PayTp language key {}; using en_us fallback",
          locale,
          key
      );
    }
  }

  private static String normalize(String locale) {
    return locale == null ? "" : locale.trim().toLowerCase(Locale.ROOT);
  }

  private static final class Holder {
    private static final PayTpLangManager INSTANCE = new PayTpLangManager();
  }
}
