package cc.asylum.iridium.core.util;

import cc.asylum.iridium.core.annotation.Internal;

@Internal
public final class Names {

  private Names() {
  }

  public static String binary(final String relative) {
    if (relative == null) {
      return null;
    }
    final String stripped = relative.endsWith(".class")
        ? relative.substring(0, relative.length() - ".class".length())
        : relative;
    return stripped.replace('/', '.').replace('\\', '.');
  }
}
