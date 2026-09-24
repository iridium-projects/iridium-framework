package cc.asylum.iridium.log;

import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.BasicMarkerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IridiumLogTest {

  @Test
  void shortensLoggerNames() {
    assertEquals("c.a.i.w.u.UndertowWebServer",
        IridiumLogger.shorten("cc.asylum.iridium.web.undertow.UndertowWebServer"));
    assertEquals("Root", IridiumLogger.shorten("Root"));
  }

  @Test
  void parsesLevels() {
    assertEquals(Level.DEBUG.toInt(), IridiumLoggerFactory.levelValue("debug", Level.INFO.toInt()));
    assertEquals(Level.WARN.toInt(), IridiumLoggerFactory.levelValue("WARNING", Level.INFO.toInt()));
    assertEquals(Level.INFO.toInt(), IridiumLoggerFactory.levelValue("nope", Level.INFO.toInt()));
    assertEquals(Level.INFO.toInt(), IridiumLoggerFactory.levelValue(null, Level.INFO.toInt()));
  }

  @Test
  void appliesThresholds() {
    final var logger = new IridiumLogger("test", Level.DEBUG.toInt(), false);
    assertTrue(logger.isDebugEnabled());
    assertTrue(logger.isInfoEnabled());
    assertFalse(logger.isTraceEnabled());
  }

  @Test
  void cachesLoggers() {
    final var factory = new IridiumLoggerFactory();
    assertSame(factory.getLogger("test"), factory.getLogger("test"));
    assertNotSame(factory.getLogger("test"), factory.getLogger("other"));
  }

  @Test
  void shortensEmptyAndBrokenNames() {
    assertEquals("", IridiumLogger.shorten(""));
    assertEquals("f.b.Baz", IridiumLogger.shorten("foo.bar.Baz"));
    assertEquals("f.Baz", IridiumLogger.shorten("foo..Baz"));
    assertEquals("a.", IridiumLogger.shorten("a."));
  }

  @Test
  void parsesLevelEdges() {
    assertEquals(Integer.MIN_VALUE, IridiumLoggerFactory.levelValue(" all ", Level.INFO.toInt()));
    assertEquals(Integer.MAX_VALUE, IridiumLoggerFactory.levelValue("OFF", Level.INFO.toInt()));
    assertEquals(Integer.MAX_VALUE, IridiumLoggerFactory.levelValue("none", Level.INFO.toInt()));
    assertEquals(Level.TRACE.toInt(), IridiumLoggerFactory.levelValue("trace", Level.INFO.toInt()));
    assertEquals(Level.ERROR.toInt(), IridiumLoggerFactory.levelValue("error", Level.INFO.toInt()));
    assertEquals(Level.WARN.toInt(), IridiumLoggerFactory.levelValue(" warn ", Level.INFO.toInt()));
    assertEquals(4, IridiumLoggerFactory.levelValue(" ", 4));
  }

  @Test
  void markerChecksFollowTheThreshold() {
    final var logger = new IridiumLogger("test", Level.WARN.toInt(), false);
    final Marker marker = new BasicMarkerFactory().getMarker("m");
    assertFalse(logger.isInfoEnabled(marker));
    assertTrue(logger.isWarnEnabled(marker));
    assertTrue(logger.isErrorEnabled(marker));
    assertFalse(logger.isDebugEnabled());
    assertEquals(IridiumLogger.class.getName(), logger.getFullyQualifiedCallerName());
  }

  @Test
  void writesColoredLinesToTheMatchingStream() {
    final var logger = new IridiumLogger("demo.Test", Level.INFO.toInt(), true);
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final ByteArrayOutputStream err = new ByteArrayOutputStream();
    final PrintStream previousOut = System.out;
    final PrintStream previousErr = System.err;
    System.setOut(new PrintStream(out));
    System.setErr(new PrintStream(err));
    try {
      logger.info("hello {}", "world");
      logger.warn("careful");
    } finally {
      System.setOut(previousOut);
      System.setErr(previousErr);
    }
    final String stdout = out.toString();
    final String stderr = err.toString();
    assertTrue(stdout.contains("d.Test"));
    assertTrue(stdout.contains("hello world"));
    assertTrue(stdout.contains("\u001B[1;32m"));
    assertTrue(stderr.contains("careful"));
    assertTrue(stderr.contains("\u001B[1;33m"));
    assertFalse(stdout.contains("careful"));
  }

  @Test
  void providerExposesSlf4jServices() {
    final var provider = new IridiumLogProvider();
    provider.initialize();
    assertEquals("2.0", provider.getRequestedApiVersion());
    assertSame(provider.getLoggerFactory().getLogger("provider"), provider.getLoggerFactory().getLogger("provider"));
    assertTrue(provider.getMarkerFactory() != null);
    assertTrue(provider.getMDCAdapter() != null);
  }
}
