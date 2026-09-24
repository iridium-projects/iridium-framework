package cc.asylum.iridium.json.avaje;

import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.json.Json;
import io.avaje.jsonb.Jsonb;

@Internal
public final class AvajeJson implements Json {

  private final Jsonb jsonb;

  public AvajeJson() {
    this(Jsonb.instance());
  }

  public AvajeJson(final Jsonb jsonb) {
    this.jsonb = jsonb;
  }

  @Override
  public String serialize(final Object value) {
    return jsonb.toJson(value);
  }

  @Override
  public byte[] serializeBytes(final Object value) {
    return jsonb.toJsonBytes(value);
  }

  @Override
  public <T> T deserialize(final String json, final Class<T> type) {
    return jsonb.type(type).fromJson(json);
  }
}
