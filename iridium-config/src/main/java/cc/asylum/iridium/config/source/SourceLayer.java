package cc.asylum.iridium.config.source;

import cc.asylum.iridium.config.ConfigError;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class SourceLayer {

  private final Map<String, String> exact = new LinkedHashMap<>();
  private final Map<String, String> relaxed = new LinkedHashMap<>();

  static SourceLayer of(final Map<String, String> values) {
    final SourceLayer layer = new SourceLayer();
    if (values != null) {
      values.forEach(layer::put);
    }
    return layer;
  }

  void put(final String key, final String value) {
    if (key == null || value == null) {
      return;
    }
    exact.put(key, value);
    relaxed.put(Keys.canonical(key), value);
  }

  String get(final String key) {
    if (exact.containsKey(key)) {
      return exact.get(key);
    }
    return relaxed.get(Keys.canonical(key));
  }

  boolean contains(final String key) {
    return exact.containsKey(key) || relaxed.containsKey(Keys.canonical(key));
  }

  Set<String> keys() {
    return exact.keySet();
  }
}
