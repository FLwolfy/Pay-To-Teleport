package com.flwolfy.paytp.data.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PayTpLanguageLoaderTest {

  @TempDir
  Path root;

  @Test
  void discoversEveryLanguageJsonWithoutAJavaRegistry() throws Exception {
    write("en_us", "English", "Hello");
    write("ja_jp", "日本語", "こんにちは");
    write("zh_cn", "简体中文", "你好");

    var languages = new PayTpLanguageLoader(List.of(root)).load();

    assertEquals(List.of("en_us", "ja_jp", "zh_cn"), List.copyOf(languages.keySet()));
    assertEquals("日本語", languages.get("ja_jp").name());
    assertEquals("こんにちは", languages.get("ja_jp").translations().get("paytp.test"));
  }

  @Test
  void requiresTheEnglishFallback() throws Exception {
    write("zh_cn", "简体中文", "你好");

    assertThrows(
        IllegalStateException.class,
        () -> new PayTpLanguageLoader(List.of(root)).load()
    );
  }

  @Test
  void rejectsLanguagesWithoutADisplayName() throws Exception {
    Path directory = languageDirectory();
    Files.writeString(
        directory.resolve("en_us.json"),
        "{\"paytp.test\":\"Hello\"}",
        StandardCharsets.UTF_8
    );

    assertThrows(
        IllegalStateException.class,
        () -> new PayTpLanguageLoader(List.of(root)).load()
    );
  }

  @Test
  void rejectsInvalidLocaleFileNames() throws Exception {
    write("en_us", "English", "Hello");
    Files.writeString(
        languageDirectory().resolve("invalid locale.json"),
        "{\"paytp.language.name\":\"Invalid\"}",
        StandardCharsets.UTF_8
    );

    assertThrows(
        IllegalStateException.class,
        () -> new PayTpLanguageLoader(List.of(root)).load()
    );
  }

  private void write(String locale, String name, String text) throws Exception {
    Files.writeString(
        languageDirectory().resolve(locale + ".json"),
        "{\"paytp.language.name\":\"" + name
            + "\",\"paytp.test\":\"" + text + "\"}",
        StandardCharsets.UTF_8
    );
  }

  private Path languageDirectory() throws Exception {
    Path directory = root.resolve("assets/pay-to-teleport/lang");
    Files.createDirectories(directory);
    return directory;
  }
}
