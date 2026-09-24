package cc.asylum.iridium.json;

import cc.asylum.iridium.json.avaje.AvajeJson;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTest {

  @Test
  void loadsTheAvajeProvider() {
    final Json json = Json.load();
    assertInstanceOf(AvajeJson.class, json);
    assertSame(json.getClass(), Json.load().getClass());
  }

  @Test
  void roundTripsMapsAndUtf8Bytes() {
    final Json json = new AvajeJson();
    final Map<String, Object> value = new LinkedHashMap<>();
    value.put("name", "är");
    value.put("count", 2);

    final String text = json.serialize(value);
    assertTrue(text.contains("är"));
    assertEquals("är", new String(json.serializeBytes(value), StandardCharsets.UTF_8).contains("är") ? "är" : "");

    @SuppressWarnings("unchecked")
    final Map<String, Object> decoded = json.deserialize(text, Map.class);
    assertEquals("är", decoded.get("name"));
    assertEquals(2, ((Number) decoded.get("count")).intValue());
  }

  @Test
  void serializesNullAndRejectsInvalidJson() {
    final Json json = new AvajeJson();
    assertEquals("", json.serialize(null));
    assertThrows(RuntimeException.class, () -> json.deserialize("{", Map.class));
    assertThrows(RuntimeException.class, () -> json.deserialize("[]", Map.class));
    assertThrows(RuntimeException.class, () -> json.deserialize(null, Map.class));
  }
}
