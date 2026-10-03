package cc.asylum.iridium.web.response;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ResponseTest {

  @Test
  void factoriesExposeStatusHeadersAndBody() {
    final Response<String> ok = Response.ok("payload");
    assertEquals(200, ok.status());
    assertEquals("payload", ok.body());
    assertTrue(ok.headers().isEmpty());

    final Response<Integer> built = Response.status(201)
      .header("X-Id", "7")
      .body(7);
    assertEquals(201, built.status());
    assertEquals(7, built.body());
    assertEquals("7", built.headers().get("X-Id"));
    assertThrows(UnsupportedOperationException.class, () -> built.headers().put("A", "B"));

    assertEquals(404, Response.notFound().build().status());
    assertEquals(400, Response.badRequest().build().status());
    assertEquals(204, Response.noContent().build().status());
    assertNull(Response.noContent().build().body());

    final Response<?> created = Response.created("/users/1").body("made");
    assertEquals(201, created.status());
    assertEquals("/users/1", created.headers().get("Location"));
    assertEquals("made", created.body());
  }

  @Test
  void nullHeaderRejected() {
    final Response.BodyBuilder builder = Response.ok();
    assertThrows(IllegalArgumentException.class, () -> builder.header(null, "v"));
    assertThrows(IllegalArgumentException.class, () -> builder.header("N", null));
  }

  @Test
  void chainedHeadersOverwrite() {
    final Response<?> response = Response.status(202).header("A", "1").header("A", "2").build();
    assertEquals(Map.of("A", "2"), response.headers());
  }
}
