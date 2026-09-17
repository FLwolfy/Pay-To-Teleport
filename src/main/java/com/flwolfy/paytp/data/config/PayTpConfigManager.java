package com.flwolfy.paytp.data.config;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.lang.PayTpLangManager;
import com.flwolfy.paytp.data.script.PayTpScript;
import com.flwolfy.paytp.data.script.PayTpScriptAdapter;
import com.flwolfy.paytp.data.warp.PayTpWarpPermission;
import com.flwolfy.paytp.data.warp.PayTpWarpPermissionAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;

/**
 * Loads, validates, normalizes, updates, and persists the PayTp configuration.
 *
 * <p>Reads and writes are guarded by a shared lock. Missing fields are recursively populated from
 * {@link PayTpConfigData#DEFAULT}, and invalid files are replaced with the default configuration.</p>
 */
public class PayTpConfigManager {

  private static final Logger LOGGER = PayTpMod.LOGGER;
  private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
      .resolve("paytp.json");
  private static final Gson GSON;

  static {
    GsonBuilder gsonBuilder = new GsonBuilder();

    // ================================
    // Register customized adapter here
    // ================================
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

    try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
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

      PayTpConfigData data = canonicalizeLanguage(
          GSON.fromJson(jsonObject, PayTpConfigData.class)
      );
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

  private static PayTpConfigData canonicalizeLanguage(PayTpConfigData data) {
    String language = PayTpConfigData.canonicalLanguage(
        data.general().language(),
        PayTpConfigData.DEFAULT.general().language(),
        PayTpLangManager.getInstance().availableLocales()
    );
    if (language.equals(data.general().language())) {
      return data;
    }
    return new PayTpConfigData(
        new PayTpConfigData.General(
            language,
            data.general().helpCommand(),
            data.general().safeTeleport(),
            data.general().safeTeleportRange(),
            data.general().effect()
        ),
        data.teleport(),
        data.request(),
        data.home(),
        data.back(),
        data.warp(),
        data.price()
    );
  }

  private static boolean saveStatic(PayTpConfigData data) {
    try {
      Files.createDirectories(CONFIG_PATH.getParent());
      Path temporary = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
      try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
        GSON.toJson(data, writer);
      }
      try {
        Files.move(
            temporary,
            CONFIG_PATH,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE
        );
      } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
        Files.move(temporary, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
      }
      LOGGER.info("Saved PayTp config to {}", CONFIG_PATH);
      return true;
    } catch (Exception e) {
      LOGGER.error("Failed to save PayTp config", e);
      return false;
    }
  }

  public PayTpConfigData loadForEditing() {
    lock.writeLock().lock();
    try {
      return loadData();
    } finally {
      lock.writeLock().unlock();
    }
  }

  public boolean savePending(PayTpConfigData newData) {
    if (newData == null) {
      return false;
    }
    newData = canonicalizeLanguage(newData);
    var invalidFields = newData.validate();
    if (!invalidFields.isEmpty()) {
      LOGGER.error("Refusing to save invalid pending PayTp config fields: {}", invalidFields);
      return false;
    }

    lock.writeLock().lock();
    try {
      return saveStatic(newData);
    } finally {
      lock.writeLock().unlock();
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
    newData = canonicalizeLanguage(newData);

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
      if (!saveStatic(newData)) {
        return false;
      }
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
   * Loads the configuration from disk when a server starts and replaces the active snapshot.
   *
   * @return {@code true} if the startup load completed; otherwise {@code false}
   */
  public boolean loadAtServerStart() {
    lock.writeLock().lock();
    try {
      this.data = loadData();
      LOGGER.info("Config loaded for server startup successfully");
      return true;
    } catch (Exception e) {
      LOGGER.error("Failed to load config for server startup", e);
      return false;
    } finally {
      lock.writeLock().unlock();
    }
  }
}
