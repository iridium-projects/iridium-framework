package cc.asylum.iridium.log;

import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.helpers.MessageFormatter;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class IridiumLogger extends AbstractLogger {

  private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
  private static final Object LOCK = new Object();

  private final int threshold;
  private final boolean colors;
  private final String shortName;

  IridiumLogger(final String name, final int threshold, final boolean colors) {
    this.name = name;
    this.threshold = threshold;
    this.colors = colors;
    this.shortName = shorten(name);
  }

  @Override
  public boolean isTraceEnabled() {
    return enabled(Level.TRACE);
  }

  @Override
  public boolean isTraceEnabled(final Marker marker) {
    return enabled(Level.TRACE);
  }

  @Override
  public boolean isDebugEnabled() {
    return enabled(Level.DEBUG);
  }

  @Override
  public boolean isDebugEnabled(final Marker marker) {
    return enabled(Level.DEBUG);
  }

  @Override
  public boolean isInfoEnabled() {
    return enabled(Level.INFO);
  }

  @Override
  public boolean isInfoEnabled(final Marker marker) {
    return enabled(Level.INFO);
  }

  @Override
  public boolean isWarnEnabled() {
    return enabled(Level.WARN);
  }

  @Override
  public boolean isWarnEnabled(final Marker marker) {
    return enabled(Level.WARN);
  }

  @Override
  public boolean isErrorEnabled() {
    return enabled(Level.ERROR);
  }

  @Override
  public boolean isErrorEnabled(final Marker marker) {
    return enabled(Level.ERROR);
  }

  @Override
  protected String getFullyQualifiedCallerName() {
    return IridiumLogger.class.getName();
  }

  @Override
  protected void handleNormalizedLoggingCall(final Level level, final Marker marker,
      final String message, final Object[] args, final Throwable throwable) {
    final String text = MessageFormatter.basicArrayFormat(message, args);
    final Throwable thrown = throwable != null ? throwable : MessageFormatter.getThrowableCandidate(args);
    final String line = TIMESTAMP.format(LocalDateTime.now())
        + "  " + levelToken(level)
        + " [" + Thread.currentThread().getName() + "] "
        + shortName + " - " + text;
    final PrintStream out = level.toInt() >= Level.WARN.toInt() ? System.err : System.out;
    synchronized (LOCK) {
      out.println(line);
      if (thrown != null) {
        thrown.printStackTrace(out);
      }
    }
  }

  private boolean enabled(final Level level) {
    return level.toInt() >= threshold;
  }

  private String levelToken(final Level level) {
    final String padded = String.format("%-5s", level);
    if (!colors) {
      return padded;
    }
    return switch (level) {
      case ERROR -> "\u001B[1;31m" + padded + "\u001B[0m";
      case WARN -> "\u001B[1;33m" + padded + "\u001B[0m";
      case INFO -> "\u001B[1;32m" + padded + "\u001B[0m";
      case DEBUG -> "\u001B[1;36m" + padded + "\u001B[0m";
      case TRACE -> "\u001B[37m" + padded + "\u001B[0m";
    };
  }

  static String shorten(final String name) {
    final String[] parts = name.split("\\.");
    if (parts.length == 1) {
      return name;
    }
    final StringBuilder result = new StringBuilder();
    for (int i = 0; i < parts.length - 1; i++) {
      if (!parts[i].isEmpty()) {
        result.append(parts[i].charAt(0)).append('.');
      }
    }
    return result.append(parts[parts.length - 1]).toString();
  }
}
