package cc.asylum.iridium.core;

import cc.asylum.iridium.core.bean.BeanPool;

public abstract class Application {

  public Application() {
    BeanPool.initialize();
  }
}
