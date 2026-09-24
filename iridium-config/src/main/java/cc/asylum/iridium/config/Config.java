package cc.asylum.iridium.config;

import cc.asylum.iridium.core.result.Result;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.IntFunction;

public final class Config {

  private static final Object LOCK = new Object();
  private static volatile Result<Sources, ConfigError> loaded;
  private static String[] args = new String[0];

  private Config() {
  }

  public static void prepare(final String[] args) {
    synchronized (LOCK) {
      if (loaded != null) {
        return;
      }
      Config.args = args == null ? new String[0] : args.clone();
    }
  }

  public static boolean has(final String key) {
    final Result<Sources, ConfigError> sources = sources();
    return sources.isOk() && sources.unwrap().has(key);
  }

  public static boolean present(final String prefix) {
    final Result<Sources, ConfigError> sources = sources();
    return sources.isOk() && sources.unwrap().present(prefix);
  }

  public static String find(final String key) {
    final Result<Sources, ConfigError> sources = sources();
    return sources.isOk() ? sources.unwrap().get(key) : null;
  }

  public static Result<String, ConfigError> string(final String key) {
    return required(key, Result::ok);
  }

  public static Result<String, ConfigError> string(final String key, final String fallback) {
    return orElse(key, fallback, Result::ok);
  }

  public static Result<Integer, ConfigError> integer(final String key) {
    return required(key, Config::parseInt);
  }

  public static Result<Integer, ConfigError> integer(final String key, final int fallback) {
    return orElse(key, fallback, Config::parseInt);
  }

  public static Result<Long, ConfigError> longValue(final String key) {
    return required(key, Config::parseLong);
  }

  public static Result<Long, ConfigError> longValue(final String key, final long fallback) {
    return orElse(key, fallback, Config::parseLong);
  }

  public static Result<Double, ConfigError> doubleValue(final String key) {
    return required(key, Config::parseDouble);
  }

  public static Result<Double, ConfigError> doubleValue(final String key, final double fallback) {
    return orElse(key, fallback, Config::parseDouble);
  }

  public static Result<Float, ConfigError> floatValue(final String key) {
    return required(key, Config::parseFloat);
  }

  public static Result<Float, ConfigError> floatValue(final String key, final float fallback) {
    return orElse(key, fallback, Config::parseFloat);
  }

  public static Result<Short, ConfigError> shortValue(final String key) {
    return required(key, Config::parseShort);
  }

  public static Result<Short, ConfigError> shortValue(final String key, final short fallback) {
    return orElse(key, fallback, Config::parseShort);
  }

  public static Result<Byte, ConfigError> byteValue(final String key) {
    return required(key, Config::parseByte);
  }

  public static Result<Byte, ConfigError> byteValue(final String key, final byte fallback) {
    return orElse(key, fallback, Config::parseByte);
  }

  public static Result<Boolean, ConfigError> bool(final String key) {
    return required(key, Config::parseBool);
  }

  public static Result<Boolean, ConfigError> bool(final String key, final boolean fallback) {
    return orElse(key, fallback, Config::parseBool);
  }

  public static Result<Boolean, ConfigError> parseBool(final String value) {
    return switch (value.trim().toLowerCase(Locale.ROOT)) {
      case "true", "yes", "on", "1" -> Result.ok(true);
      case "false", "no", "off", "0" -> Result.ok(false);
      default -> Result.err(ConfigError.of("Invalid boolean '" + value + "'"));
    };
  }

  public static <E extends Enum<E>> Result<E, ConfigError> enumeration(
      final String key,
      final Class<E> type
  ) {
    return required(key, value -> parseEnum(value, type));
  }

  public static <E extends Enum<E>> Result<E, ConfigError> enumeration(
      final String key,
      final Class<E> type,
      final E fallback
  ) {
    return orElse(key, fallback, value -> parseEnum(value, type));
  }

  public static <E extends Enum<E>> Result<E, ConfigError> parseEnum(
      final String value,
      final Class<E> type
  ) {
    for (final E constant : type.getEnumConstants()) {
      if (constant.name().equalsIgnoreCase(value.trim())) {
        return Result.ok(constant);
      }
    }
    return Result.err(ConfigError.of("No enum constant " + type.getSimpleName() + "." + value));
  }

  public static Result<Integer, ConfigError> parseInt(final String value) {
    return parse(value, Integer::parseInt);
  }

  public static Result<Long, ConfigError> parseLong(final String value) {
    return parse(value, Long::parseLong);
  }

  public static Result<Double, ConfigError> parseDouble(final String value) {
    return parse(value, Double::parseDouble);
  }

  public static Result<Float, ConfigError> parseFloat(final String value) {
    return parse(value, Float::parseFloat);
  }

  public static Result<Short, ConfigError> parseShort(final String value) {
    return parse(value, Short::parseShort);
  }

