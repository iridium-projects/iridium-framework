package cc.asylum.iridium.codegen.code;

import java.util.HashMap;
import java.util.Map;

public final class Names {

  private final Map<String, Integer> counts = new HashMap<>();

  public String next(final String prefix) {
    final int index = counts.getOrDefault(prefix, 0);
    counts.put(prefix, index + 1);
    return prefix + index;
  }
}
