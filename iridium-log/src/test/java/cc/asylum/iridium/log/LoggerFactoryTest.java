package cc.asylum.iridium.log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggerFactoryTest {

  @AfterEach
  void clearLevelProperties() {
    final var keys = System.getProperties().stringPropertyNames().stream()
        .filter(key -> key.startsWith("iridium.log.level"))
        .toList();
    keys.forEach(System::clearProperty);
  }

  @Test
  void levelValueMapsKnownNamesAndFallback() {
    assertEquals(Integer.MIN_VALUE, LoggerFactory.levelValue("all", 1));
    assertEquals(Integer.MIN_VALUE, LoggerFactory.levelValue("  ALL  ", 1));
    assertEquals(Level.TRACE.toInt(), LoggerFactory.levelValue("trace", 1));
    assertEquals(Level.DEBUG.toInt(), LoggerFactory.levelValue("Debug", 1));
    assertEquals(Level.INFO.toInt(), LoggerFactory.levelValue("info", 1));
    assertEquals(Level.WARN.toInt(), LoggerFactory.levelValue("warn", 1));
    assertEquals(Level.WARN.toInt(), LoggerFactory.levelValue("warning", 1));
    assertEquals(Level.ERROR.toInt(), LoggerFactory.levelValue("ERROR", 1));
    assertEquals(Integer.MAX_VALUE, LoggerFactory.levelValue("off", 1));
    assertEquals(Integer.MAX_VALUE, LoggerFactory.levelValue("none", 1));
    assertEquals(9, LoggerFactory.levelValue("nope", 9));
    assertEquals(9, LoggerFactory.levelValue(null, 9));
    assertEquals(9, LoggerFactory.levelValue("   ", 9));
  }

  @Test
  void defaultRootIsInfoWhenUnset() {
    System.clearProperty("iridium.log.level");
    final LoggerFactory factory = new LoggerFactory();
    final Logger logger = factory.getLogger("cc.asylum.unset");

    assertTrue(logger.isInfoEnabled());
    assertFalse(logger.isDebugEnabled());
  }

  @Test
  void rootPropertyOverridesDefault() {
    System.setProperty("iridium.log.level", "debug");
    final LoggerFactory factory = new LoggerFactory();
    final Logger logger = factory.getLogger("cc.asylum.rootprop");

    assertTrue(logger.isDebugEnabled());
    assertFalse(logger.isTraceEnabled());
  }

  @Test
  void invalidRootPropertyFallsBackToInfo() {
    System.setProperty("iridium.log.level", "banana");
    final LoggerFactory factory = new LoggerFactory();
    final Logger logger = factory.getLogger("cc.asylum.badroot");

    assertTrue(logger.isInfoEnabled());
    assertFalse(logger.isDebugEnabled());
  }

  @Test
  void namedThresholdMatchesExactAndChildButNotSibling() {
    System.setProperty("iridium.log.level", "error");
    System.setProperty("iridium.log.level.cc.asylum", "debug");
    System.setProperty("iridium.log.level.cc.asylum.core", "trace");
    final LoggerFactory factory = new LoggerFactory();

    final Logger child = factory.getLogger("cc.asylum.core.service");
    final Logger exact = factory.getLogger("cc.asylum");
    final Logger sibling = factory.getLogger("cc.asylumother");
    final Logger other = factory.getLogger("other.pkg");

    assertTrue(child.isTraceEnabled());
    assertTrue(exact.isDebugEnabled());
    assertFalse(exact.isTraceEnabled());
    assertFalse(sibling.isWarnEnabled());
    assertTrue(sibling.isErrorEnabled());
    assertFalse(other.isWarnEnabled());
  }

  @Test
  void shorterPrefixDoesNotOverrideLongerMatch() {
    System.setProperty("iridium.log.level", "off");
    System.setProperty("iridium.log.level.a", "error");
    System.setProperty("iridium.log.level.a.b", "debug");
    System.setProperty("iridium.log.level.a.b.c", "trace");
    final LoggerFactory factory = new LoggerFactory();
    final Logger logger = factory.getLogger("a.b.c.d");

    assertTrue(logger.isTraceEnabled());
    assertSame(logger, factory.getLogger("a.b.c.d"));
    assertTrue(factory.getLogger("a.b").isDebugEnabled());
    assertFalse(factory.getLogger("a.b").isTraceEnabled());
    assertTrue(factory.getLogger("a").isErrorEnabled());
    assertFalse(factory.getLogger("a").isWarnEnabled());
  }

  @Test
  void emptyLoggerNameSuffixAndNonStringEntriesAreIgnored() {
    System.setProperty("iridium.log.level.", "trace");
    System.setProperty("iridium.log.level.kept", "warn");
    System.getProperties().put("iridium.log.level.numeric", Integer.valueOf(1));
    System.getProperties().put(Integer.valueOf(7), "trace");
    try {
      final LoggerFactory factory = new LoggerFactory();

      assertTrue(factory.getLogger("numeric").isInfoEnabled());
      assertFalse(factory.getLogger("numeric").isDebugEnabled());
      assertTrue(factory.getLogger("kept").isWarnEnabled());
      assertFalse(factory.getLogger("kept").isInfoEnabled());
      assertTrue(factory.getLogger("").isInfoEnabled());
      assertFalse(factory.getLogger("").isDebugEnabled());
    } finally {
      System.getProperties().remove("iridium.log.level.numeric");
      System.getProperties().remove(Integer.valueOf(7));
    }
  }

  @Test
  void invalidNamedLevelFallsBackToInfo() {
    System.setProperty("iridium.log.level", "off");
    System.setProperty("iridium.log.level.pkg", "not-a-level");
    final LoggerFactory factory = new LoggerFactory();
    final Logger logger = factory.getLogger("pkg.child");

    assertTrue(logger.isInfoEnabled());
    assertFalse(logger.isDebugEnabled());
  }

  @Test
  void shorterPrefixSeenAfterLongerMatchIsSkipped() throws Exception {
    System.setProperty("iridium.log.level", "off");
    final LoggerFactory factory = new LoggerFactory();
    final Map<String, Integer> ordered = new LinkedHashMap<>();
    ordered.put("a.b", Level.TRACE.toInt());
    ordered.put("a", Level.ERROR.toInt());
    ordered.put("a.b.c", Level.DEBUG.toInt());
    final Field field = LoggerFactory.class.getDeclaredField("thresholds");
    field.setAccessible(true);
    field.set(factory, ordered);

    final Logger logger = factory.getLogger("a.b.c.d");

    assertTrue(logger.isDebugEnabled());
    assertFalse(logger.isTraceEnabled());
  }
}
