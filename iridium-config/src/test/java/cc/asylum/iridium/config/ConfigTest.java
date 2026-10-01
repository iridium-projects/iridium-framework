package cc.asylum.iridium.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigTest {

  @TempDir
  Path temp;

  @Test
  void readsTypedValuesFromClasspathFiles() throws Exception {
    Files.writeString(temp.resolve("application.yaml"), """
        app:
          name: iridium
          enabled: true
          ratio: 1.5
          count: 3
          big: 9
          small: 2
          tiny: 1
          mode: DEV
        tags: a, b
        items:
          - 4
          - 5
        nodes:
          - name: one
        table:
          alpha: 10
          beta: 11
        """);
    try (Isolated isolated = isolate(new String[0])) {
      assertTrue(isolated.bool("has", String.class, "app.name"));
      assertTrue(isolated.bool("present", String.class, "app"));
      assertFalse(isolated.bool("present", String.class, "missing"));
      assertEquals("iridium", isolated.call("find", String.class, "app.name"));
      assertNull(isolated.call("find", String.class, "missing"));
      assertEquals("iridium", isolated.ok("string", new Class<?>[] { String.class }, "app.name"));
      assertEquals("fallback", isolated.ok("string", new Class<?>[] { String.class, String.class }, "missing", "fallback"));
      assertEquals(3, isolated.ok("integer", new Class<?>[] { String.class }, "app.count"));
      assertEquals(7, isolated.ok("integer", new Class<?>[] { String.class, int.class }, "missing", 7));
      assertEquals(9L, isolated.ok("longValue", new Class<?>[] { String.class }, "app.big"));
      assertEquals(8L, isolated.ok("longValue", new Class<?>[] { String.class, long.class }, "missing", 8L));
      assertEquals(1.5d, isolated.ok("doubleValue", new Class<?>[] { String.class }, "app.ratio"));
      assertEquals(1.25d, isolated.ok("doubleValue", new Class<?>[] { String.class, double.class }, "missing", 1.25d));
      assertEquals(1.5f, isolated.ok("floatValue", new Class<?>[] { String.class }, "app.ratio"));
      assertEquals(1.25f, isolated.ok("floatValue", new Class<?>[] { String.class, float.class }, "missing", 1.25f));
      assertEquals((short) 2, isolated.ok("shortValue", new Class<?>[] { String.class }, "app.small"));
      assertEquals((short) 4, isolated.ok("shortValue", new Class<?>[] { String.class, short.class }, "missing", (short) 4));
      assertEquals((byte) 1, isolated.ok("byteValue", new Class<?>[] { String.class }, "app.tiny"));
      assertEquals((byte) 4, isolated.ok("byteValue", new Class<?>[] { String.class, byte.class }, "missing", (byte) 4));
      assertEquals(Boolean.TRUE, isolated.ok("bool", new Class<?>[] { String.class }, "app.enabled"));
      assertEquals(Boolean.FALSE, isolated.ok("bool", new Class<?>[] { String.class, boolean.class }, "missing", false));
      assertEquals("DEV", String.valueOf(isolated.ok("enumeration", new Class<?>[] { String.class, Class.class }, "app.mode", isolated.mode)));
      assertEquals("QA", String.valueOf(isolated.ok(
          "enumeration",
          new Class<?>[] { String.class, Class.class, Enum.class },
          "missing",
          isolated.mode,
          isolated.constant("QA"))));
      assertEquals(List.of("a", "b"), isolated.list("tags"));
      assertEquals(List.of(4, 5), isolated.listInts("items"));
      assertEquals(List.of(), isolated.listInts("absent"));
      assertEquals(Map.of("alpha", 10, "beta", 11), isolated.map("table"));
      assertEquals(List.of(0), isolated.indexed("nodes"));
      assertEquals("Missing required configuration key 'gone'", isolated.missing("gone"));
    }
  }

  @Test
  void missingAndInvalidValues() throws Exception {
    Files.writeString(temp.resolve("application.properties"),
        "port=nope\nflag=maybe\nmode=nope\nnums=1,x\nbad[0]=1\nbad[1]=nope\nnested[0].name=one\n");
    try (Isolated isolated = isolate(new String[0])) {
      assertEquals("Missing required configuration key 'name'", isolated.err("string", new Class<?>[] { String.class }, "name"));
      assertEquals("Invalid value 'nope' for configuration key 'port'", isolated.err("integer", new Class<?>[] { String.class }, "port"));
      assertEquals("Invalid value 'nope' for configuration key 'port'", isolated.err("integer", new Class<?>[] { String.class, int.class }, "port", 7));
      assertEquals("Invalid value 'nope' for configuration key 'port'", isolated.err("longValue", new Class<?>[] { String.class }, "port"));
      assertEquals("Invalid value 'nope' for configuration key 'port'", isolated.err("longValue", new Class<?>[] { String.class, long.class }, "port", 1L));
      assertEquals("Invalid value 'nope' for configuration key 'port'", isolated.err("doubleValue", new Class<?>[] { String.class }, "port"));
      assertEquals("Invalid value 'nope' for configuration key 'port'", isolated.err("doubleValue", new Class<?>[] { String.class, double.class }, "port", 1d));
      assertEquals("Invalid value 'nope' for configuration key 'port'", isolated.err("floatValue", new Class<?>[] { String.class }, "port"));
      assertEquals("Invalid value 'nope' for configuration key 'port'", isolated.err("floatValue", new Class<?>[] { String.class, float.class }, "port", 1f));
      assertEquals("Invalid value 'nope' for configuration key 'port'", isolated.err("shortValue", new Class<?>[] { String.class }, "port"));
      assertEquals(
          "Invalid value 'nope' for configuration key 'port'",
          isolated.err("shortValue", new Class<?>[] { String.class, short.class }, "port", (short) 1));
      assertEquals("Invalid value 'nope' for configuration key 'port'", isolated.err("byteValue", new Class<?>[] { String.class }, "port"));
      assertEquals(
          "Invalid value 'nope' for configuration key 'port'",
          isolated.err("byteValue", new Class<?>[] { String.class, byte.class }, "port", (byte) 1));
      assertEquals(Boolean.FALSE, isolated.ok("bool", new Class<?>[] { String.class }, "flag"));
      assertEquals(Boolean.FALSE, isolated.ok("bool", new Class<?>[] { String.class, boolean.class }, "flag", true));
      assertEquals(
          "Invalid value 'nope' for configuration key 'mode'",
          isolated.err("enumeration", new Class<?>[] { String.class, Class.class }, "mode", isolated.mode));
      assertEquals(
          "Invalid value 'nope' for configuration key 'mode'",
          isolated.err("enumeration", new Class<?>[] { String.class, Class.class, Enum.class }, "mode", isolated.mode, isolated.constant("DEV")));
      assertEquals("Invalid value 'x' for configuration key 'nums'", isolated.listError("nums"));
      assertEquals("Invalid value 'nope' for configuration key 'bad[1]'", isolated.listError("bad"));
      assertEquals("Missing required configuration key 'nested[0]'", isolated.listError("nested"));
      assertEquals("Invalid value 'nope'", isolated.mapError());
    }
  }

  @Test
  void parsersRejectBlankAndGarbage() throws Exception {
    try (Isolated isolated = isolate(new String[0])) {
      assertEquals("Invalid boolean. Value can't be blank or null", isolated.parseError("parseBool", "  "));
      assertEquals("Invalid boolean. Value can't be blank or null", isolated.parseError("parseBool", null));
      assertEquals(Boolean.TRUE, isolated.parseOk("parseBool", "true"));
      assertEquals(Boolean.FALSE, isolated.parseOk("parseBool", "false"));
      assertEquals(Boolean.FALSE, isolated.parseOk("parseBool", "nope"));
      assertEquals("DEV", String.valueOf(isolated.parseEnum("dev")));
      assertTrue(isolated.parseEnumError(" ").contains("No enum constant"));
      assertEquals(12, isolated.parseOk("parseInt", " 12 "));
      assertEquals("Invalid value 'x'", isolated.parseError("parseInt", "x"));
      assertEquals(12L, isolated.parseOk("parseLong", "12"));
      assertEquals("Invalid value 'x'", isolated.parseError("parseLong", "x"));
      assertEquals(1.5d, isolated.parseOk("parseDouble", "1.5"));
      assertEquals("Invalid value 'x'", isolated.parseError("parseDouble", "x"));
      assertEquals(1.5f, isolated.parseOk("parseFloat", "1.5"));
      assertEquals("Invalid value 'x'", isolated.parseError("parseFloat", "x"));
      assertEquals((short) 3, isolated.parseOk("parseShort", "3"));
      assertEquals("Invalid value 'x'", isolated.parseError("parseShort", "x"));
      assertEquals((byte) 3, isolated.parseOk("parseByte", "3"));
      assertEquals("Invalid value 'x'", isolated.parseError("parseByte", "x"));
    }
  }

  @Test
  void prepareIsIdempotentAndNullArgsAreEmpty() throws Exception {
    try (Isolated isolated = isolate(null)) {
      isolated.config.getMethod("prepare", String[].class).invoke(null, (Object) new String[] { "--late=1" });
      assertNull(isolated.call("find", String.class, "late"));
      isolated.config.getMethod("prepare", String[].class).invoke(null, new Object[] { null });
      assertNull(isolated.call("find", String.class, "late"));
    }
  }

  @Test
  void prepareBeforeLoadKeepsArgs() throws Exception {
    try (Isolated isolated = isolate(new String[] { "--app.name=from-args" })) {
      isolated.config.getMethod("prepare", String[].class).invoke(null, (Object) new String[] { "--app.name=later" });
      assertEquals("from-args", isolated.ok("string", new Class<?>[] { String.class }, "app.name"));
    }
  }

  @Test
  void brokenFileSurfacesAsConfigError() throws Exception {
    Files.writeString(temp.resolve("application.yaml"), "server: [\n");
    try (Isolated isolated = isolate(new String[0])) {
      assertEquals("Failed to read application.yaml", isolated.err("string", new Class<?>[] { String.class }, "server.port"));
      assertFalse(isolated.bool("has", String.class, "server.port"));
      assertNull(isolated.call("find", String.class, "server.port"));
      assertEquals("Failed to read application.yaml", isolated.err("string", new Class<?>[] { String.class, String.class }, "server.port", "fallback"));
    }
  }

  @Test
  void usesContextClassLoaderWhenPresent() throws Exception {
    Files.writeString(temp.resolve("application.properties"), "from=loader\n");
    try (URLClassLoader loader = new URLClassLoader(codeUrls(), ClassLoader.getPlatformClassLoader())) {
      final Class<?> config = loader.loadClass(Config.class.getName());
      final Thread thread = Thread.currentThread();
      final ClassLoader previous = thread.getContextClassLoader();
      thread.setContextClassLoader(new URLClassLoader(new URL[] { temp.toUri().toURL() }, null));
      try {
        final Object found = config.getMethod("string", String.class).invoke(null, "from");
        assertEquals("loader", found.getClass().getMethod("unwrap").invoke(found));
      } finally {
        thread.setContextClassLoader(previous);
      }
    }
  }

  @Test
  void fallsBackWhenContextClassLoaderIsNull() throws Exception {
    Files.writeString(temp.resolve("application.properties"), "from=self\n");
    try (URLClassLoader loader = new URLClassLoader(classpath(), ClassLoader.getPlatformClassLoader())) {
      final Class<?> config = loader.loadClass(Config.class.getName());
      final Thread thread = Thread.currentThread();
      final ClassLoader previous = thread.getContextClassLoader();
      thread.setContextClassLoader(null);
      try {
        final Object found = config.getMethod("string", String.class).invoke(null, "from");
        assertEquals("self", found.getClass().getMethod("unwrap").invoke(found));
      } finally {
        thread.setContextClassLoader(previous);
      }
    }
  }

  @Test
  void systemPropertyIsVisible() throws Exception {
    final Properties properties = System.getProperties();
    final String previous = properties.getProperty("iridium.config.test.token");
    properties.setProperty("iridium.config.test.token", "seen");
    try (Isolated isolated = isolate(new String[0])) {
      assertEquals("seen", isolated.ok("string", new Class<?>[] { String.class }, "iridium.config.test.token"));
    } finally {
      if (previous == null) {
        properties.remove("iridium.config.test.token");
      } else {
        properties.setProperty("iridium.config.test.token", previous);
      }
    }
  }

  private Isolated isolate(final String[] args) throws Exception {
    final URLClassLoader loader = new URLClassLoader(classpath(), ClassLoader.getPlatformClassLoader());
    final Class<?> config = loader.loadClass(Config.class.getName());
    final Thread thread = Thread.currentThread();
    final ClassLoader previous = thread.getContextClassLoader();
    thread.setContextClassLoader(loader);
    try {
      if (args != null) {
        config.getMethod("prepare", String[].class).invoke(null, (Object) args);
      } else {
        config.getMethod("prepare", String[].class).invoke(null, new Object[] { null });
      }
      config.getMethod("has", String.class).invoke(null, "warmup");
    } finally {
      thread.setContextClassLoader(previous);
    }
    return new Isolated(loader, config);
  }

  private URL[] classpath() throws Exception {
    return new URL[] { temp.toUri().toURL(), codeUrls()[0], codeUrls()[1], codeUrls()[2] };
  }

  private URL[] codeUrls() {
    return new URL[] {
        Config.class.getProtectionDomain().getCodeSource().getLocation(),
        cc.asylum.iridium.core.result.Result.class.getProtectionDomain().getCodeSource().getLocation(),
        org.yaml.snakeyaml.Yaml.class.getProtectionDomain().getCodeSource().getLocation()
    };
  }

  private final class Isolated implements AutoCloseable {

    private final URLClassLoader loader;
    private final Class<?> config;
    private final Class<?> errorType;
    private final Class<?> mode;

    private Isolated(final URLClassLoader loader, final Class<?> config) throws Exception {
      this.loader = loader;
      this.config = config;
      this.errorType = loader.loadClass(ConfigError.class.getName());
      this.mode = Mode.class;
    }

    private Object constant(final String name) {
      return Enum.valueOf(mode.asSubclass(Enum.class), name);
    }

    private Object call(final String name, final Class<?> type, final Object arg) throws Exception {
      return config.getMethod(name, type).invoke(null, arg);
    }

    private boolean bool(final String name, final Class<?> type, final Object arg) throws Exception {
      return (Boolean) call(name, type, arg);
    }

    private Object ok(final String name, final Class<?>[] types, final Object... args) throws Exception {
      final Object result = config.getMethod(name, types).invoke(null, args);
      assertTrue((Boolean) result.getClass().getMethod("isOk").invoke(result));
      return result.getClass().getMethod("unwrap").invoke(result);
    }

    private String err(final String name, final Class<?>[] types, final Object... args) throws Exception {
      final Object result = config.getMethod(name, types).invoke(null, args);
      assertTrue((Boolean) result.getClass().getMethod("isErr").invoke(result));
      return message(result);
    }

    private Object parseOk(final String name, final String value) throws Exception {
      final Object result = config.getMethod(name, String.class).invoke(null, value);
      assertTrue((Boolean) result.getClass().getMethod("isOk").invoke(result));
      return result.getClass().getMethod("unwrap").invoke(result);
    }

    private String parseError(final String name, final String value) throws Exception {
      return message(config.getMethod(name, String.class).invoke(null, value));
    }

    private Object parseEnum(final String value) throws Exception {
      final Object result = config.getMethod("parseEnum", String.class, Class.class).invoke(null, value, mode);
      assertTrue((Boolean) result.getClass().getMethod("isOk").invoke(result));
      return result.getClass().getMethod("unwrap").invoke(result);
    }

    private String parseEnumError(final String value) throws Exception {
      return message(config.getMethod("parseEnum", String.class, Class.class).invoke(null, value, mode));
    }

    private Object list(final String key) throws Exception {
      final Object result = config.getMethod("list", String.class, Function.class)
          .invoke(null, key, (Function<String, Object>) value -> {
            try {
              return config.getMethod("string", String.class, String.class).invoke(null, "unused", value);
            } catch (final Exception exception) {
              throw new IllegalStateException(exception);
            }
          });
      return result.getClass().getMethod("unwrap").invoke(result);
    }

    private Object listInts(final String key) throws Exception {
      final Object result = config.getMethod("list", String.class, Function.class)
          .invoke(null, key, (Function<String, Object>) value -> {
            try {
              return config.getMethod("parseInt", String.class).invoke(null, value);
            } catch (final Exception exception) {
              throw new IllegalStateException(exception);
            }
          });
      assertTrue((Boolean) result.getClass().getMethod("isOk").invoke(result));
      return result.getClass().getMethod("unwrap").invoke(result);
    }

    private String listError(final String key) throws Exception {
      final Object result = config.getMethod("list", String.class, Function.class)
          .invoke(null, key, (Function<String, Object>) value -> {
            try {
              return config.getMethod("parseInt", String.class).invoke(null, value);
            } catch (final Exception exception) {
              throw new IllegalStateException(exception);
            }
          });
      return message(result);
    }

    private Object map(final String prefix) throws Exception {
      final Object result = config.getMethod("map", String.class, Function.class)
          .invoke(null, prefix, (Function<String, Object>) value -> {
            try {
              return config.getMethod("parseInt", String.class).invoke(null, value);
            } catch (final Exception exception) {
              throw new IllegalStateException(exception);
            }
          });
      return result.getClass().getMethod("unwrap").invoke(result);
    }

    private String mapError() throws Exception {
      final Object result = config.getMethod("map", String.class, Function.class)
          .invoke(null, "", (Function<String, Object>) value -> {
            try {
              return config.getMethod("parseInt", String.class).invoke(null, value);
            } catch (final Exception exception) {
              throw new IllegalStateException(exception);
            }
          });
      return message(result);
    }

    private Object indexed(final String prefix) throws Exception {
      final Object result = config.getMethod("indexed", String.class, Function.class)
          .invoke(null, prefix, (Function<Integer, Object>) index -> {
            try {
              return config.getMethod("integer", String.class, int.class).invoke(null, "missing", index);
            } catch (final Exception exception) {
              throw new IllegalStateException(exception);
            }
          });
      return result.getClass().getMethod("unwrap").invoke(result);
    }

    private String missing(final String key) throws Exception {
      return message(config.getMethod("missing", String.class).invoke(null, key));
    }

    private String message(final Object result) throws Exception {
      final Object error = result.getClass().getMethod("unwrapErr").invoke(result);
      return (String) errorType.getMethod("message").invoke(error);
    }

    @Override
    public void close() throws Exception {
      loader.close();
    }
  }

  public enum Mode {
    DEV,
    QA
  }
}
