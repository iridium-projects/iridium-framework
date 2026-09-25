package cc.asylum.iridium.web.webserver;

import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.web.router.Router;

@Internal
public interface WebRegistrar {

  void register(final Router router);
}
