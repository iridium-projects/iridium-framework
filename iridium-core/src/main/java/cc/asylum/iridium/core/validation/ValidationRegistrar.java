package cc.asylum.iridium.core.validation;

import cc.asylum.iridium.core.annotation.Internal;

@Internal
public interface ValidationRegistrar {

  void register(final ValidatorRegistry registry);
}
