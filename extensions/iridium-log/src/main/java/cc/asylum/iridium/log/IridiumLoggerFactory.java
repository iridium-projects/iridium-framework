package cc.asylum.iridium.log;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class IridiumLoggerFactory implements ILoggerFactory {

  private static final String LEVEL_PREFIX = "iridium.log.level.";

  private final int rootThreshold;
  private final Map<String, Integer> thresholds;
  private final boolean colors;
  private final ConcurrentMap<String, IridiumLogger> loggers = new ConcurrentHashMap<>();

  public IridiumLoggerFactory() {
    this.rootThreshold = levelValue(
        System.getProperty("iridium.log.level", System.getenv().getOrDefault("IRIDIUM_LOG_LEVEL", "INFO")),
        Level.INFO.toInt());
    this.thresholds = prefixedThresholds();
    this.colors = resolveColors();
  }

  @Override
  public Logger getLogger(final String name) {
    return loggers.computeIfAbsent(name, key -> new IridiumLogger(key, thresholdFor(key), colors));
  }

  private int thresholdFor(final String name) {
    int match = rootThreshold;
    int length = -1;
    for (final Map.Entry<String, Integer> entry : thresholds.entrySet()) {
      if (entry.getKey().length() > length
          && (name.equals(entry.getKey()) || name.startsWith(entry.getKey() + "."))) {
        match = entry.getValue();
        length = entry.getKey().length();
      }
    }
    return match;
  }

  private static Map<String, Integer> prefixedThresholds() {
    final Map<String, Integer> result = new HashMap<>();
    for (final Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
      if (entry.getKey() instanceof final String key && key.startsWith(LEVEL_PREFIX)) {
        final String logger = key.substring(LEVEL_PREFIX.length());
        if (!logger.isEmpty() && entry.getValue() instanceof final String value) {
          result.put(logger, levelValue(value, Level.INFO.toInt()));
        }
      }
    }
    return result;
  }

  private static boolean resolveColors() {
    final String mode = System.getProperty(
        "iridium.log.color", System.getenv().getOrDefault("IRIDIUM_LOG_COLOR", "auto"));
    return switch (mode.toLowerCase(Locale.ROOT)) {
      case "always", "true" -> true;
      case "never", "false" -> false;
      default -> System.console() != null
          && System.getenv("NO_COLOR") == null
          && !"dumb".equals(System.getenv("TERM"));
    };
  }

  static int levelValue(final String value, final int fallback) {
    if (value == null) {
      return fallback;
    }
    return switch (value.trim().toUpperCase(Locale.ROOT)) {
      case "ALL" -> Integer.MIN_VALUE;
      case "TRACE" -> Level.TRACE.toInt();
      case "DEBUG" -> Level.DEBUG.toInt();
      case "INFO" -> Level.INFO.toInt();
      case "WARN", "WARNING" -> Level.WARN.toInt();
      case "ERROR" -> Level.ERROR.toInt();
      case "OFF", "NONE" -> Integer.MAX_VALUE;
      default -> fallback;
    };
  }
}
