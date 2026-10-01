package cc.asylum.iridium.config.source;

import cc.asylum.iridium.config.ConfigError;
import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.result.Unit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigFilesTest {

  @TempDir
  Path temp;

  @Test
  void loadReadsPropertiesThenYamlAndProfileOverrides() throws Exception {
    Files.writeString(temp.resolve("application.properties"), "server.port=8080\napp.name=base\n");
    Files.writeString(temp.resolve("application.yaml"), """
        server:
          host: localhost
        features:
          - one
          - two
        """);
    Files.writeString(temp.resolve("application-dev.yml"), """
        app:
          name: dev
        nested:
          flag: true
        """);
    final ClassLoader loader = new DirectoryLoader(temp);
    final SourceLayer files = new SourceLayer();

    final Result<Unit, ConfigError> loaded = ConfigFiles.load(loader, files, "")
        .flatMap(ignored -> ConfigFiles.load(loader, files, "dev"));

    assertTrue(loaded.isOk());
    assertEquals("8080", files.get("server.port"));
    assertEquals("localhost", files.get("server.host"));
    assertEquals("one", files.get("features[0]"));
    assertEquals("two", files.get("features[1]"));
    assertEquals("dev", files.get("app.name"));
    assertEquals("true", files.get("nested.flag"));
  }

  @Test
  void missingResourcesAreSkipped() {
    final SourceLayer files = new SourceLayer();
    final Result<Unit, ConfigError> loaded = ConfigFiles.load(new EmptyLoader(), files, "missing");
    assertTrue(loaded.isOk());
    assertTrue(files.keys().isEmpty());
  }

  @Test
  void emptyYamlDocumentIsIgnored() throws Exception {
    Files.writeString(temp.resolve("application.yaml"), "");
    final SourceLayer files = new SourceLayer();
    final Result<Unit, ConfigError> loaded = ConfigFiles.load(new DirectoryLoader(temp), files, "");
    assertTrue(loaded.isOk());
    assertTrue(files.keys().isEmpty());
  }

  @Test
  void yamlRootMustBeAMapping() throws Exception {
    Files.writeString(temp.resolve("application.yml"), "- just\n- a\n- list\n");
    final Result<Unit, ConfigError> loaded = ConfigFiles.load(new DirectoryLoader(temp), new SourceLayer(), "");
    assertTrue(loaded.isErr());
    assertEquals("application.yml must be a mapping", loaded.unwrapErr().message());
  }

  @Test
  void brokenYamlIsReported() throws Exception {
    Files.writeString(temp.resolve("application.yaml"), "server: [\n");
    final Result<Unit, ConfigError> loaded = ConfigFiles.load(new DirectoryLoader(temp), new SourceLayer(), "");
    assertTrue(loaded.isErr());
    assertEquals("Failed to read application.yaml", loaded.unwrapErr().message());
  }

  @Test
  void unreadablePropertiesAreReported() {
    final Result<Unit, ConfigError> loaded = ConfigFiles.load(
        new BrokenLoader("application.properties"),
        new SourceLayer(),
        "");
    assertTrue(loaded.isErr());
    assertEquals("Failed to read application.properties", loaded.unwrapErr().message());
  }

  @Test
  void unreadableYamlIsReported() {
    final Result<Unit, ConfigError> loaded = ConfigFiles.load(
        new BrokenLoader("application.yaml"),
        new SourceLayer(),
        "");
    assertTrue(loaded.isErr());
    assertEquals("Failed to read application.yaml", loaded.unwrapErr().message());
  }

  @Test
  void flattenSkipsNullsEmptyPrefixAndWalksNestedStructures() {
    final Map<String, String> out = new LinkedHashMap<>();
    final Map<String, Object> root = new LinkedHashMap<>();
    root.put("name", "iridium");
    root.put("skip", null);
    root.put("items", List.of("a", Map.of("id", 1)));
    ConfigFiles.flatten("", root, out);
    ConfigFiles.flatten("", null, out);
    ConfigFiles.flatten("", "scalar", out);
    assertEquals("iridium", out.get("name"));
    assertEquals("a", out.get("items[0]"));
    assertEquals("1", out.get("items[1].id"));
    assertEquals(3, out.size());
  }

  private static final class DirectoryLoader extends ClassLoader {

    private final Path root;

    private DirectoryLoader(final Path root) {
      super(null);
      this.root = root;
    }

    @Override
    public URL getResource(final String name) {
      final Path file = root.resolve(name);
      if (!Files.isRegularFile(file)) {
        return null;
      }
      try {
        return file.toUri().toURL();
      } catch (final IOException exception) {
        return null;
      }
    }
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

  private static final class BrokenLoader extends ClassLoader {

    private final String broken;

    private BrokenLoader(final String broken) {
      super(null);
      this.broken = broken;
    }

    @Override
    public URL getResource(final String name) {
      if (!broken.equals(name)) {
        return null;
      }
      try {
        return new URL("memory", "local", 0, "/" + name, new URLStreamHandler() {
          @Override
          protected URLConnection openConnection(final URL url) {
            return new URLConnection(url) {
              @Override
              public void connect() {
              }

              @Override
              public InputStream getInputStream() throws IOException {
                throw new IOException("unreadable");
              }
            };
          }
        });
      } catch (final IOException exception) {
        throw new IllegalStateException(exception);
      }
    }
  }

  }
