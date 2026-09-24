package cc.asylum.iridium.web.undertow;

import cc.asylum.iridium.web.response.Response;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ResponseWriterTest {

  @Test
  void writesNullBytesAndStringsWithoutJson() {
    final ResponseWriter writer = new ResponseWriter();
    final byte[] bytes = new byte[] {1, 2, 3};

    assertArrayEquals(new byte[0], writer.writeBody(Response.ok().build()));
    assertSame(bytes, writer.writeBody(Response.ok(bytes)));
    assertArrayEquals("ä".getBytes(StandardCharsets.UTF_8), writer.writeBody(Response.ok("ä")));
  }

  @Test
  void serializesObjectsAndReadsContentTypeIgnoringCase() {
    final ResponseWriter writer = new ResponseWriter();
    final byte[] body = writer.writeBody(Response.ok(Map.of("ok", true)));
    assertEquals("{\"ok\":true}", new String(body, StandardCharsets.UTF_8));

    assertEquals("application/json", writer.contentType(Response.ok("x")));
    assertEquals("text/plain", writer.contentType(Response.ok().header("content-type", "text/plain").build()));
    assertEquals("text/html", writer.contentType(Response.ok().header("Content-Type", "text/html").build()));
  }
}
