package cc.asylum.iridium.log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.BasicMarkerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggerTest {

  @Mock
  private org.slf4j.Logger delegate;

  private PrintStream originalOut;
  private ByteArrayOutputStream captured;

  @BeforeEach
  void captureOut() {
    originalOut = System.out;
    captured = new ByteArrayOutputStream();
    System.setOut(new PrintStream(captured, true));
  }

  @AfterEach
  void restoreOut() {
    System.setOut(originalOut);
  }

  @Test
  void enabledChecksRespectThreshold() {
    final Logger logger = new Logger("app", Level.INFO.toInt());
    final Marker marker = new BasicMarkerFactory().getMarker("m");

    assertFalse(logger.isTraceEnabled());
    assertFalse(logger.isTraceEnabled(marker));
    assertFalse(logger.isDebugEnabled());
    assertFalse(logger.isDebugEnabled(marker));
    assertTrue(logger.isInfoEnabled());
    assertTrue(logger.isInfoEnabled(marker));
    assertTrue(logger.isWarnEnabled());
    assertTrue(logger.isWarnEnabled(marker));
    assertTrue(logger.isErrorEnabled());
    assertTrue(logger.isErrorEnabled(marker));
  }

  @Test
  void traceThresholdEnablesEveryLevel() {
    final Logger logger = new Logger("app", Level.TRACE.toInt());
    final Marker marker = new BasicMarkerFactory().getMarker("m");

    assertTrue(logger.isTraceEnabled());
    assertTrue(logger.isTraceEnabled(marker));
    assertTrue(logger.isDebugEnabled());
    assertTrue(logger.isDebugEnabled(marker));
    assertTrue(logger.isInfoEnabled(marker));
    assertTrue(logger.isWarnEnabled(marker));
    assertTrue(logger.isErrorEnabled(marker));
  }

  @Test
  void warnThresholdDisablesInfo() {
    final Logger logger = new Logger("app", Level.WARN.toInt());

    assertFalse(logger.isInfoEnabled());
    assertTrue(logger.isWarnEnabled());
    assertTrue(logger.isErrorEnabled());
  }

  @Test
  void errorThresholdDisablesWarn() {
    final Logger logger = new Logger("app", Level.ERROR.toInt());

    assertFalse(logger.isWarnEnabled());
    assertTrue(logger.isErrorEnabled());
  }

  @Test
  void offThresholdDisablesError() {
    final Logger logger = new Logger("app", Integer.MAX_VALUE);

    assertFalse(logger.isErrorEnabled());
    assertFalse(logger.isErrorEnabled(new BasicMarkerFactory().getMarker("m")));
  }

  @Test
  void disabledLevelDoesNotWrite() {
    final Logger logger = new Logger("app", Level.ERROR.toInt());

    logger.info("hidden");
    logger.debug("hidden {}", "arg");
    logger.trace("hidden", new IllegalStateException("nope"));
    logger.warn("hidden");

    assertEquals("", captured.toString());
  }

  @Test
  void infoWritesFormattedMessage() {
    final Logger logger = new Logger("svc", Level.INFO.toInt());

    logger.info("hello {}", "world");

    final String output = captured.toString();
    assertTrue(output.contains("INFO"));
    assertTrue(output.contains("svc - hello world"));
  }

  @Test
  void eachEnabledLevelWritesToken() {
    final Logger trace = new Logger("t", Level.TRACE.toInt());
    trace.trace("trace-msg");
    trace.debug("debug-msg");
    trace.info("info-msg");
    trace.warn("warn-msg");
    trace.error("error-msg");

    final String output = captured.toString();
    assertTrue(output.contains("TRACE"));
    assertTrue(output.contains("DEBUG"));
    assertTrue(output.contains("INFO"));
    assertTrue(output.contains("WARN"));
    assertTrue(output.contains("ERROR"));
    assertTrue(output.contains("trace-msg"));
    assertTrue(output.contains("error-msg"));
  }

  @Test
  void markerOverloadsWriteWhenEnabled() {
    final Logger logger = new Logger("marked", Level.DEBUG.toInt());
    final Marker marker = new BasicMarkerFactory().getMarker("req");

    logger.trace(marker, "skip");
    logger.debug(marker, "debug {}", "one");
    logger.info(marker, "info {} {}", "a", "b");
    logger.warn(marker, "warn {}", "w", new IllegalArgumentException("bad"));
    logger.error(marker, "error {}", new Object[] {"e"});

    final String output = captured.toString();
    assertFalse(output.contains("skip"));
    assertTrue(output.contains("debug one"));
    assertTrue(output.contains("info a b"));
    assertTrue(output.contains("warn w"));
    assertTrue(output.contains("IllegalArgumentException"));
    assertTrue(output.contains("error e"));
  }

  @Test
  void throwableArgumentIsPrintedWhenNoExplicitThrowable() {
    final Logger logger = new Logger("boom", Level.ERROR.toInt());
    final RuntimeException failure = new RuntimeException("exploded");

    logger.error("failed {}", "now", failure);

    final String output = captured.toString();
    assertTrue(output.contains("failed now"));
    assertTrue(output.contains("exploded"));
    assertTrue(output.contains("RuntimeException"));
  }

  @Test
  void explicitThrowableIsPreferred() {
    final Logger logger = new Logger("boom", Level.ERROR.toInt());

    logger.error("failed", new IllegalStateException("explicit"));

    assertTrue(captured.toString().contains("explicit"));
  }

  @Test
  void nullMessageAndNullArgsStillWrite() {
    final Logger logger = new Logger("nulls", Level.INFO.toInt());

    logger.info(null);
    logger.info("plain {}", (Object) null);
    logger.info("pair {} {}", null, null);

    final String output = captured.toString();
    assertTrue(output.contains("nulls - "));
    assertTrue(output.contains("plain null"));
    assertTrue(output.contains("pair null null"));
  }

  @Test
  void varargsAndTwoArgOverloadsFormat() {
    final Logger logger = new Logger("fmt", Level.INFO.toInt());

    logger.info("one {}", 1);
    logger.info("two {} {}", 2, 3);
    logger.warn("many {} {} {}", 4, 5, 6);
    logger.debug("nope");
    logger.error("err {}", 7);

    final String output = captured.toString();
    assertTrue(output.contains("one 1"));
    assertTrue(output.contains("two 2 3"));
    assertTrue(output.contains("many 4 5 6"));
    assertFalse(output.contains("nope"));
    assertTrue(output.contains("err 7"));
  }

  @Test
  void fluentAtLevelRespectsThreshold() {
    final Logger logger = new Logger("fluent", Level.WARN.toInt());

    logger.atInfo().log("quiet");
    logger.atWarn().log("loud {}", "yes");
    logger.atError().setCause(new IllegalStateException("cause")).log("bad");
    logger.atLevel(Level.DEBUG).log("hidden");
    logger.makeLoggingEventBuilder(Level.ERROR).log("built");

    final String output = captured.toString();
    assertFalse(output.contains("quiet"));
    assertFalse(output.contains("hidden"));
    assertTrue(output.contains("loud yes"));
    assertTrue(output.contains("bad"));
    assertTrue(output.contains("cause"));
    assertTrue(output.contains("built"));
  }

  @Test
  void isEnabledForLevelMatchesThreshold() {
    final Logger logger = new Logger("lvl", Level.INFO.toInt());

    assertFalse(logger.isEnabledForLevel(Level.DEBUG));
    assertTrue(logger.isEnabledForLevel(Level.INFO));
    assertTrue(logger.isEnabledForLevel(Level.ERROR));
  }

  @Test
  void getNameStaysUnsetBecauseConstructorDoesNotAssignSlf4jName() {
    final Logger logger = new Logger("named", Level.INFO.toInt());

    assertNull(logger.getName());
  }

  @Test
  void handleNormalizedLoggingCallFormatsAndExtractsThrowable() throws Exception {
    final Logger logger = new Logger("direct", Level.TRACE.toInt());
    final Method method = Logger.class.getDeclaredMethod(
        "handleNormalizedLoggingCall",
        Level.class,
        Marker.class,
        String.class,
        Object[].class,
        Throwable.class);
    method.setAccessible(true);
    method.invoke(logger, Level.INFO, null, "hello {}", new Object[] {"there"}, null);

    assertTrue(captured.toString().contains("hello there"));

    method.invoke(
        logger,
        Level.ERROR,
        new BasicMarkerFactory().getMarker("m"),
        "with cause",
        new Object[] {new IllegalStateException("candidate")},
        null);
    assertTrue(captured.toString().contains("candidate"));

    method.invoke(logger, Level.WARN, null, "explicit", null, new RuntimeException("given"));
    assertTrue(captured.toString().contains("given"));
  }

  @Test
  void callerNameIsLoggerClass() throws Exception {
    final Logger logger = new Logger("caller", Level.INFO.toInt());
    final Method method = Logger.class.getDeclaredMethod("getFullyQualifiedCallerName");
    method.setAccessible(true);

    assertEquals(Logger.class.getName(), method.invoke(logger));
  }

  @Test
  void mockedSlf4jDelegateIsNotUsedByThisLogger() {
    when(delegate.isInfoEnabled()).thenReturn(true);

    assertTrue(delegate.isInfoEnabled());
    verify(delegate).isInfoEnabled();
  }
}
