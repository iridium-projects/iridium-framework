package cc.asylum.iridium.web.response;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class ResponseWriterTest {

  private final ResponseWriter writer = new ResponseWriter();

  @Test
  void writesNullBytesStringAndJson() throws Exception {
    assertArrayEquals(new byte[0], writer.writeBody(Response.ok().build()));
    assertEquals("text/plain", writer.contentType(Response.ok().build()));

    final byte[] raw = {1, 2, 3};
    assertArrayEquals(raw, writer.writeBody(Response.ok(raw)));
    assertEquals("application/octet-stream", writer.contentType(Response.ok(raw)));

    assertArrayEquals("hi".getBytes(StandardCharsets.UTF_8), writer.writeBody(Response.ok("hi")));
    assertEquals("text/plain; charset=utf-8", writer.contentType(Response.ok("hi")));

    final byte[] json = writer.writeBody(Response.ok(Map.of("n", 1)));
    assertEquals("{\"n\":1}", new String(json, StandardCharsets.UTF_8));
    assertEquals("application/json", writer.contentType(Response.ok(Map.of("n", 1))));
  }

  @Test
  void declaredContentTypeWinsRegardlessOfCase() {
    final Response<?> response = Response.ok()
      .header("X-Other", "nope")
      .header("content-type", "application/xml")
      .body("hi");
    assertEquals("application/xml", writer.contentType(response));
  }

  @Test
  void streamsEachBodyKind() throws Exception {
    final ByteArrayOutputStream empty = new ByteArrayOutputStream();
    writer.writeBody(Response.status(204).build(), empty);
    assertEquals(0, empty.size());

    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    writer.writeBody(Response.ok(new byte[] {9}), bytes);
    assertArrayEquals(new byte[] {9}, bytes.toByteArray());

    final ByteArrayOutputStream text = new ByteArrayOutputStream();
    writer.writeBody(Response.ok("yo"), text);
    assertEquals("yo", text.toString(StandardCharsets.UTF_8));

    final ByteArrayOutputStream json = new ByteArrayOutputStream();
    writer.writeBody(Response.ok(Map.of("ok", true)), json);
    assertEquals("{\"ok\":true}", json.toString(StandardCharsets.UTF_8));
  }
}
