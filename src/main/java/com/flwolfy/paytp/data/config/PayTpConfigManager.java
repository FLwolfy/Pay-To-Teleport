package com.flwolfy.paytp.data.config;

import com.flwolfy.paytp.PayTpMod;

import com.flwolfy.paytp.data.lang.PayTpLang;
import com.flwolfy.paytp.data.lang.PayTpLangAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;

/**
 * PayTpConfigManager handles loading and saving configuration for the Pay-to-Teleport mod.
 */
public class PayTpConfigManager {

  private static final Logger LOGGER = PayTpMod.LOGGER;
  private static final Path CONFIG_PATH = Path.of("config", "paytp.json");
  private static final Gson GSON;
  static {
    GsonBuilder gsonBuilder = new GsonBuilder();

    // ================================
    // Register customized adapter here
    // ================================
    gsonBuilder.registerTypeAdapter(PayTpLang.class, new PayTpLangAdapter());
    // ================================

    GSON = gsonBuilder.setPrettyPrinting().create();
  }

  private PayTpConfigManager(PayTpConfigData data) {
    this.data = data;
  }

  private static PayTpConfigManager instance;
  public static PayTpConfigManager getInstance() {
    if (instance == null) {
      instance = loadConfig();
    }
    return instance;
  }

  private PayTpConfigData data;
  public PayTpConfigData data() {
    return data;
  }

  // =================================
  // ====== Load & Save Config =======
  // =================================

  private static PayTpConfigManager loadConfig() {
    PayTpConfigData defaults = PayTpConfigData.DEFAULT;

    try {
      if (Files.notExists(CONFIG_PATH)) {
        saveStatic(defaults);
        return new PayTpConfigManager(defaults);
      }

      try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
        JsonElement element = GSON.fromJson(reader, JsonElement.class);
        JsonObject jsonObject = element != null && element.isJsonObject()
            ? element.getAsJsonObject()
            : new JsonObject();

        JsonObject defaultJson = GSON.toJsonTree(defaults).getAsJsonObject();
        mergeDefaults(jsonObject, defaultJson);

        PayTpConfigData data = GSON.fromJson(jsonObject, PayTpConfigData.class);
        saveStatic(data);

        return new PayTpConfigManager(data);
      }
    } catch (Exception e) {
      LOGGER.error("Failed to load PayTp config, using defaults", e);
      saveStatic(defaults);
      return new PayTpConfigManager(defaults);
    }
  }

  private static void mergeDefaults(JsonObject target, JsonObject defaults) {
    for (var entry : defaults.entrySet()) {
      String key = entry.getKey();
      JsonElement defaultValue = entry.getValue();

      if (!target.has(key) || target.get(key).isJsonNull()) {
        target.add(key, defaultValue);
      } else if (defaultValue.isJsonObject() && target.get(key).isJsonObject()) {
        mergeDefaults(target.getAsJsonObject(key), defaultValue.getAsJsonObject());
      }
    }
  }

  private static void saveStatic(PayTpConfigData data) {
    try {
      Files.createDirectories(CONFIG_PATH.getParent());
    } catch (IOException e) {
      LOGGER.error("Failed to create config directory", e);
    }

    try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
      GSON.toJson(data, writer);
      LOGGER.info("Saved PayTp config to {}", CONFIG_PATH);
    } catch (IOException e) {
      LOGGER.error("Failed to save PayTp config", e);
    }
  }

  // ============================
  // ====== Update Config =======
  // ============================

  public void update(PayTpConfigData newData) {
    this.data = newData;
    saveStatic(newData);
  }
}
