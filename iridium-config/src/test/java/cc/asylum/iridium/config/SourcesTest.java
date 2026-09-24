package cc.asylum.iridium.config;

import cc.asylum.iridium.core.result.Result;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourcesTest {

  @Test
  void flattensYaml() {
    final var flat = new java.util.LinkedHashMap<String, String>();
    Sources.flatten("", Map.of(
        "server", Map.of("max-threads", 4, "ssl", Map.of("enabled", true)),
        "names", java.util.List.of("a", "b")), flat);
    assertEquals("4", flat.get("server.max-threads"));
    assertEquals("true", flat.get("server.ssl.enabled"));
    assertEquals("a", flat.get("names[0]"));
    assertEquals("b", flat.get("names[1]"));
  }

  @Test
  void laterSourcesOverrideAndProfilesApply() throws Exception {
    final Path root = Files.createTempDirectory("iridium-config");
    Files.writeString(root.resolve("application.properties"), "server.port=1\napp.name=file\nlabels.home=x\n");
    Files.writeString(root.resolve("application.yml"), "server:\n  maxThreads: 4\nnames:\n  - a\n  - b\n");
    Files.writeString(root.resolve("application-dev.properties"), "server.port=2\n");
    try (var loader = new URLClassLoader(new URL[] {root.toUri().toURL()}, null)) {
      final var loaded = Sources.load(loader, new String[] {"--server.port=9"},
          Map.of("SERVER_MAX_THREADS", "8", "IRIDIUM_PROFILES", "dev"), Map.of());
      assertTrue(loaded.isOk(), () -> String.valueOf(loaded.err()));
      final var sources = loaded.unwrap();
      assertEquals("9", sources.get("server.port"));
      assertEquals("8", sources.get("server.maxThreads"));
      assertEquals("file", sources.get("app.name"));
      assertEquals("a", sources.get("names[0]"));
      assertTrue(sources.present("server"));
      assertTrue(sources.hasIndex("names", 1));
      assertFalse(sources.hasIndex("names", 2));
      assertEquals("x", sources.children("labels").get("home"));
    }
  }

  @Test
  void missingKeyIsAbsent() throws Exception {
    final Path root = Files.createTempDirectory("iridium-config-empty");
    try (var loader = new URLClassLoader(new URL[] {root.toUri().toURL()}, null)) {
      final var loaded = Sources.load(loader, new String[0], Map.of(), Map.of());
      assertTrue(loaded.isOk());
      final var sources = loaded.unwrap();
      assertNull(sources.get("server.port"));
      assertFalse(sources.has("server.port"));
    }
  }

  @Test
  void rejectsInvalidProfile() throws Exception {
    final Path root = Files.createTempDirectory("iridium-config-profile");
    try (var loader = new URLClassLoader(new URL[] {root.toUri().toURL()}, null)) {
      final Result<Sources, ConfigError> loaded = Sources.load(loader, new String[0], Map.of(),
          Map.of("iridium.profiles", "../x"));
      assertTrue(loaded.isErr());
    }
  }

  @Test
  void parsesScalars() {
    assertTrue(Config.parseBool("yes").unwrap());
    assertFalse(Config.parseBool("off").unwrap());
    assertEquals(Color.RED, Config.parseEnum("red", Color.class).unwrap());
    assertTrue(Config.parseBool("maybe").isErr());
  }

  private enum Color {
    RED
  }
}
