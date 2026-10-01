package cc.asylum.iridium.web.controller;

import cc.asylum.iridium.web.router.Request;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class ParametersTest {

  @Test
  void pathQueryAndHeaderFallbacks() {
    final Request request = new Request(
      "GET",
      "/users/4",
      Map.of("X-Trace", List.of("abc")),
      Map.of("q", List.of("found")),
      Map.of("id", "4"),
      "body".getBytes(StandardCharsets.UTF_8));

    assertEquals("4", Parameters.pathVariable(request, "id", "missing"));
    assertEquals("missing", Parameters.pathVariable(request, "nope", "missing"));
    assertEquals("found", Parameters.query(request, "q", "none"));
    assertEquals("none", Parameters.query(request, "absent", "none"));
    assertEquals("abc", Parameters.header(request, "x-trace", "none"));
    assertEquals("none", Parameters.header(request, "missing", "none"));
    assertEquals(List.of("abc"), Parameters.headers(request, "X-Trace"));
    assertEquals("body", Parameters.body(request));
    assertArrayEquals("body".getBytes(StandardCharsets.UTF_8), Parameters.bodyBytes(request));
  }

  @Test
  void queriesCopyNonListCollectionsAndEmptyBodies() {
    final Set<String> values = new LinkedHashSet<>();
    values.add("a");
    values.add("b");
    final Request request = new Request("GET", "/", Map.of(), Map.of("tag", values), Map.of(), new byte[0]);

    assertEquals(List.of("a", "b"), Parameters.queries(request, "tag"));
    assertEquals(List.of(), Parameters.queries(request, "missing"));
    assertNull(Parameters.bodyBytes(request));
    assertNull(Parameters.text(new byte[0]));
    assertNull(Parameters.text(null));

    final Request noQuery = new Request("GET", "/", (String[]) null, null, null, () -> null);
    assertEquals(List.of(), Parameters.queries(noQuery, "tag"));
    assertNull(Parameters.bodyBytes(noQuery));
  }

  @Test
  void cookiesParseAcrossHeaders() {
    final Request request = new Request(
      "GET",
      "/",
      Map.of("Cookie", List.of(" theme=dark ; session=abc", "other=1; id=42 ;")),
      Map.of(),
      Map.of(),
      null);

    assertEquals("dark", Parameters.cookie(request, "theme", "none"));
    assertEquals("abc", Parameters.cookie(request, "session", "none"));
    assertEquals("42", Parameters.cookie(request, "id", "none"));
    assertEquals("none", Parameters.cookie(request, "missing", "none"));
    assertEquals("none", Parameters.cookie(new Request("GET", "/", Map.of(), Map.of(), Map.of(), null), "a", "none"));
  }
}
