package cc.asylum.iridium.config;

import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.result.Unit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

final class Sources {

  private static final Pattern PROFILE = Pattern.compile("[A-Za-z0-9_-]+");

  private final Layer arguments;
  private final Layer system;
  private final Layer files;
  private final Map<String, String> environment;

  private Sources(
      final Layer arguments,
      final Layer system,
      final Layer files,
      final Map<String, String> environment
  ) {
    this.arguments = arguments;
    this.system = system;
    this.files = files;
    this.environment = environment;
  }

  static Result<Sources, ConfigError> load(
      final ClassLoader loader,
      final String[] args,
      final Map<String, String> environment,
      final Map<String, String> systemProperties
  ) {
    final Layer arguments = Layer.of(parseArgs(args == null ? new String[0] : args));
    final Layer system = Layer.of(systemProperties);
    final Map<String, String> env = environment == null ? Map.of() : environment;
    return profiles(arguments, system, env).flatMap(names -> {
      final Layer files = new Layer();
      Result<Unit, ConfigError> loaded = ConfigFiles.load(loader, files, "");
      for (final String profile : names) {
        loaded = loaded.flatMap(ignored -> ConfigFiles.load(loader, files, profile));
      }
      return loaded.map(ignored -> new Sources(arguments, system, files, env));
    });
  }

  static void flatten(
      final String prefix,
      final Object node,
      final Map<String, String> out
  ) {
    ConfigFiles.flatten(prefix, node, out);
  }

  String get(final String key) {
    if (arguments.contains(key)) {
      return arguments.get(key);
    }

    final String env = fromEnvironment(key);
    if (env != null) {
      return env;
    }
    if (system.contains(key)) {
      return system.get(key);
    }
    return files.get(key);
  }

  boolean has(final String key) {
    return arguments.contains(key)
        || fromEnvironment(key) != null
        || system.contains(key)
        || files.contains(key);
  }

  boolean present(final String prefix) {
    if (anyStored(key -> matchesPrefix(key, prefix))) {
      return true;
    }

    final String envPrefix = Keys.separate(prefix).toUpperCase(Locale.ROOT);
    for (final String envKey : environment.keySet()) {
      if (envKey.equals(envPrefix) || envKey.startsWith(envPrefix + "_")) {
        return true;
      }
    }
    return false;
  }

  boolean hasIndex(
      final String prefix,
      final int index
  ) {
    final String token = prefix + "[" + index + "]";
    final String canonical = Keys.canonical(token);
    return anyStored(key -> key.equals(token)
        || key.startsWith(token + ".")
        || key.startsWith(token + "[")
        || Keys.canonical(key).equals(canonical)
        || Keys.canonical(key).startsWith(canonical + ".")
        || Keys.canonical(key).startsWith(canonical + "["));
  }

  Map<String, String> children(final String prefix) {
    final Map<String, String> result = new LinkedHashMap<>();
    for (final String key : storedKeys()) {
      final String child = leafChild(key, prefix);
      if (child != null) {
        result.putIfAbsent(child, get(Keys.join(prefix, child)));
      }
    }

    final String envPrefix = prefix.isEmpty()
        ? ""
        : Keys.separate(prefix).toUpperCase(Locale.ROOT) + "_";
    for (final String envKey : environment.keySet()) {
      if (!prefix.isEmpty() && !envKey.startsWith(envPrefix)) {
        continue;
      }

      final String remainder = prefix.isEmpty() ? envKey : envKey.substring(envPrefix.length());
      if (remainder.isEmpty() || remainder.contains("_")) {
        continue;
      }
      final String child = remainder.toLowerCase(Locale.ROOT);
      result.putIfAbsent(child, get(Keys.join(prefix, child)));
    }
    return result;
  }

  private String fromEnvironment(final String key) {
    for (final String candidate : Keys.envCandidates(key)) {
      if (environment.containsKey(candidate)) {
        return environment.get(candidate);
      }
    }
    return null;
  }

