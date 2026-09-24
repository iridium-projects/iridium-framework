package cc.asylum.iridium.core.config;

import cc.asylum.iridium.core.annotation.Internal;

@Internal
public interface ConfigInitializer {

  void prepare(final String[] args);
}
