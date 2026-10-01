package cc.asylum.iridium.log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggerFormatTest {

  private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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
  void privateConstructorIsInvocable() throws Exception {
    final Constructor<LoggerFormat> constructor = LoggerFormat.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    assertNotNull(constructor.newInstance());
  }

  @Test
  void writePrintsTimestampLevelThreadNameAndMessage() {
    LoggerFormat.write(Level.INFO, "cc.asylum.app", "started", null);

    final String output = captured.toString();
    assertTrue(output.startsWith(TIMESTAMP.format(LocalDateTime.now()).substring(0, 10)));
    assertTrue(output.contains("INFO"));
    assertTrue(output.contains("[" + Thread.currentThread().getName() + "] "));
    assertTrue(output.contains("cc.asylum.app - started\n"));
    assertFalse(output.contains("Exception"));
  }

  @Test
  void eachLevelUsesDistinctToken() throws Exception {
    final Method levelToken = LoggerFormat.class.getDeclaredMethod("levelToken", Level.class);
    levelToken.setAccessible(true);

    assertEquals("\u001B[1;31mERROR\u001B[0m", levelToken.invoke(null, Level.ERROR));
    assertEquals("\u001B[1;33mWARN \u001B[0m", levelToken.invoke(null, Level.WARN));
    assertEquals("\u001B[1;32mINFO \u001B[0m", levelToken.invoke(null, Level.INFO));
    assertEquals("\u001B[1;36mDEBUG\u001B[0m", levelToken.invoke(null, Level.DEBUG));
    assertEquals("\u001B[37mTRACE\u001B[0m", levelToken.invoke(null, Level.TRACE));
  }

  @Test
  void writeIncludesThrowableStack() {
    LoggerFormat.write(Level.ERROR, "fail", "boom", new IllegalStateException("broken"));

    final String output = captured.toString();
    assertTrue(output.contains("fail - boom\n"));
    assertTrue(output.contains("java.lang.IllegalStateException: broken"));
    assertTrue(output.contains(LoggerFormatTest.class.getName()));
  }

  @Test
  void nullMessageIsAppendedAsNullLiteral() throws Exception {
    final Method format = LoggerFormat.class.getDeclaredMethod(
        "format", Level.class, String.class, String.class, Throwable.class);
    format.setAccessible(true);

    final String output = (String) format.invoke(null, Level.DEBUG, "n", null, null);

    assertTrue(output.contains("DEBUG"));
    assertTrue(output.contains("n - null\n"));
  }

  @Test
  void concurrentWritesDoNotInterleaveLines() throws Exception {
    final Thread first = new Thread(() -> LoggerFormat.write(Level.WARN, "a", "first-line", null), "fmt-a");
    final Thread second = new Thread(() -> LoggerFormat.write(Level.TRACE, "b", "second-line", null), "fmt-b");

    first.start();
    second.start();
    first.join();
    second.join();

    final String output = captured.toString();
    assertTrue(output.contains("first-line\n"));
    assertTrue(output.contains("second-line\n"));
    assertTrue(output.contains("[fmt-a] "));
    assertTrue(output.contains("[fmt-b] "));
  }
}
