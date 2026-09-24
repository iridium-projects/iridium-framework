package cc.asylum.iridium.config;

import cc.asylum.iridium.core.config.ConfigInitializer;

public final class IridiumConfigInitializer implements ConfigInitializer {

  @Override
  public void prepare(final String[] args) {
    Config.prepare(args);
  }
}
