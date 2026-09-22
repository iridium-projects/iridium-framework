package dev.yyuh.iridium.json;

public interface Json {

  String serialize(final Object value);

  byte[] serializeBytes(final Object value);

  <T> T deserialize(final String json, final Class<T> type);
}
