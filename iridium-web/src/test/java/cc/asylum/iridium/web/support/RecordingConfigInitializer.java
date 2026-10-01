package cc.asylum.iridium.web.support;

import cc.asylum.iridium.core.config.ConfigInitializer;

public final class RecordingConfigInitializer implements ConfigInitializer {

  public static volatile String[] seen;

  @Override
  public void prepare(final String[] args) {
    seen = args;
  }
}
