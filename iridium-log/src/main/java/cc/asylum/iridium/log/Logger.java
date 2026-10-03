package cc.asylum.iridium.log;

import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.helpers.MessageFormatter;

public final class Logger extends AbstractLogger {

  private final int threshold;
  private final String name;

  public Logger(final String name, final int threshold) {
    this.name = name;
    this.threshold = threshold;
  }

  @Override
  public boolean isTraceEnabled() {
    return isEnabled(Level.TRACE);
  }

  @Override
  public boolean isTraceEnabled(final Marker marker) {
    return isTraceEnabled();
  }

  @Override
  public boolean isDebugEnabled() {
    return isEnabled(Level.DEBUG);
  }

  @Override
  public boolean isDebugEnabled(final Marker marker) {
    return isDebugEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return isEnabled(Level.INFO);
  }

  @Override
  public boolean isInfoEnabled(final Marker marker) {
    return isInfoEnabled();
  }

  @Override
  public boolean isWarnEnabled() {
    return isEnabled(Level.WARN);
  }

  @Override
  public boolean isWarnEnabled(final Marker marker) {
    return isWarnEnabled();
  }

  @Override
  public boolean isErrorEnabled() {
    return isEnabled(Level.ERROR);
  }

  @Override
  public boolean isErrorEnabled(final Marker marker) {
    return isErrorEnabled();
  }

  @Override
  protected String getFullyQualifiedCallerName() {
    return Logger.class.getName();
  }

  @Override
  protected void handleNormalizedLoggingCall(
      final Level level,
      final Marker marker,
      final String message,
      final Object[] arguments,
      final Throwable throwable) {

    final String formattedMessage = MessageFormatter.basicArrayFormat(message, arguments);

    final Throwable cause = throwable != null
        ? throwable
        : MessageFormatter.getThrowableCandidate(arguments);

    LoggerFormat.write(
        level,
        name,
        formattedMessage,
        cause);
  }

  private boolean isEnabled(final Level level) {
    return level.toInt() >= threshold;
  }
}
