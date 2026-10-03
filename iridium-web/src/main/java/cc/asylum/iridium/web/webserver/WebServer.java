package cc.asylum.iridium.web.webserver;

import cc.asylum.iridium.core.bean.BeanPool;
import cc.asylum.iridium.core.hook.ShutdownHook;
import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.result.Unit;
import cc.asylum.iridium.core.validation.Validation;

import java.util.Comparator;
import java.util.ServiceLoader;

public interface WebServer {

  Result<Unit, Exception> start(final int port, final String host);

  default void stop() {
    BeanPool.instance().all(ShutdownHook.class).stream()
        .sorted(Comparator.comparingInt(ShutdownHook::priority))
        .forEach(ShutdownHook::run);
  }

  Result<Unit, Exception> registerRoutes();

  byte[] exchange(
      final String baseUrl,
      final String method,
      final String path,
      final String contentType,
      final byte[] body);

  static Result<Unit, Exception> reload() {
    final Result<Unit, Exception> beans = BeanPool.initialize();

    if (beans.isErr()) {
      return beans;
    }

    Validation.reset();
    Validation.initialize();

    return load().registerRoutes();
  }

  static WebServer load() {
    return Loaded.SERVER;
  }

  final class Loaded {
    private static final WebServer SERVER = ServiceLoader.load(WebServer.class).findFirst()
        .orElseThrow(() -> new IllegalStateException("No WebServer implementation on classpath"));

    private Loaded() {
    }
  }
}