  public static Result<Byte, ConfigError> parseByte(final String value) {
    return parse(value, Byte::parseByte);
  }

  public static <T> Result<List<T>, ConfigError> list(
      final String key,
      final Function<String, Result<T, ConfigError>> convert
  ) {
    return sources().flatMap(loaded -> readList(loaded, key, convert));
  }

  public static <T> Result<List<T>, ConfigError> indexed(
      final String prefix,
      final Function<Integer, Result<T, ConfigError>> factory
  ) {
    return sources().flatMap(loaded -> {
      final List<Result<T, ConfigError>> items = new ArrayList<>();
      for (int i = 0; loaded.hasIndex(prefix, i); i++) {
        items.add(factory.apply(i));
      }
      return sequence(items);
    });
  }

  public static <T> Result<Map<String, T>, ConfigError> map(
      final String prefix,
      final Function<String, Result<T, ConfigError>> convert
  ) {
    return sources().flatMap(loaded -> {
      final Map<String, T> result = new LinkedHashMap<>();
      for (final Map.Entry<String, String> entry : loaded.children(prefix).entrySet()) {
        final Result<T, ConfigError> value = convert.apply(entry.getValue());
        if (value.isErr()) {
          return Result.err(value.unwrapErr());
        }
        result.put(entry.getKey(), value.unwrap());
      }
      return Result.ok(Map.copyOf(result));
    });
  }

  public static <T> Result<T, ConfigError> missing(final String key) {
    return Result.err(ConfigError.missing(key));
  }

  private static <T> Result<List<T>, ConfigError> readList(
      final Sources loaded,
      final String key,
      final Function<String, Result<T, ConfigError>> convert
  ) {
    if (loaded.hasIndex(key, 0)) {
      final List<Result<T, ConfigError>> indexed = new ArrayList<>();
      for (int i = 0; loaded.hasIndex(key, i); i++) {
        final String indexedKey = key + "[" + i + "]";
        final String raw = loaded.get(indexedKey);
        indexed.add(raw == null
            ? Result.err(ConfigError.missing(indexedKey))
            : convert.apply(raw).mapErr(error -> ConfigError.invalid(indexedKey, raw)));
      }
      return sequence(indexed);
    }

    final String raw = loaded.get(key);
    if (raw == null || raw.isEmpty()) {
      return Result.ok(List.of());
    }

    final List<Result<T, ConfigError>> items = new ArrayList<>();
    for (final String part : raw.split(",", -1)) {
      final String trimmed = part.trim();
      if (!trimmed.isEmpty()) {
        items.add(convert.apply(trimmed).mapErr(error -> ConfigError.invalid(key, trimmed)));
      }
    }
    return sequence(items);
  }

  private static <T> Result<List<T>, ConfigError> sequence(final List<Result<T, ConfigError>> items) {
    final List<T> values = new ArrayList<>();
    for (final Result<T, ConfigError> item : items) {
      if (item.isErr()) {
        return Result.err(item.unwrapErr());
      }
      values.add(item.unwrap());
    }
    return Result.ok(List.copyOf(values));
  }

  private static <T> Result<T, ConfigError> required(
      final String key,
      final Function<String, Result<T, ConfigError>> parse
  ) {
    return sources().flatMap(loaded -> {
      final String value = loaded.get(key);
      if (value == null) {
        return Result.err(ConfigError.missing(key));
      }
      return parse.apply(value).mapErr(error -> ConfigError.invalid(key, value));
    });
  }

  private static <T> Result<T, ConfigError> orElse(
      final String key,
      final T fallback,
      final Function<String, Result<T, ConfigError>> parse
  ) {
    return sources().flatMap(loaded -> {
      final String value = loaded.get(key);
      if (value == null) {
        return Result.ok(fallback);
      }
      return parse.apply(value).mapErr(error -> ConfigError.invalid(key, value));
    });
  }

  private static <T> Result<T, ConfigError> parse(
      final String value,
      final Function<String, T> parser
  ) {
    try {
      return Result.ok(parser.apply(value.trim()));
    } catch (final RuntimeException exception) {
      return Result.err(ConfigError.of("Invalid value '" + value + "'"));
    }
  }

  private static Result<Sources, ConfigError> sources() {
    final Result<Sources, ConfigError> current = loaded;
    if (current != null) {
      return current;
    }

    synchronized (LOCK) {
      if (loaded == null) {
        loaded = Sources.load(loader(), args, System.getenv(), systemProperties());
      }
      return loaded;
    }
  }

  private static ClassLoader loader() {
    final ClassLoader context = Thread.currentThread().getContextClassLoader();
    return context == null ? Config.class.getClassLoader() : context;
  }

  private static Map<String, String> systemProperties() {
    final Properties properties = System.getProperties();
    final Map<String, String> system = new LinkedHashMap<>();
    for (final String name : properties.stringPropertyNames()) {
      system.put(name, properties.getProperty(name));
    }
    return system;
  }
}
