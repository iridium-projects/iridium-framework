package cc.asylum.iridium.web.processor.route;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MethodMappingTest {

  @Test
  void holdsMethodAndPath() {
    final MethodMapping mapping = new MethodMapping("GET", "/users");
    assertEquals("GET", mapping.httpMethod());
    assertEquals("/users", mapping.path());
  }
}
