package cc.asylum.iridium.config;

public record ConfigError(String message) {

  @Override
  public String toString() {
    return message;
  }

  static ConfigError missing(final String key) {
    return new ConfigError("Missing required configuration key '" + key + "'");
  }

  static ConfigError invalid(final String key, final String value) {
    return new ConfigError("Invalid value '" + value + "' for configuration key '" + key + "'");
  }

  static ConfigError of(final String message) {
    return new ConfigError(message);
  }
}
