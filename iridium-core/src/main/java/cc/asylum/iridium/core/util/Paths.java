package cc.asylum.iridium.core.util;

import cc.asylum.iridium.core.annotation.Internal;

@Internal
public final class Paths {

  private Paths() {
  }

  public static String join(final String prefix, final String path) {
    final String head = Strings.trim(prefix, '/');
    final String tail = Strings.trim(path, '/');
    if (head.isEmpty()) {
      return tail.isEmpty() ? "/" : "/" + tail;
    }
    return tail.isEmpty() ? "/" + head : "/" + head + "/" + tail;
  }

  public static String[] segments(final String path) {
    if (path == null || path.isEmpty() || "/".equals(path)) {
      return new String[] {""};
    }
    return normalize(path).split("/", -1);
  }

  public static String variable(final String segment) {
    if (segment == null || segment.length() < 2
        || segment.charAt(0) != '{'
        || segment.charAt(segment.length() - 1) != '}') {
      return null;
    }
    return segment.substring(1, segment.length() - 1);
  }

  public static String normalize(final String path) {
    if (path == null || path.isEmpty()) {
      return "/";
    }
    String normalized = path.charAt(0) == '/' ? path : "/" + path;
    if (normalized.length() > 1 && normalized.charAt(normalized.length() - 1) == '/') {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }
}
