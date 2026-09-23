package cc.asylum.iridium.core.bean;

import cc.asylum.iridium.core.annotation.Internal;

@Internal
public interface BeanRegistrar {

  void register(final BeanPool pool);
}
