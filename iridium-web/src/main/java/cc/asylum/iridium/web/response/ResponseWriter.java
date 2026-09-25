package cc.asylum.iridium.web.response;

import cc.asylum.iridium.core.util.Strings;
import io.avaje.jsonb.Jsonb;

import java.io.OutputStream;
import java.util.Map.Entry;

public final class ResponseWriter {

  private final Jsonb jsonb = Jsonb.instance();

  public byte[] writeBody(final Response<?> response) {
    final Object body = response.body();

    return switch (body) {
      case null -> new byte[0];
      case final byte[] bytes -> bytes;
      case final String string -> Strings.utf8(string);
      default -> jsonb.toJsonBytes(body);
    };
  }

  public void writeBody(final Response<?> response, final OutputStream out) throws Exception {
    final Object body = response.body();
    switch (body) {
      case null -> {
      }

      case final byte[] bytes -> out.write(bytes);
      case final String string -> out.write(Strings.utf8(string));
      default -> jsonb.toJson(body, out);
    }
  }

  public String contentType(final Response<?> response) {
    for (final Entry<String, String> header : response.headers().entrySet()) {
      if (header.getKey().equalsIgnoreCase("Content-Type")) {
        return header.getValue();
      }
    }

    return switch (response.body()) {
      case null -> "text/plain";
      case final byte[] _ -> "application/octet-stream";
      case final String _ -> "text/plain; charset=utf-8";
      default -> "application/json";
    };
  }
}