  private boolean anyStored(final Predicate<String> predicate) {
    for (final String key : storedKeys()) {
      if (predicate.test(key)) {
        return true;
      }
    }
    return false;
  }

  private Set<String> storedKeys() {
    final Set<String> keys = new LinkedHashSet<>();
    keys.addAll(files.exact.keySet());
    keys.addAll(system.exact.keySet());
    keys.addAll(arguments.exact.keySet());
    return keys;
  }

  private static String leafChild(
      final String key,
      final String prefix
  ) {
    final String remainder = remainder(key, prefix);
    if (remainder == null || remainder.isEmpty() || remainder.indexOf('.') >= 0 || remainder.indexOf('[') >= 0) {
      return null;
    }
    return remainder;
  }

  private static String remainder(
      final String key,
      final String prefix
  ) {
    if (prefix.isEmpty()) {
      return key;
    }
    if (key.startsWith(prefix + ".")) {
      return key.substring(prefix.length() + 1);
    }

    final String canonicalPrefix = Keys.canonical(prefix) + ".";
    final String canonicalKey = Keys.canonical(key);
    if (canonicalKey.startsWith(canonicalPrefix)) {
      return canonicalKey.substring(canonicalPrefix.length());
    }
    return null;
  }

  private static boolean matchesPrefix(
      final String key,
      final String prefix
  ) {
    if (key.equals(prefix) || key.startsWith(prefix + ".") || key.startsWith(prefix + "[")) {
      return true;
    }

    final String canonKey = Keys.canonical(key);
    final String canonPrefix = Keys.canonical(prefix);
    return canonKey.equals(canonPrefix)
        || canonKey.startsWith(canonPrefix + ".")
        || canonKey.startsWith(canonPrefix + "[");
  }

  private static Result<List<String>, ConfigError> profiles(
      final Layer arguments,
      final Layer system,
      final Map<String, String> environment
  ) {
    final String raw = firstNonBlank(
        arguments.get("iridium.profiles"),
        system.get("iridium.profiles"),
        environment.get("IRIDIUM_PROFILES")
    );
    if (raw == null) {
      return Result.ok(List.of());
    }

    final List<String> names = new ArrayList<>();
    for (final String part : raw.split(",")) {
      final String name = part.trim();
      if (name.isEmpty()) {
        continue;
      }
      if (!PROFILE.matcher(name).matches()) {
        return Result.err(ConfigError.of("Invalid configuration profile '" + name + "'"));
      }
      names.add(name);
    }
    return Result.ok(names);
  }

  private static String firstNonBlank(final String... values) {
    for (final String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private static Map<String, String> parseArgs(final String[] args) {
    final Map<String, String> result = new LinkedHashMap<>();
    for (int i = 0; i < args.length; i++) {
      final String arg = args[i];
      if (arg == null || !arg.startsWith("--") || arg.length() == 2) {
        continue;
      }

      final String body = arg.substring(2);
      final int eq = body.indexOf('=');
      if (eq >= 0) {
        result.put(body.substring(0, eq), body.substring(eq + 1));
        continue;
      }
      if (i + 1 < args.length && args[i + 1] != null && !args[i + 1].startsWith("--")) {
        result.put(body, args[++i]);
        continue;
      }
      result.put(body, "true");
    }
    return result;
  }

  static final class Layer {

    private final Map<String, String> exact = new LinkedHashMap<>();
    private final Map<String, String> relaxed = new LinkedHashMap<>();

    private static Layer of(final Map<String, String> values) {
      final Layer layer = new Layer();
      if (values != null) {
        values.forEach(layer::put);
      }
      return layer;
    }

    void put(
        final String key,
        final String value
    ) {
      if (key == null || value == null) {
        return;
      }
      exact.put(key, value);
      relaxed.put(Keys.canonical(key), value);
    }

    private String get(final String key) {
      if (exact.containsKey(key)) {
        return exact.get(key);
      }
      return relaxed.get(Keys.canonical(key));
    }

    private boolean contains(final String key) {
      return exact.containsKey(key) || relaxed.containsKey(Keys.canonical(key));
    }
  }
}
