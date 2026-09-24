package cc.asylum.iridium.web.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResponseTest {

  @Test
  void buildsStatusHelpers() {
    assertEquals(200, Response.ok().build().status());
    assertEquals("body", Response.ok("body").body());
    assertEquals(404, Response.notFound().build().status());
    assertEquals(400, Response.badRequest().build().status());
    assertEquals(204, Response.noContent().build().status());
    assertNull(Response.noContent().build().body());
    assertEquals("201", String.valueOf(Response.created("/items/1").build().status()));
    assertEquals("/items/1", Response.created("/items/1").build().headers().get("Location"));
  }

  @Test
  void rejectsNullHeadersAndCopiesThem() {
    assertThrows(IllegalArgumentException.class, () -> Response.ok().header(null, "v"));
    assertThrows(IllegalArgumentException.class, () -> Response.ok().header("n", null));
    assertThrows(IllegalArgumentException.class, () -> Response.created(null));

    final Response.BodyBuilder builder = Response.status(201).header("X-Test", "one");
    final Response<String> response = builder.header("X-Test", "two").body("ok");
    builder.header("X-Later", "nope");

    assertEquals("two", response.headers().get("X-Test"));
    assertNull(response.headers().get("X-Later"));
    assertThrows(UnsupportedOperationException.class, () -> response.headers().put("X-Mutate", "no"));
  }
}
