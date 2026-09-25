package cc.asylum.iridium.log;

import org.slf4j.event.Level;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class LoggerFormat {

  private static final Object OUTPUT_LOCK = new Object();
  private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private static final String ANSI_RESET = "\u001B[0m";
  private static final String ANSI_RED = "\u001B[1;31m";
  private static final String ANSI_YELLOW = "\u001B[1;33m";
  private static final String ANSI_GREEN = "\u001B[1;32m";
  private static final String ANSI_CYAN = "\u001B[1;36m";
  private static final String ANSI_WHITE = "\u001B[37m";
  private static final String SEPARATOR = " ";
  private static final String LOGGER_SEPARATOR = " - ";

  private LoggerFormat() {
  }

  static void write(
      final Level level,
      final String loggerName,
      final String message,
      final Throwable throwable) {
    final String output = format(level, loggerName, message, throwable);

    synchronized (OUTPUT_LOCK) {
      System.out.print(output);
      System.out.flush();
    }
  }

  private static String format(
      final Level level,
      final String loggerName,
      final String message,
      final Throwable throwable) {
    final StringBuilder output = new StringBuilder(256);

    output.append(TIMESTAMP.format(LocalDateTime.now()))
        .append(SEPARATOR)
        .append(levelToken(level))
        .append(" [")
        .append(Thread.currentThread().getName()).append("] ")
        .append(loggerName).append(LOGGER_SEPARATOR)
        .append(message).append('\n');

    if (throwable != null) {
      appendThrowable(output, throwable);
    }

    return output.toString();
  }

  private static String levelToken(final Level level) {
    return switch (level) {
      case ERROR -> ANSI_RED + "ERROR" + ANSI_RESET;
      case WARN -> ANSI_YELLOW + "WARN " + ANSI_RESET;
      case INFO -> ANSI_GREEN + "INFO " + ANSI_RESET;
      case DEBUG -> ANSI_CYAN + "DEBUG" + ANSI_RESET;
      case TRACE -> ANSI_WHITE + "TRACE" + ANSI_RESET;
    };
  }

  private static void appendThrowable(final StringBuilder output, final Throwable throwable) {
    final StringWriter buffer = new StringWriter(256);

    try (final PrintWriter writer = new PrintWriter(buffer)) {
      throwable.printStackTrace(writer);
    }

    output.append(buffer);
  }
}
