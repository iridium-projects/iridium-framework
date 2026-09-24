package cc.asylum.iridium.json;

import io.avaje.jsonb.Jsonb;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTest {

  @Test
  void roundTripsMapsAndUtf8Bytes() {
    final Jsonb jsonb = Jsonb.instance();
    final Map<String, Object> value = new LinkedHashMap<>();
    value.put("name", "är");
    value.put("count", 2);

    final String text = jsonb.toJson(value);
    assertTrue(text.contains("är"));
    assertTrue(new String(jsonb.toJsonBytes(value), StandardCharsets.UTF_8).contains("är"));

    @SuppressWarnings("unchecked")
    final Map<String, Object> decoded = jsonb.type(Map.class).fromJson(text);
    assertEquals("är", decoded.get("name"));
    assertEquals(2, ((Number) decoded.get("count")).intValue());
  }

  @Test
  void serializesNullAndRejectsInvalidJson() {
    final Jsonb jsonb = Jsonb.instance();
    assertEquals("", jsonb.toJson(null));
    assertThrows(RuntimeException.class, () -> jsonb.type(Map.class).fromJson("{"));
    assertThrows(RuntimeException.class, () -> jsonb.type(Map.class).fromJson("[]"));
    assertThrows(RuntimeException.class, () -> jsonb.type(Map.class).fromJson((String) null));
  }
}
