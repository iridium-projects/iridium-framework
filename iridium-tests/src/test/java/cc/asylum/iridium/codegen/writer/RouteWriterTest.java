package cc.asylum.iridium.codegen.writer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteWriterTest {

  @Test
  void joinsPrefixAndPath() {
    assertEquals("/api/hello", RouteWriter.resolvePath("/api", "/hello"));
    assertEquals("/api/hello", RouteWriter.resolvePath("/api/", "hello"));
    assertEquals("/api/hello", RouteWriter.resolvePath("api", "hello"));
  }

  @Test
  void keepsBarePaths() {
    assertEquals("/hello", RouteWriter.resolvePath("", "/hello"));
    assertEquals("/hello", RouteWriter.resolvePath(null, "/hello"));
  }

  @Test
  void keepsBarePrefix() {
    assertEquals("/api", RouteWriter.resolvePath("/api", ""));
    assertEquals("/api", RouteWriter.resolvePath("/api", null));
    assertEquals("/", RouteWriter.resolvePath("", ""));
  }

  @Test
  void trimsRepeatedSlashes() {
    assertEquals("/api/hello", RouteWriter.resolvePath("//api//", "//hello//"));
    assertEquals("/api", RouteWriter.resolvePath("///api///", "///"));
    assertEquals("/", RouteWriter.resolvePath(null, null));
    assertEquals("/", RouteWriter.resolvePath("///", "///"));
  }
}
