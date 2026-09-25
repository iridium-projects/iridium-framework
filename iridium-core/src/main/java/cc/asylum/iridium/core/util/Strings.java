package cc.asylum.iridium.core.util;

import cc.asylum.iridium.core.annotation.Internal;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Internal
public final class Strings {

  private Strings() {
  }

  public static String firstNonBlank(final String... values) {
    if (values == null) {
      return null;
    }
    for (final String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  public static String decapitalize(final String name) {
    if (name == null || name.isEmpty()) {
      return name;
    }
    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
  }

  public static String capitalize(final String name) {
    if (name == null || name.isEmpty()) {
      return name;
    }
    return Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }

  public static String quote(final String value) {
    if (value == null) {
      return "null";
    }
    return "\"" + value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        + "\"";
  }

  public static boolean blank(final String value) {
    return value == null || value.isBlank();
  }

  public static List<String> split(final String value, final char separator) {
    if (value == null || value.isEmpty()) {
      return List.of();
    }
    final List<String> parts = new ArrayList<>();
    for (final String part : value.split(String.valueOf(separator), -1)) {
      final String trimmed = part.trim();
      if (!trimmed.isEmpty()) {
        parts.add(trimmed);
      }
    }
    return parts;
  }

  public static String simpleName(final String qualified) {
    if (qualified == null || qualified.isEmpty()) {
      return qualified;
    }

    final int dot = qualified.lastIndexOf('.');

    return dot < 0 ? qualified : qualified.substring(dot + 1);
  }

  public static Boolean truthy(final String raw) {
    if (raw == null) {
      return null;
    }

    return switch (raw.trim().toLowerCase(Locale.ROOT)) {
      case "true", "yes", "on", "1" -> true;
      case "false", "no", "off", "0" -> false;
      default -> null;
    };
  }

  public static String utf8(final byte[] bytes) {
    return bytes == null || bytes.length == 0 ? null : new String(bytes, StandardCharsets.UTF_8);
  }

  public static byte[] utf8(final String value) {
    return value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
  }

  public static String trim(final String value, final char edge) {
    if (value == null) {
      return "";
    }

    int start = 0;
    int end = value.length();

    while (start < end && value.charAt(start) == edge) {
      start++;
    }

    while (end > start && value.charAt(end - 1) == edge) {
      end--;
    }

    return value.substring(start, end);
  }
}
