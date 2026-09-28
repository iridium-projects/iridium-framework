package cc.asylum.iridium.web.http;

import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.util.Strings;
import cc.asylum.iridium.web.webserver.WebServer;
import io.avaje.jsonb.Jsonb;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Internal
public final class HttpClients {

  private static final Jsonb JSONB = Jsonb.instance();

  private HttpClients() {
  }

  public static byte[] exchange(
      final String baseUrl,
      final String method,
      final String path,
      final String contentType,
      final byte[] body) {
    return WebServer.load().exchange(baseUrl, method, path, contentType, body);
  }

  public static byte[] bytes(final Object body) {
    return switch (body) {
      case null -> null;
      case final byte[] raw -> raw;
      default -> JSONB.toJsonBytes(body);
    };
  }

  public static <T> T json(final byte[] body, final Class<T> type) {
    if (body == null || body.length == 0) {
      return null;
    }

    return JSONB.type(type).fromJson(body);
  }

  public static String text(final byte[] body) {
    return Strings.utf8(body);
  }

  public static String query(final Object value) {
    if (value == null) {
      return "";
    }

    return URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8);
  }

  public static String contentTypeOf(final String declared, final Object body) {
    if (!Strings.blank(declared)) {
      return declared;
    }

    return switch (body) {
      case null -> null;
      case final byte[] _ -> "application/octet-stream";
      case final String _ -> "text/plain; charset=utf-8";
      default -> "application/json";
    };
  }
}
