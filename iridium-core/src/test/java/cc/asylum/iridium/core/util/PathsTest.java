package cc.asylum.iridium.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class PathsTest {

  @Test
  void joinTrimsSlashes() {
    assertEquals("/", Paths.join(null, null));
    assertEquals("/", Paths.join("", "/"));
    assertEquals("/tail", Paths.join("/", "tail"));
    assertEquals("/head", Paths.join("head/", ""));
    assertEquals("/head/tail", Paths.join("/head/", "/tail/"));
  }

  @Test
  void segmentsAndNormalize() {
    assertArrayEquals(new String[] {""}, Paths.segments(null));
    assertArrayEquals(new String[] {""}, Paths.segments(""));
    assertArrayEquals(new String[] {""}, Paths.segments("/"));
    assertArrayEquals(new String[] {"", "a", "b"}, Paths.segments("/a/b/"));
    assertArrayEquals(new String[] {"", "a"}, Paths.segments("a"));
    assertEquals("/", Paths.normalize(null));
    assertEquals("/", Paths.normalize(""));
    assertEquals("/a", Paths.normalize("a"));
    assertEquals("/a", Paths.normalize("/a/"));
    assertEquals("/", Paths.normalize("/"));
  }

  @Test
  void variableExtractsBracedNames() {
    assertNull(Paths.variable(null));
    assertNull(Paths.variable("{"));
    assertNull(Paths.variable("id"));
    assertNull(Paths.variable("{id"));
    assertNull(Paths.variable("id}"));
    assertEquals("id", Paths.variable("{id}"));
    assertEquals("", Paths.variable("{}"));
  }
}
