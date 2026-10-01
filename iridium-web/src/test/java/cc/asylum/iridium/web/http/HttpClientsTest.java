package cc.asylum.iridium.web.http;

import cc.asylum.iridium.web.webserver.WebServer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class HttpClientsTest {

  @Test
  void exchangeDelegatesToLoadedServer() {
    final WebServer server = mock(WebServer.class);
    final byte[] body = {1};
    final byte[] response = {2};
    when(server.exchange("http://localhost", "POST", "/v1", "application/json", body)).thenReturn(response);

    try (MockedStatic<WebServer> loaded = mockStatic(WebServer.class)) {
      loaded.when(WebServer::load).thenReturn(server);
      assertSame(response, HttpClients.exchange("http://localhost", "POST", "/v1", "application/json", body));
    }

    verify(server).exchange("http://localhost", "POST", "/v1", "application/json", body);
  }

  @Test
  void bytesJsonAndText() {
    assertNull(HttpClients.bytes(null));
    final byte[] raw = {9, 8};
    assertSame(raw, HttpClients.bytes(raw));
    assertArrayEquals("{\"n\":1}".getBytes(StandardCharsets.UTF_8), HttpClients.bytes(Map.of("n", 1)));

    assertNull(HttpClients.json(null, String.class));
    assertNull(HttpClients.json(new byte[0], String.class));
    assertEquals("hi", HttpClients.json("\"hi\"".getBytes(StandardCharsets.UTF_8), String.class));
    assertEquals("hi", HttpClients.text("hi".getBytes(StandardCharsets.UTF_8)));
    assertNull(HttpClients.text(null));
  }

  @Test
  void queryEncodesAndContentTypeFallsBack() {
    assertEquals("", HttpClients.query(null));
    assertEquals("a+b", HttpClients.query("a b"));
    assertEquals("7", HttpClients.query(7));

    assertEquals("text/custom", HttpClients.contentTypeOf("text/custom", "ignored"));
    assertNull(HttpClients.contentTypeOf("  ", null));
    assertNull(HttpClients.contentTypeOf(null, null));
    assertEquals("application/octet-stream", HttpClients.contentTypeOf("", new byte[] {1}));
    assertEquals("text/plain; charset=utf-8", HttpClients.contentTypeOf(null, "text"));
    assertEquals("application/json", HttpClients.contentTypeOf("", Map.of("a", 1)));
  }
}
