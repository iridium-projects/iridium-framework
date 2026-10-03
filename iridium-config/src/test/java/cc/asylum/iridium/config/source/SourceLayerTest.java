package cc.asylum.iridium.config.source;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceLayerTest {

  @Test
  void ofSkipsNullsAndMatchesRelaxedKeys() {
    final Map<String, String> values = new LinkedHashMap<>();
    values.put("server.port", "8080");
    values.put("server-host", "localhost");
    values.put(null, "ignored");
    values.put("blank", null);

    final SourceLayer layer = SourceLayer.of(values);
    assertEquals("8080", layer.get("server.port"));
    assertEquals("8080", layer.get("Server.Port"));
    assertNull(layer.get("server_port"));
    assertNull(layer.get("missing"));
    assertTrue(layer.contains("Server.Port"));
    assertFalse(layer.contains("ServerPort"));
    assertFalse(layer.contains("blank"));
    assertFalse(layer.contains("missing"));
    assertEquals(Set.of("server.port", "server-host"), layer.keys());
  }

  @Test
  void ofTreatsNullMapAsEmpty() {
    final SourceLayer layer = SourceLayer.of(null);
    assertNull(layer.get("anything"));
    assertFalse(layer.contains("anything"));
    assertTrue(layer.keys().isEmpty());
  }

  @Test
  void putIgnoresNullKeyOrValue() {
    final SourceLayer layer = new SourceLayer();
    layer.put(null, "value");
    layer.put("key", null);
    layer.put("key", "value");
    assertEquals("value", layer.get("key"));
    assertTrue(layer.contains("KEY"));
  }
}
