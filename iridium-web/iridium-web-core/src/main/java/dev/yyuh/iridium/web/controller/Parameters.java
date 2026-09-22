package dev.yyuh.iridium.web.controller;

import de.yyuh.iridium.core.annotation.Internal;
import dev.yyuh.iridium.web.router.Request;

import java.nio.charset.StandardCharsets;

@Internal
public final class Parameters {

  private Parameters() {
  }

  public static String pathVariable(final Request request, final String name, final String defaultValue) {
    return request.pathVariable(name).orElse(defaultValue);
  }

  public static String query(final Request request, final String name, final String defaultValue) {
    return request.query(name).orElse(defaultValue);
  }

  public static String header(final Request request, final String name, final String defaultValue) {
    return request.header(name).orElse(defaultValue);
  }

  public static String cookie(final Request request, final String name, final String defaultValue) {
    for (final String cookieHeader : request.headers("Cookie")) {
      for (final String pair : cookieHeader.split(";")) {
        final String trimmed = pair.trim();
        final int index = trimmed.indexOf('=');
        if (index == -1) {
          continue;
        }
        if (trimmed.substring(0, index).equals(name)) {
          return trimmed.substring(index + 1);
        }
      }
    }
    return defaultValue;
  }

  public static String body(final Request request) {
    final byte[] bytes = request.body();
    return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
  }
}
