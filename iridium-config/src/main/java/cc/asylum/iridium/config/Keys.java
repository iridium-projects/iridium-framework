package cc.asylum.iridium.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class Keys {

  private Keys() {
  }

  static String join(
      final String prefix,
      final String name
  ) {
    if (prefix == null || prefix.isEmpty()) {
      return name;
    }
    if (name == null || name.isEmpty()) {
      return prefix;
    }
    return prefix + "." + name;
  }

  static String canonical(final String key) {
    final StringBuilder out = new StringBuilder(key.length());
    for (int i = 0; i < key.length(); i++) {
      final char c = key.charAt(i);
      if (c == '-' || c == '_') {
        continue;
      }
      if (c == '.' || c == '[' || c == ']') {
        out.append(c);
      } else {
        out.append(Character.toLowerCase(c));
      }
    }
    return out.toString();
  }

  static List<String> envCandidates(final String key) {
    final String separated = separate(key).toUpperCase(Locale.ROOT);
    final String compact = compact(key).toUpperCase(Locale.ROOT);
    if (separated.equals(compact)) {
      return List.of(separated);
    }
    return List.of(separated, compact);
  }

  static String separate(final String key) {
    final StringBuilder out = new StringBuilder();
    for (int i = 0; i < key.length(); i++) {
      final char c = key.charAt(i);
      if (c == '.' || c == '-' || c == '_') {
        appendUnderscore(out);
        continue;
      }
      if (Character.isUpperCase(c) && out.length() > 0 && out.charAt(out.length() - 1) != '_') {
        out.append('_');
      }
      out.append(c);
    }
    return out.toString();
  }

  private static String compact(final String key) {
    final StringBuilder out = new StringBuilder();
    for (int i = 0; i < key.length(); i++) {
      final char c = key.charAt(i);
      if (c == '-') {
        continue;
      }
      if (c == '.' || c == '_') {
        appendUnderscore(out);
        continue;
      }
      out.append(c);
    }
    return out.toString();
  }

  private static void appendUnderscore(final StringBuilder out) {
    if (out.length() == 0 || out.charAt(out.length() - 1) != '_') {
      out.append('_');
    }
  }
}
