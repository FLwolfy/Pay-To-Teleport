package com.flwolfy.paytp.data.config;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.lang.PayTpLang;
import com.flwolfy.paytp.data.lang.PayTpLangAdapter;
import com.flwolfy.paytp.data.script.PayTpScript;
import com.flwolfy.paytp.data.script.PayTpScriptAdapter;
import com.flwolfy.paytp.data.warp.PayTpWarpPermission;
import com.flwolfy.paytp.data.warp.PayTpWarpPermissionAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;

/**
 * Loads, validates, normalizes, updates, and persists the PayTp configuration.
 *
 * <p>Reads and writes are guarded by a shared lock. Missing fields are recursively populated from
 * {@link PayTpConfigData#DEFAULT}, and invalid files are replaced with the default configuration.</p>
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
    gsonBuilder.registerTypeAdapter(PayTpScript.class, new PayTpScriptAdapter());
    gsonBuilder.registerTypeAdapter(
        PayTpWarpPermission.class,
        new PayTpWarpPermissionAdapter()
    );
    // ================================

    GSON = gsonBuilder.setPrettyPrinting().create();
  }

  private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private static PayTpConfigManager instance;
  private volatile PayTpConfigData data;

  private PayTpConfigManager(PayTpConfigData data) {
    this.data = data;
  }

  public static PayTpConfigManager getInstance() {
    if (instance == null) {
      instance = new PayTpConfigManager(loadData());
    }
    return instance;
  }

  /**
   * Returns the current immutable configuration snapshot under the read lock.
   *
   * @return the active configuration
   */
  public PayTpConfigData data() {
    lock.readLock().lock();
    try {
      return data;
    } finally {
      lock.readLock().unlock();
    }
  }

  // =================================
  // ====== Load & Save Config =======
  // =================================

  private static PayTpConfigData loadData() {
    PayTpConfigData defaults = PayTpConfigData.DEFAULT;

    if (Files.notExists(CONFIG_PATH)) {
      LOGGER.info("Config file not found, creating default config at {}", CONFIG_PATH);
      saveStatic(defaults);
      return defaults;
    }

    try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
      JsonElement element = GSON.fromJson(reader, JsonElement.class);
      JsonObject jsonObject = element != null && element.isJsonObject()
          ? element.getAsJsonObject()
          : new JsonObject();

      if (jsonObject.entrySet().isEmpty()) {
        LOGGER.warn("Config file is empty, filling with defaults");
        saveStatic(defaults);
        return defaults;
      }

      JsonObject defaultJson = GSON.toJsonTree(defaults).getAsJsonObject();
      boolean hasMissing = mergeDefaults(jsonObject, defaultJson);

      PayTpConfigData data = GSON.fromJson(jsonObject, PayTpConfigData.class);
      var invalidFields = data.validate();
      if (!invalidFields.isEmpty()) {
        LOGGER.error(
            "Invalid PayTp config fields: {}; using defaults",
            invalidFields
        );
        saveStatic(defaults);
        return defaults;
      }
      JsonObject normalizedJson = GSON.toJsonTree(data).getAsJsonObject();

      if (hasMissing || !jsonObject.equals(normalizedJson)) {
        LOGGER.info("Normalizing config file at {}", CONFIG_PATH);
        saveStatic(data);
      }

      LOGGER.info("Loaded PayTp config from {}", CONFIG_PATH);
      return data;

    } catch (Exception e) {
      LOGGER.error("Failed to load PayTp config from {}, using defaults", CONFIG_PATH, e);
      saveStatic(defaults);
      return defaults;
    }
  }

  private static boolean mergeDefaults(JsonObject target, JsonObject defaults) {
    boolean hasMissing = false;
    for (var entry : defaults.entrySet()) {
      String key = entry.getKey();
      JsonElement defaultValue = entry.getValue();

      if (!target.has(key) || target.get(key).isJsonNull()) {
        target.add(key, defaultValue);
        hasMissing = true;
      } else if (defaultValue.isJsonObject() && target.get(key).isJsonObject()) {
        if (mergeDefaults(target.getAsJsonObject(key), defaultValue.getAsJsonObject())) {
          hasMissing = true;
        }
      }
    }
    return hasMissing;
  }

  private static void saveStatic(PayTpConfigData data) {
    try {
      Files.createDirectories(CONFIG_PATH.getParent());
    } catch (IOException e) {
      LOGGER.error("Failed to create config directory", e);
      return;
    }

    try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
      GSON.toJson(data, writer);
      writer.flush();
      LOGGER.info("Saved PayTp config to {}", CONFIG_PATH);
    } catch (IOException e) {
      LOGGER.error("Failed to save PayTp config", e);
    }
  }

  // ============================
  // ====== Update Config =======
  // ============================

  /**
   * Validates, persists, and activates a replacement configuration.
   *
   * @param newData the complete replacement configuration
   * @return {@code true} if validation and persistence succeeded; otherwise {@code false}
   */
  public boolean update(PayTpConfigData newData) {
    if (newData == null) {
      LOGGER.warn("Attempted to update with null data, ignoring");
      return false;
    }

    lock.writeLock().lock();
    try {
      var invalidFields = newData.validate();
      if (!invalidFields.isEmpty()) {
        LOGGER.error(
            "Refusing to save invalid PayTp config fields: {}",
            invalidFields
        );
        return false;
      }
      saveStatic(newData);
      this.data = newData;
      LOGGER.info("Config updated successfully");
      return true;
    } catch (Exception e) {
      LOGGER.error("Failed to update config", e);
      return false;
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Reloads the configuration from disk and replaces the active snapshot.
   *
   * @return {@code true} if the reload completed; otherwise {@code false}
   */
  public boolean reload() {
    lock.writeLock().lock();
    try {
      this.data = loadData();
      LOGGER.info("Config reloaded successfully");
      return true;
    } catch (Exception e) {
      LOGGER.error("Failed to reload config", e);
      return false;
    } finally {
      lock.writeLock().unlock();
    }
  }
}
