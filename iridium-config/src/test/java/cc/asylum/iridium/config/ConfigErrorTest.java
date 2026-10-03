package cc.asylum.iridium.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ConfigErrorTest {

  @Test
  void missingNamesTheKey() {
    final ConfigError error = ConfigError.missing("server.port");
    assertEquals("Missing required configuration key 'server.port'", error.message());
    assertEquals("Missing required configuration key 'server.port'", error.toString());
  }

  @Test
  void invalidNamesKeyAndValue() {
    final ConfigError error = ConfigError.invalid("server.port", "nope");
    assertEquals("Invalid value 'nope' for configuration key 'server.port'", error.message());
  }

  @Test
  void ofKeepsTheMessage() {
    assertEquals("boom", ConfigError.of("boom").toString());
  }
}
