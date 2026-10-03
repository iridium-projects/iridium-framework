package cc.asylum.iridium.web.router;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

final class RequestTest {

  @Test
  void accessorsAndNullInput() {
    final byte[] body = {7};
    final Map<String, List<String>> headers = Map.of("Accept", List.of("text/plain"));
    final Map<String, List<String>> query = Map.of("q", List.of("one"));
    final Map<String, String> variables = Map.of("id", "4");
    final Request request = new Request("GET", "/items/4", headers, query, variables, body);

    assertEquals("GET", request.method());
    assertEquals("/items/4", request.path());
    assertNull(request.segments());
    assertNull(request.headerMap());
    assertEquals(headers, request.headers());
    assertEquals(query, request.queryParameters());
    assertEquals(variables, request.pathVariables());
    assertSame(body, request.body());
    assertNull(request.input());
    assertEquals("4", request.pathVariable("id"));
    assertNull(request.pathVariable("missing"));
  }

  @Test
  void headerLookupIsCaseInsensitiveAndFallsBack() throws Exception {
    final Map<String, List<String>> headerMap = new LinkedHashMap<>();
    headerMap.put("X-Trace", List.of("a", "b"));
    headerMap.put("Empty", null);
    final Request request = new Request("GET", "/", new String[] {""}, headerMap, Map.of(), () -> new ByteArrayInputStream(new byte[] {1}));

    assertEquals("a", request.header("x-trace"));
    assertEquals(List.of("a", "b"), request.headers("X-Trace"));
    assertEquals(List.of(), request.headers("empty"));
    assertEquals(List.of(), request.headers("missing"));
    assertEquals(List.of(), request.headers(null));
    assertEquals(1, request.input().read());
    assertNull(request.headers());
    assertNull(request.query("q"));
    assertNull(request.pathVariable("id"));
  }

  @Test
  void headersUseDirectMapWhenHeaderMapMissing() {
    final Request request = new Request("GET", "/", Map.of("Host", List.of("localhost")), Map.of(), Map.of(), null);
    assertEquals("localhost", request.header("host"));
    assertEquals(List.of(), request.headers("nope"));

    final Request empty = new Request("GET", "/", (java.util.Map<String, java.util.List<String>>) null, null, null, (byte[]) null);
    assertEquals(List.of(), empty.headers("Host"));
    assertNull(empty.query("q"));
    assertNull(empty.pathVariable("id"));
    assertNull(empty.header("Host"));
  }

  @Test
  void queryAndCopies() {
    final Map<String, List<String>> query = Map.of("q", List.of());
    final Request request = new Request("get", "/a", Map.of(), query, null, new byte[] {1});

    assertNull(request.query(null));
    assertNull(request.query("q"));
    assertNull(request.query("missing"));

    final Request rebound = request.withPathVariables(Map.of("id", "1")).withBody(new byte[] {9});
    assertEquals("1", rebound.pathVariable("id"));
    assertArrayEquals(new byte[] {9}, rebound.body());
    assertEquals("get", rebound.method());
    assertSame(query, rebound.queryParameters());
  }

  @Test
  void splitAndNormalizeMethod() {
    assertArrayEquals(new String[] {""}, Request.split("/"));
    assertEquals("", Request.normalizeMethod(null));
    assertEquals("POST", Request.normalizeMethod("post"));
    assertEquals("GET", Request.normalizeMethod("GET".toLowerCase(Locale.ROOT)));
  }
}
