package cc.asylum.iridium.log;

import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
  }
}
