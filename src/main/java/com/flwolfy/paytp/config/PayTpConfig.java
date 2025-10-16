package com.flwolfy.paytp.config;

import com.flwolfy.paytp.PayTpMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * PayTpConfig handles loading and saving configuration for the Pay-to-Teleport mod.
 */
public class PayTpConfig {

  private static final Logger LOGGER = PayTpMod.LOGGER;
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final String CONFIG_FILE_NAME = "paytp.json";

  private static PayTpConfig instance;

  private final PayTpConfigData data;

  private PayTpConfig(PayTpConfigData data) {
    this.data = data;
  }

  /**
   * Returns the current config data.
   */
  public PayTpConfigData data() {
    return data;
  }

  /**
   * Singleton accessor.
   */
  public static PayTpConfig getInstance() {
    if (instance == null) instance = loadConfig();
    return instance;
  }

  // ========================================= //
  // ============= File Operations =========== //
  // ========================================= //

  private static PayTpConfig loadConfig() {
    File file = new File("config/" + CONFIG_FILE_NAME);
    if (!file.exists()) {
      PayTpConfigData defaults = PayTpConfigData.DEFAULT;
      saveStatic(defaults, file);
      return new PayTpConfig(defaults);
    }

    try (FileReader reader = new FileReader(file)) {
      PayTpConfigData data = GSON.fromJson(reader, PayTpConfigData.class);
      return new PayTpConfig(data);
    } catch (IOException e) {
      LOGGER.error("Failed to load PayTp config", e);
      return new PayTpConfig(PayTpConfigData.DEFAULT);
    }
  }

  public void save() {
    File file = new File("config/" + CONFIG_FILE_NAME);
    saveStatic(data, file);
  }

  private static void saveStatic(PayTpConfigData data, File file) {
    if (file.getParentFile().mkdirs()) {
      LOGGER.info("Successfully saved config file: " + file.getAbsolutePath());
    }
    try (FileWriter writer = new FileWriter(file)) {
      GSON.toJson(data, writer);
    } catch (IOException e) {
      LOGGER.error("Failed to save PayTp config", e);
    }
  }
}
