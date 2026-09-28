package cc.asylum.iridium.config;

public record ConfigError(String message) {

  @Override
  public String toString() {
    return message;
  }

  public static ConfigError missing(final String key) {
    return new ConfigError("Missing required configuration key '" + key + "'");
  }

  public static ConfigError invalid(final String key, final String value) {
    return new ConfigError("Invalid value '" + value + "' for configuration key '" + key + "'");
  }

  public static ConfigError of(final String message) {
    return new ConfigError(message);
  }
}
