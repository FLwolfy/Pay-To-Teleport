package com.flwolfy.paytp.util;

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

public class PayTpTextLoader {

  private static final Gson GSON = new Gson();
  private static final Logger LOGGER = PayTpMod.LOGGER;
  private static final Map<String, Map<String, String>> LANGUAGE_MAP = loadAllLanguages();

  private static PayTpTextLoader instance;
  private PayTpTextLoader() {}

  private String language;

  public static PayTpTextLoader getInstance(String lang) {
    if (instance == null) {
      instance = new PayTpTextLoader();
    }
    instance.language = lang;
    return instance;
  }

  public static Map<String, Map<String, String>> loadAllLanguages() {
    Map<String, Map<String, String>> langMap = new HashMap<>();
    String[] languages = {"en_us", "zh_cn", "zh_tw"};

    for (String lang : languages) {
      String path = "/assets/" + PayTpMod.MOD_ID + "/lang/" + lang + ".json";
      try (InputStreamReader reader = new InputStreamReader(
          Objects.requireNonNull(PayTpTextLoader.class.getResourceAsStream(path)))) {

        Type type = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> map = GSON.fromJson(reader, type);

        if (map != null) {
          langMap.put(lang, map);
        }

      } catch (Exception e) {
        LOGGER.error(e.getMessage(), e);
      }
    }

    return langMap;
  }

  /**
   * Get the localized Text based on the given key.
   */
  public MutableText getText(String key) {
    if (LANGUAGE_MAP.isEmpty()) {
      return Text.literal(key);
    }

    Map<String, String> map = LANGUAGE_MAP.get(this.language);
    if (map == null) {
      return Text.literal(key);
    }

    String value = map.getOrDefault(key, key);
    return Text.literal(value);
  }

}
