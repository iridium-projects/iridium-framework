package cc.asylum.iridium.web.http;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ErrorDecoderTest {

  @Test
  void defaultIncludesStatusReasonAndBody() {
    final Exception decoded = ErrorDecoder.DEFAULT.decode(503, "Unavailable", "down".getBytes(StandardCharsets.UTF_8));
    final IllegalStateException error = assertInstanceOf(IllegalStateException.class, decoded);
    assertEquals("503 Unavailable: down", error.getMessage());
  }

  @Test
  void defaultOmitsEmptyOrMissingBody() {
    assertEquals("404 Missing", ErrorDecoder.DEFAULT.decode(404, "Missing", null).getMessage());
    assertEquals("400 Bad", ErrorDecoder.DEFAULT.decode(400, "Bad", new byte[0]).getMessage());
    assertTrue(ErrorDecoder.DEFAULT.decode(500, "Oops", "x".getBytes(StandardCharsets.UTF_8)) instanceof IllegalStateException);
  }
}
