package cc.asylum.iridium.web.processor.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ClientPathExprTest {

  @Test
  void buildsPathExpressions() {
    assertNotNull(ClientPath.expression("", List.of(), List.of()));
    assertNotNull(ClientPath.expression("/plain", List.of(), List.of()));
    assertNotNull(ClientPath.expression("/users/{id}/posts/{pid}", List.of("id", "userId"), List.of()));
    assertNotNull(ClientPath.expression("{id}", List.of("missing", "x"), List.of()));
    assertNotNull(ClientPath.expression("/open{", List.of(), List.of()));
    assertNotNull(ClientPath.expression("/q", List.of(), List.of("page", "page", "size", "size")));
    assertNotNull(ClientPath.CLIENTS);
  }
}
