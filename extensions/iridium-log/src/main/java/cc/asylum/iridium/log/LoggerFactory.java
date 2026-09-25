package cc.asylum.iridium.log;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class LoggerFactory implements ILoggerFactory {

  private static final String LEVEL_PREFIX = "iridium.log.level.";
  private static final String ROOT_LEVEL_PROPERTY = "iridium.log.level";
  private static final String ROOT_LEVEL_ENV = "IRIDIUM_LOG_LEVEL";

  private final int rootThreshold;
  private final Map<String, Integer> thresholds;
  private final ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<>();

  public LoggerFactory() {
    final var rootLevelEnv = System.getenv()
        .getOrDefault(ROOT_LEVEL_ENV, "INFO");

    rootThreshold = levelValue(
        System.getProperty(ROOT_LEVEL_PROPERTY, rootLevelEnv),
        Level.INFO.toInt());

    thresholds = loadThresholds();
  }

  @Override
  public Logger getLogger(final String name) {
    return loggers.computeIfAbsent(
        name, loggerName -> new cc.asylum.iridium.log.Logger(loggerName, thresholdFor(loggerName)));
  }

  private int thresholdFor(final String loggerName) {
    int threshold = rootThreshold;
    int longestMatch = -1;

    for (final Entry<String, Integer> entry : thresholds.entrySet()) {
      final String prefix = entry.getKey();

      if (prefix.length() <= longestMatch) {
        continue;
      }

      if (loggerName.equals(prefix) || loggerName.startsWith(prefix + '.')) {
        threshold = entry.getValue();
        longestMatch = prefix.length();
      }
    }

    return threshold;
  }

  private static Map<String, Integer> loadThresholds() {
    final Map<String, Integer> thresholds = new HashMap<>();

    for (final Entry<Object, Object> entry : System.getProperties().entrySet()) {
      if (!(entry.getKey() instanceof String key)
          || !key.startsWith(LEVEL_PREFIX)
          || !(entry.getValue() instanceof String value)) {
        continue;
      }

      final String loggerName = key.substring(LEVEL_PREFIX.length());

      if (loggerName.isEmpty()) {
        continue;
      }

      thresholds.put(
          loggerName,
          levelValue(value, Level.INFO.toInt()));
    }

    return thresholds;
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
