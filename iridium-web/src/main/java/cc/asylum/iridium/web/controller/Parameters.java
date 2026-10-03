package cc.asylum.iridium.web.controller;

import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.util.Strings;
import cc.asylum.iridium.web.router.Request;

import java.util.List;

@Internal
public final class Parameters {

  private Parameters() {
  }

  public static String pathVariable(
    final Request request,
    final String name,
    final String defaultValue) {
    final String value = request.pathVariable(name);

    return value == null ? defaultValue : value;
  }

  public static String query(
    final Request request,
    final String name,
    final String defaultValue) {
    final String value = request.query(name);

    return value == null ? defaultValue : value;
  }

  public static List<String> queries(final Request request, final String name) {
    if (request.queryParameters() == null) {
      return List.of();
    }

    final var values = request.queryParameters().get(name);
    if (values == null) {
      return List.of();
    }

    return values instanceof final List<String> list ? list : List.copyOf(values);
  }

  public static String header(
    final Request request,
    final String name,
    final String defaultValue) {
    final String value = request.header(name);

    return value == null ? defaultValue : value;
  }

  public static List<String> headers(final Request request, final String name) {
    return request.headers(name);
  }

  public static String cookie(
    final Request request,
    final String name,
    final String defaultValue) {
    for (final String cookieHeader : request.headers("Cookie")) {
      int start = 0;
      final int length = cookieHeader.length();

      while (start < length) {
        int end = cookieHeader.indexOf(';', start);
        if (end < 0) {
          end = length;
        }

        int left = start;
        int right = end;
        while (left < right && cookieHeader.charAt(left) <= ' ') {
          left++;
        }

        while (right > left && cookieHeader.charAt(right - 1) <= ' ') {
          right--;
        }

        final int index = cookieHeader.indexOf('=', left);
        if (index > left && index < right && index - left == name.length()
          && cookieHeader.regionMatches(left, name, 0, name.length())) {
          return cookieHeader.substring(index + 1, right);
        }

        start = end + 1;
      }
    }

    return defaultValue;
  }

  public static String body(final Request request) {
    return text(request.body());
  }

  public static String text(final byte[] bytes) {
    return Strings.utf8(bytes);
  }

  public static byte[] bodyBytes(final Request request) {
    final byte[] bytes = request.body();

    return bytes == null || bytes.length == 0 ? null : bytes;
  }

}
