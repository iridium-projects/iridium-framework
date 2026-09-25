package cc.asylum.iridium.web;

import cc.asylum.iridium.core.bean.BeanPool;
import cc.asylum.iridium.core.config.ConfigInitializer;
import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.result.Unit;
import cc.asylum.iridium.web.webserver.WebServer;
import lombok.extern.slf4j.Slf4j;

import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;

@Slf4j
public final class Iridium {

  private final WebApplication config;

  private Iridium(final Class<?> application) {
    final WebApplication config = application.getAnnotation(WebApplication.class);

    if (config == null) {
      throw new IllegalStateException("Missing @WebApplication on " + application.getName());
    }

    this.config = config;
  }

  public static void run(final Class<?> application, final String[] args) {
    Banner.print();
    ServiceLoader.load(ConfigInitializer.class).findFirst().ifPresent(initializer -> initializer.prepare(args));
    new Iridium(application).start();
  }

  private void start() {
    final var poolInitialized = BeanPool.initialize();

    if (poolInitialized.isErr()) {
      final var exception = poolInitialized.unwrapErr();
      final var message = exception.getMessage();

      log.error("Failed to initialize BeanPool: {}", message);
      throw new IllegalStateException(message, exception);
    }

    final var server = WebServer.load();
    final Result<Unit, Exception> started = server.start(config.port(), config.host());

    if (started.isErr()) {
      final var exception = started.unwrapErr();
      final var message = exception.getMessage();

      log.error("Failed to start web server on {}:{}: {}", config.host(), config.port(), message);
      throw new IllegalStateException(message, exception);
    }

    server.registerRoutes();
    log.info("Iridium application listening on http://localhost:{}", config.port());

    Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "iridium-shutdown"));
    try {
      new CountDownLatch(1).await();
    } catch (final InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
  }
}
