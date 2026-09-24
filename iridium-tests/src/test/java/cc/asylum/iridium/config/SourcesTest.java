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
import static org.junit.jupiter.api.Assertions.assertThrows;
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

  @Test
  void parsesScalarEdges() {
    assertTrue(Config.parseBool(" TRUE ").unwrap());
    assertTrue(Config.parseBool("1").unwrap());
    assertFalse(Config.parseBool("NO").unwrap());
    assertFalse(Config.parseBool("0").unwrap());
    assertTrue(Config.parseBool("").isErr());
    assertTrue(Config.parseBool(" ").isErr());
    assertEquals(Color.RED, Config.parseEnum(" RED ", Color.class).unwrap());
    assertTrue(Config.parseEnum("green", Color.class).isErr());
    assertTrue(Config.parseEnum("", Color.class).isErr());
    assertEquals(12, Config.parseInt(" 12 ").unwrap());
    assertEquals(3L, Config.parseLong("3").unwrap());
    assertEquals(1.5d, Config.parseDouble("1.5").unwrap());
    assertEquals(1.5f, Config.parseFloat("1.5").unwrap());
    assertEquals((short) 4, Config.parseShort("4").unwrap());
    assertEquals((byte) 5, Config.parseByte("5").unwrap());
    assertTrue(Config.parseInt("").isErr());
    assertTrue(Config.parseInt("999999999999").isErr());
    assertTrue(Config.parseLong("nope").isErr());
    assertTrue(Config.parseShort("40000").isErr());
    assertTrue(Config.parseByte("128").isErr());
    assertTrue(Double.isNaN(Config.parseDouble("NaN").unwrap()));
    assertTrue(Config.parseDouble("Infinity").unwrap().isInfinite());
    assertThrows(NullPointerException.class, () -> Config.parseBool(null));
    assertTrue(Config.parseInt(null).isErr());
    assertTrue(Config.parseLong(null).isErr());
    assertThrows(NullPointerException.class, () -> Config.parseEnum(null, Color.class));
    assertTrue(Config.missing("server.port").isErr());
    assertTrue(Config.missing("server.port").unwrapErr().message().contains("server.port"));
  }

  @Test
  void flattenIgnoresEmptyMapsNullsAndBareScalars() {
    final var flat = new java.util.LinkedHashMap<String, String>();
    Sources.flatten("", Map.of(), flat);
    Sources.flatten("", null, flat);
    Sources.flatten("", "bare", flat);
    Sources.flatten("items", java.util.List.of(Map.of("name", "ada"), "x"), flat);
    assertTrue(flat.containsKey("items[0].name"));
    assertEquals("ada", flat.get("items[0].name"));
    assertEquals("x", flat.get("items[1]"));
    assertFalse(flat.containsValue("bare"));
  }

  @Test
  void nullArgsAndEnvironmentStillLoad() throws Exception {
    final Path root = Files.createTempDirectory("iridium-config-null");
    try (var loader = new URLClassLoader(new URL[] {root.toUri().toURL()}, null)) {
      final var loaded = Sources.load(loader, null, null, null);
      assertTrue(loaded.isOk(), () -> String.valueOf(loaded.err()));
      assertNull(loaded.unwrap().get("server.port"));
    }
  }

  @Test
  void parsesFlagsSpacedArgsAndIgnoresNonOptions() throws Exception {
    final Path root = Files.createTempDirectory("iridium-config-args");
    Files.writeString(root.resolve("application.properties"), "server.port=1\nenabled=false\n");
    try (var loader = new URLClassLoader(new URL[] {root.toUri().toURL()}, null)) {
      final var loaded = Sources.load(loader,
          new String[] {"leftover", "--", "--enabled", "--server.port", "9", "--other"},
          Map.of(),
          Map.of());
      assertTrue(loaded.isOk(), () -> String.valueOf(loaded.err()));
      final var sources = loaded.unwrap();
      assertEquals("true", sources.get("enabled"));
      assertEquals("9", sources.get("server.port"));
      assertEquals("true", sources.get("other"));
      assertFalse(sources.has("leftover"));
    }
  }

  @Test
  void laterProfilesAndRelaxedKeysApply() throws Exception {
    final Path root = Files.createTempDirectory("iridium-config-profiles");
    Files.writeString(root.resolve("application.properties"), "server.max-threads=1\napp.name=base\n");
    Files.writeString(root.resolve("application-a.properties"), "app.name=a\n");
    Files.writeString(root.resolve("application-b.properties"), "app.name=b\n");
    try (var loader = new URLClassLoader(new URL[] {root.toUri().toURL()}, null)) {
      final var loaded = Sources.load(loader, new String[0], Map.of("IRIDIUM_PROFILES", "a, b"), Map.of());
      assertTrue(loaded.isOk(), () -> String.valueOf(loaded.err()));
      final var sources = loaded.unwrap();
      assertEquals("b", sources.get("app.name"));
      assertEquals("1", sources.get("server.maxThreads"));
      assertTrue(sources.present("app"));
    }
  }

  @Test
  void argumentsBeatEnvironmentWhichBeatsSystemProperties() throws Exception {
    final Path root = Files.createTempDirectory("iridium-config-order");
    Files.writeString(root.resolve("application.properties"), "server.port=1\n");
    try (var loader = new URLClassLoader(new URL[] {root.toUri().toURL()}, null)) {
      final var loaded = Sources.load(loader,
          new String[] {"--server.port=9"},
          Map.of("SERVER_PORT", "8"),
          Map.of("server.port", "7"));
      assertTrue(loaded.isOk(), () -> String.valueOf(loaded.err()));
      assertEquals("9", loaded.unwrap().get("server.port"));
    }
  }

  @Test
  void rejectsYamlThatIsNotAMappingAndBrokenYaml() throws Exception {
    final Path listRoot = Files.createTempDirectory("iridium-config-list");
    Files.writeString(listRoot.resolve("application.yml"), "- a\n");
    try (var loader = new URLClassLoader(new URL[] {listRoot.toUri().toURL()}, null)) {
      final var loaded = Sources.load(loader, new String[0], Map.of(), Map.of());
      assertTrue(loaded.isErr());
      assertTrue(loaded.unwrapErr().message().contains("must be a mapping"));
    }

    final Path broken = Files.createTempDirectory("iridium-config-broken");
    Files.writeString(broken.resolve("application.yml"), ":\n  - [\n");
    try (var loader = new URLClassLoader(new URL[] {broken.toUri().toURL()}, null)) {
      final var loaded = Sources.load(loader, new String[0], Map.of(), Map.of());
      assertTrue(loaded.isErr());
      assertTrue(loaded.unwrapErr().message().contains("Failed to read"));
    }
  }

  @Test
  void skipsBlankProfilesAndRejectsSpaces() throws Exception {
    final Path root = Files.createTempDirectory("iridium-config-blank-profile");
    Files.writeString(root.resolve("application-dev.properties"), "app.name=dev\n");
    try (var loader = new URLClassLoader(new URL[] {root.toUri().toURL()}, null)) {
      final var loaded = Sources.load(loader, new String[0], Map.of("IRIDIUM_PROFILES", ",dev,"), Map.of());
      assertTrue(loaded.isOk(), () -> String.valueOf(loaded.err()));
      assertEquals("dev", loaded.unwrap().get("app.name"));
    }
    try (var loader = new URLClassLoader(new URL[] {root.toUri().toURL()}, null)) {
      final var loaded = Sources.load(loader, new String[0], Map.of(), Map.of("iridium.profiles", "dev prod"));
      assertTrue(loaded.isErr());
    }
  }

  private enum Color {
    RED
  }
}
