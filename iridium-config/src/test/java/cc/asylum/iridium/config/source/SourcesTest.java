package cc.asylum.iridium.config.source;

import cc.asylum.iridium.config.ConfigError;
import cc.asylum.iridium.core.result.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourcesTest {

  @TempDir
  Path temp;

  @Test
  void argsOverrideEnvironmentSystemAndFiles() throws Exception {
    Files.writeString(temp.resolve("application.yaml"), """
        app:
          name: from-file
          title: file-title
        server:
          port: 1
        list:
          - a
          - b
        """);
    final Map<String, String> env = new LinkedHashMap<>();
    env.put("APP_NAME", "from-env");
    env.put("APP_TITLE", "from-env-title");
    env.put("FEATURE_FLAG", "env-only");
    final Map<String, String> system = new LinkedHashMap<>();
    system.put("app.name", "from-system");
    system.put("server.port", "2");
    system.put("relaxedKey", "relaxed");

    final Sources sources = Sources.load(
        loader(),
        new String[] {
            "--app.name=from-args",
            "--bare",
            "--",
            null,
            "--flag",
            "yes",
            "--empty=",
            "--alone"
        },
        env,
        system).unwrap();

    assertEquals("from-args", sources.get("app.name"));
    assertEquals("from-env-title", sources.get("app.title"));
    assertEquals("2", sources.get("server.port"));
    assertNull(sources.get("missing.file"));
    assertEquals("relaxed", sources.get("relaxedKey"));
    assertEquals("yes", sources.get("flag"));
    assertEquals("", sources.get("empty"));
    assertEquals("true", sources.get("alone"));
    assertEquals("true", sources.get("bare"));
    assertTrue(sources.has("app.name"));
    assertTrue(sources.has("feature.flag"));
    assertFalse(sources.has("no.such"));
    assertTrue(sources.present("app"));
    assertTrue(sources.present("feature"));
    assertTrue(sources.present("list"));
    assertFalse(sources.present("absent"));
    assertTrue(sources.hasIndex("list", 0));
    assertTrue(sources.hasIndex("list", 1));
    assertFalse(sources.hasIndex("list", 2));
    assertEquals(Map.of("name", "from-args", "title", "from-env-title"), sources.children("app"));
    assertEquals(Map.of("flag", "env-only"), sources.children("feature"));
  }

  @Test
  void profilesComeFromArgsThenSystemThenEnvironment() throws Exception {
    Files.writeString(temp.resolve("application-dev.yaml"), "mode: dev\n");
    Files.writeString(temp.resolve("application-qa.yaml"), "mode: qa\n");
    final Map<String, String> env = Map.of("IRIDIUM_PROFILES", "qa");
    final Map<String, String> system = Map.of("iridium.profiles", "dev");

    final Sources fromArgs = Sources.load(loader(), new String[] { "--iridium.profiles=qa" }, env, system).unwrap();
    assertEquals("qa", fromArgs.get("mode"));

    final Sources fromSystem = Sources.load(loader(), new String[0], env, system).unwrap();
    assertEquals("dev", fromSystem.get("mode"));

    final Sources fromEnv = Sources.load(loader(), null, env, Map.of()).unwrap();
    assertEquals("qa", fromEnv.get("mode"));
  }

  @Test
  void invalidProfileFailsLoad() {
    final Result<Sources, ConfigError> loaded = Sources.load(
        new EmptyLoader(),
        new String[] { "--iridium.profiles=bad profile" },
        Map.of(),
        Map.of());
    assertTrue(loaded.isErr());
    assertEquals("Invalid configuration profile 'bad profile'", loaded.unwrapErr().message());
  }

  @Test
  void blankProfilesAreIgnoredAndNullEnvironmentIsEmpty() {
    final Result<Sources, ConfigError> loaded = Sources.load(
        new EmptyLoader(),
        new String[] { "--iridium.profiles=   " },
        null,
        null);
    assertTrue(loaded.isOk());
    assertFalse(loaded.unwrap().has("anything"));
    assertFalse(loaded.unwrap().present("anything"));
    assertEquals(Map.of(), loaded.unwrap().children(""));
    assertEquals(Map.of(), loaded.unwrap().children("app"));
  }

  @Test
  void fileReadFailurePropagates() throws Exception {
    Files.writeString(temp.resolve("application.yaml"), "server: [\n");
    final Result<Sources, ConfigError> loaded = Sources.load(loader(), new String[0], Map.of(), Map.of());
    assertTrue(loaded.isErr());
    assertEquals("Failed to read application.yaml", loaded.unwrapErr().message());
  }

  @Test
  void indexedAndCanonicalKeysAreDetected() {
    final Map<String, String> system = new LinkedHashMap<>();
    system.put("items[0].name", "a");
    system.put("widgets[0]", "w");
    system.put("Server.Host", "localhost");
    final Sources sources = Sources.load(new EmptyLoader(), new String[0], Map.of(), system).unwrap();

    assertTrue(sources.hasIndex("items", 0));
    assertTrue(sources.hasIndex("widgets", 0));
    assertFalse(sources.hasIndex("server.host", 0));
    assertTrue(sources.present("server"));
    assertTrue(sources.present("Server.Host"));
    assertEquals("localhost", sources.get("server.host"));
    assertEquals(Map.of(), sources.children("items"));
    assertEquals(Map.of("host", "localhost"), sources.children("server"));
  }

  @Test
  void environmentChildrenSkipNestedAndBlankRemainders() {
    final Map<String, String> env = new LinkedHashMap<>();
    env.put("APP_NAME", "n");
    env.put("APP_NESTED_VALUE", "skip");
    env.put("APP_", "blank");
    env.put("ROOT", "root");
    final Sources sources = Sources.load(new EmptyLoader(), new String[0], env, Map.of()).unwrap();
    assertEquals(Map.of("name", "n"), sources.children("app"));
    assertFalse(sources.children("").isEmpty());
    assertTrue(sources.present("app"));
    assertFalse(sources.present(""));
  }

  @Test
  void flattenDelegatesToConfigFiles() {
    final Map<String, String> out = new LinkedHashMap<>();
    Sources.flatten("root", Map.of("child", 7), out);
    assertEquals("7", out.get("root.child"));
  }

  @Test
  void argsWithoutValueBecomeTrueAndEqualsWins() {
    final Sources sources = Sources.load(
        new EmptyLoader(),
        new String[] { "--switch", "--next", "--with=value", "ignored", "--spaced", "kept" },
        Map.of(),
        Map.of()).unwrap();
    assertEquals("true", sources.get("switch"));
    assertEquals("true", sources.get("next"));
    assertEquals("value", sources.get("with"));
    assertEquals("kept", sources.get("spaced"));
  }

  private URLClassLoader loader() throws IOException {
    return new URLClassLoader(new URL[] { temp.toUri().toURL() }, null);
  }

  private static final class EmptyLoader extends ClassLoader {

    private EmptyLoader() {
      super(null);
    }

    @Override
    public URL getResource(final String name) {
      return null;
    }
  }
}
