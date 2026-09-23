package cc.asylum.iridium.web;

import cc.asylum.iridium.core.bean.BeanPool;
import cc.asylum.iridium.web.webserver.WebServer;

public final class Iridium {

  private Iridium() {
  }

  public static void run(
      final Class<?> application,
      final String[] args) {
    final WebApplication config = application.getAnnotation(WebApplication.class);

    if (config == null) {
      throw new IllegalStateException("Missing @WebApplication on " + application.getName());
    }

    BeanPool.initialize();
    final WebServer server = WebServer.load();
    server.start(config.port(), config.host());
    server.registerRoutes();
    System.out.println("Iridium application listening on http://localhost:" + config.port());
  }
}
