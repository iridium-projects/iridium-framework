package cc.asylum.iridium.web.response;

import cc.asylum.iridium.json.Json;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class ResponseWriter {

  private final Json json = Json.load();

  public byte[] writeBody(final Response<?> response) {
    final Object body = response.body();

    return switch (body) {
      case null -> new byte[0];
      case final byte[] bytes -> bytes;
      case final String string -> string.getBytes(StandardCharsets.UTF_8);
      default -> json.serializeBytes(body);
    };
  }

  public String contentType(final Response<?> response) {
    for (final Map.Entry<String, String> header : response.headers().entrySet()) {
      if (header.getKey().equalsIgnoreCase("Content-Type")) {
        return header.getValue();
      }
    }
    return "application/json";
  }
}
