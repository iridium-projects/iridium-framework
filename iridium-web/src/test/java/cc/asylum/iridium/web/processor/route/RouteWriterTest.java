package cc.asylum.iridium.web.processor.route;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RouteWriterTest {

  @Test
  void joinsPrefixAndPath() {
    assertEquals("/api/users", RouteWriter.resolvePath("/api", "/users"));
    assertEquals("/users", RouteWriter.resolvePath("", "/users"));
  }
}
