package com.flwolfy.paytp.data.lang;

import com.flwolfy.paytp.PayTpMod;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

final class PayTpLanguageLoader {

  static final String DEFAULT_LOCALE = "en_us";
  static final String NAME_KEY = "paytp.language.name";

  private static final Gson GSON = new Gson();
  private static final Pattern LOCALE = Pattern.compile("[a-z0-9][a-z0-9_-]*");
  private final List<Path> roots;

  PayTpLanguageLoader() {
    this(resolveModRoots());
  }

  PayTpLanguageLoader(List<Path> roots) {
    this.roots = List.copyOf(roots);
  }

  Map<String, PayTpLanguage> load() {
    Map<String, PayTpLanguage> discovered = new TreeMap<>();
    for (Path root : roots) {
      Path directory = root.resolve("assets").resolve(PayTpMod.MOD_ID).resolve("lang");
      if (Files.isDirectory(directory)) {
        discover(directory, discovered);
      }
    }
    if (!discovered.containsKey(DEFAULT_LOCALE)) {
      throw new IllegalStateException(
          "Missing bundled PayTp language " + DEFAULT_LOCALE + ".json"
      );
    }
    return Collections.unmodifiableMap(new LinkedHashMap<>(discovered));
  }

  private static void discover(
      Path directory,
      Map<String, PayTpLanguage> discovered
  ) {
    try (var paths = Files.list(directory)) {
      List<Path> languageFiles = paths
          .filter(Files::isRegularFile)
          .filter(PayTpLanguageLoader::isJson)
          .sorted(Comparator.comparing(path -> path.getFileName().toString()))
          .toList();
      for (Path path : languageFiles) {
        String locale = locale(path);
        PayTpLanguage language = read(path);
        if (discovered.putIfAbsent(locale, language) != null) {
          throw new IllegalStateException("Duplicate bundled PayTp language locale " + locale);
        }
      }
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Failed to scan bundled PayTp languages in " + directory,
          exception
      );
    }
  }

  private static List<Path> resolveModRoots() {
    ModContainer mod = FabricLoader.getInstance().getModContainer(PayTpMod.MOD_ID)
        .orElseThrow(() -> new IllegalStateException("Could not resolve PayTp mod container"));
    return mod.getRootPaths();
  }

  private static boolean isJson(Path path) {
    return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json");
  }

  private static String locale(Path path) {
    String fileName = path.getFileName().toString();
    String locale = fileName.substring(0, fileName.length() - ".json".length())
        .toLowerCase(Locale.ROOT);
    if (!LOCALE.matcher(locale).matches()) {
      throw new IllegalStateException(
          "Invalid bundled PayTp language filename " + path.getFileName()
      );
    }
    return locale;
  }

  private static PayTpLanguage read(Path path) {
    JsonElement parsed;
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      parsed = GSON.fromJson(reader, JsonElement.class);
    } catch (IOException | RuntimeException exception) {
      throw new IllegalStateException("Invalid bundled PayTp language JSON " + path, exception);
    }
    if (parsed == null || !parsed.isJsonObject()) {
      throw new IllegalStateException(
          "Bundled PayTp language must contain a JSON object: " + path
      );
    }

    JsonObject object = parsed.getAsJsonObject();
    Map<String, String> translations = new LinkedHashMap<>();
    object.entrySet().forEach(entry -> {
      JsonElement value = entry.getValue();
      if (value == null || !value.isJsonPrimitive()
          || !value.getAsJsonPrimitive().isString()) {
        throw new IllegalStateException(
            "Bundled PayTp language value must be a string: "
                + path + " -> " + entry.getKey()
        );
      }
      translations.put(entry.getKey(), value.getAsString());
    });

    String name = translations.get(NAME_KEY);
    if (name == null || name.isBlank()) {
      throw new IllegalStateException(
          "Bundled PayTp language is missing non-blank " + NAME_KEY + ": " + path
      );
    }
    return new PayTpLanguage(name, translations);
  }
}
