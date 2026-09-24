package cc.asylum.iridium.web.controller;

import cc.asylum.iridium.web.router.Request;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParametersTest {

  @Test
  void readsPathQueryAndHeadersWithDefaults() {
    final Request request = new Request(
        "GET",
        "/users/7",
        Map.of("Accept", List.of("text/plain", "application/json"), "X-Empty", List.of()),
        Map.of("q", List.of("first", "second")),
        Map.of("id", "7"),
        null);

    assertEquals("7", Parameters.pathVariable(request, "id", "missing"));
    assertEquals("missing", Parameters.pathVariable(request, "other", "missing"));
    assertEquals("first", Parameters.query(request, "q", "fallback"));
    assertEquals("fallback", Parameters.query(request, "absent", "fallback"));
    assertEquals(List.of("first", "second"), Parameters.queries(request, "q"));
    assertTrue(Parameters.queries(request, "absent").isEmpty());
    assertEquals("text/plain", Parameters.header(request, "accept", "none"));
    assertEquals(List.of("text/plain", "application/json"), Parameters.headers(request, "ACCEPT"));
    assertTrue(Parameters.headers(request, "missing").isEmpty());
  }

  @Test
  void treatsNullQueryMapsAndPathVariablesAsAbsent() {
    final Request request = new Request("GET", "/", Map.of(), null, null, null);
    assertThrows(NullPointerException.class, () -> Parameters.query(request, "q", "fallback"));
    assertTrue(Parameters.queries(request, "q").isEmpty());
    assertEquals("fallback", Parameters.pathVariable(request, "id", "fallback"));
  }

  @Test
  void parsesCookiesAndSkipsMalformedPairs() {
    final Request request = new Request(
        "GET",
        "/",
        Map.of("Cookie", List.of("session", " a=1; b=two=2 ", "c=3")),
        Map.of(),
        Map.of(),
        null);

    assertEquals("1", Parameters.cookie(request, "a", "missing"));
    assertEquals("two=2", Parameters.cookie(request, "b", "missing"));
    assertEquals("3", Parameters.cookie(request, "c", "missing"));
    assertEquals("missing", Parameters.cookie(request, "session", "missing"));
    assertEquals("missing", Parameters.cookie(request, "absent", "missing"));
  }

  @Test
  void firstCookieWins() {
    final Request request = new Request(
        "GET",
        "/",
        Map.of("Cookie", List.of("token=first", "token=second")),
        Map.of(),
        Map.of(),
        null);
    assertEquals("first", Parameters.cookie(request, "token", "missing"));
  }

  @Test
  void decodesBodyAsUtf8() {
    assertNull(Parameters.body(new Request("POST", "/", Map.of(), Map.of(), Map.of(), null)));
    assertEquals("", Parameters.body(new Request("POST", "/", Map.of(), Map.of(), Map.of(), new byte[0])));
    assertEquals("ä", Parameters.body(new Request(
        "POST", "/", Map.of(), Map.of(), Map.of(), "ä".getBytes(StandardCharsets.UTF_8))));
  }
}
