package cc.asylum.iridium.config;

import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.result.Unit;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

final class ConfigFiles {

  private ConfigFiles() {
  }

  static Result<Unit, ConfigError> load(
      final ClassLoader loader,
      final Sources.Layer files,
      final String profile) {
    final String suffix = profile.isEmpty() ? "" : "-" + profile;
    return loadProperties(loader, files, "application" + suffix + ".properties")
        .flatMap(ignored -> loadYaml(loader, files, "application" + suffix + ".yaml"))
        .flatMap(ignored -> loadYaml(loader, files, "application" + suffix + ".yml"));
  }

  static void flatten(
      final String prefix,
      final Object node,
      final Map<String, String> out) {
    if (node instanceof final Map<?, ?> map) {
      for (final Map.Entry<?, ?> entry : map.entrySet()) {
        flatten(Keys.join(prefix, String.valueOf(entry.getKey())), entry.getValue(), out);
      }
      return;
    }

    if (node instanceof final List<?> list) {
      for (int i = 0; i < list.size(); i++) {
        flatten(prefix + "[" + i + "]", list.get(i), out);
      }
      return;
    }

    if (node != null && !prefix.isEmpty()) {
      out.put(prefix, String.valueOf(node));
    }
  }

  private static Result<Unit, ConfigError> loadProperties(
      final ClassLoader loader,
      final Sources.Layer files,
      final String name) {
    final URL url = loader.getResource(name);
    if (url == null) {
      return Result.ok(Unit.INSTANCE);
    }

    try (InputStream in = url.openStream();
        Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
      final Properties properties = new Properties();
      properties.load(reader);
      for (final String key : properties.stringPropertyNames()) {
        files.put(key, properties.getProperty(key));
      }
      return Result.ok(Unit.INSTANCE);
    } catch (final IOException exception) {
      return Result.err(ConfigError.of("Failed to read " + name));
    }
  }

  private static Result<Unit, ConfigError> loadYaml(
      final ClassLoader loader,
      final Sources.Layer files,
      final String name) {
    final URL url = loader.getResource(name);
    if (url == null) {
      return Result.ok(Unit.INSTANCE);
    }

    try (InputStream in = url.openStream();
        Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
      final Object document = new Yaml(new SafeConstructor(new LoaderOptions())).load(reader);
      if (document == null) {
        return Result.ok(Unit.INSTANCE);
      }
      if (!(document instanceof Map<?, ?>)) {
        return Result.err(ConfigError.of(name + " must be a mapping"));
      }

      final Map<String, String> flat = new LinkedHashMap<>();
      flatten("", document, flat);
      flat.forEach(files::put);
      return Result.ok(Unit.INSTANCE);
    } catch (final RuntimeException | IOException exception) {
      return Result.err(ConfigError.of("Failed to read " + name));
    }
  }
}
