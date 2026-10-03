package cc.asylum.iridium.web;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class BannerTest {

  @Test
  void printPaintsMarkAndSlogan() {
    final PrintStream original = System.out;
    final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
    try {
      Banner.print();
    } finally {
      System.setOut(original);
    }

    final String banner = captured.toString(StandardCharsets.UTF_8);
    assertTrue(banner.contains("Iridium"));
    assertTrue(banner.contains("Hardwired Java"));
    assertTrue(banner.contains("\u001B[0m"));
    assertTrue(banner.contains("\u001B[38;2;94;234;212m"));
    assertTrue(banner.contains("\u001B[38;2;209;213;219m"));
    assertTrue(banner.contains("\u001B[38;2;248;250;252m"));
    assertTrue(banner.contains("\u001B[1m"));
  }
}
