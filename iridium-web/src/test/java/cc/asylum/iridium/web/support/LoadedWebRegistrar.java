package cc.asylum.iridium.web.support;

import cc.asylum.iridium.web.router.Router;
import cc.asylum.iridium.web.webserver.WebRegistrar;

public final class LoadedWebRegistrar implements WebRegistrar {

  @Override
  public void register(final Router router) {
    router.register("GET", "/loaded", request -> null);
  }
}
