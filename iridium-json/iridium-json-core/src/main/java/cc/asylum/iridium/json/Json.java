package cc.asylum.iridium.json;

import java.util.ServiceLoader;

public interface Json {

  String serialize(final Object value);

  byte[] serializeBytes(final Object value);

  <T> T deserialize(final String json, final Class<T> type);

  static Json load() {
    return ServiceLoader.load(Json.class).findFirst()
        .orElseThrow(() -> new IllegalStateException("No Json implementation on classpath"));
  }
}
