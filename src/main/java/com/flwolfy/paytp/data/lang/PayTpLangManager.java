package com.flwolfy.paytp.data.lang;

import com.flwolfy.paytp.PayTpMod;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;

public class PayTpLangManager {

  public static final String DEFAULT_LANGUAGE = PayTpLang.ENGLISH.getLangKey();

  private static final Logger LOGGER = PayTpMod.LOGGER;
  private static final Gson GSON = new Gson();

  private static PayTpLangManager instance;
  private PayTpLangManager() {}

  private Map<String, Map<String, String>> languageMap = new HashMap<>();
  private String language;

  /**
   * Singleton method to get the loader instance.
   */
  public static PayTpLangManager getInstance() {
    if (instance == null) {
      instance = new PayTpLangManager();
      instance.languageMap = loadAllLanguages();
      instance.language = DEFAULT_LANGUAGE;
    }
    return instance;
  }

  // =========================================== //
  // ============= Languages Methods =========== //
  // =========================================== //

  private static Map<String, Map<String, String>> loadAllLanguages() {
    Map<String, Map<String, String>> langMap = new HashMap<>();

    for (PayTpLang langEnum : PayTpLang.values()) {
      String path = "/assets/" + PayTpMod.MOD_ID + "/lang/" + langEnum.getLangKey() + ".json";
      try (InputStreamReader reader = new InputStreamReader(
          Objects.requireNonNull(PayTpLangManager.class.getResourceAsStream(path)))) {

        Type type = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> map = GSON.fromJson(reader, type);

        if (map != null) {
          langMap.put(langEnum.getLangKey(), map);
        }

      } catch (Exception e) {
        LOGGER.error("Could not load language {}", langEnum.getLangKey(), e);
      }
    }

    return langMap;
  }

  /**
   * Set the language for the loader to output text.
   */
  public void setLanguage(PayTpLang lang) {
    if (languageMap.containsKey(lang.getLangKey())) {
      language = lang.getLangKey();
    } else {
      language = DEFAULT_LANGUAGE;
      LOGGER.warn("Language {} is not supported, set to default language {}.", lang.getLangKey(), DEFAULT_LANGUAGE);
    }
  }

  /**
   * Get the localized Text based on the given key.
   */
  public MutableText getText(String key) {
    if (languageMap.isEmpty()) {
      return Text.literal(key);
    }

    Map<String, String> map = languageMap.get(language);
    if (map == null) {
      return Text.literal(key);
    }

    String value = map.getOrDefault(key, key);
    return Text.literal(value);
  }

}
