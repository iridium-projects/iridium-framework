package cc.asylum.iridium.web.webserver;

import cc.asylum.iridium.core.bean.BeanPool;
import cc.asylum.iridium.core.hook.ShutdownHook;
import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.result.Unit;
import cc.asylum.iridium.core.validation.Validation;
import cc.asylum.iridium.web.router.Router;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class WebServerTest {

  @AfterEach
  void clearBeans() {
    BeanPool.instance().clear();
    Validation.reset();
  }

  @Test
  void stopRunsHooksByPriority() {
    final AtomicInteger order = new AtomicInteger();
    final int[] seen = new int[2];
    BeanPool.instance().put("late", (ShutdownHook) () -> seen[order.getAndIncrement()] = 2);
    BeanPool.instance().put("early", new ShutdownHook() {
      @Override
      public void run() {
        seen[order.getAndIncrement()] = 1;
      }

      @Override
      public int priority() {
        return -1;
      }
    });

    final WebServer server = new StubServer();
    server.stop();
    assertEquals(1, seen[0]);
    assertEquals(2, seen[1]);
  }

  @Test
  void registrarIsInvoked() {
    final Router router = new Router();
    final WebRegistrar registrar = mock(WebRegistrar.class);
    registrar.register(router);
    verify(registrar).register(router);
  }

  @Test
  void reloadReturnsBeanFailureWithoutTouchingServer() {
    final RuntimeException failure = new IllegalStateException("beans");
    try (MockedStatic<BeanPool> pool = mockStatic(BeanPool.class)) {
      pool.when(BeanPool::initialize).thenReturn(Result.err(failure));
      final Result<Unit, Exception> reloaded = WebServer.reload();
      assertTrue(reloaded.isErr());
      assertSame(failure, reloaded.unwrapErr());
    }
  }

  @Test
  void reloadRegistersRoutesAfterValidation() {
    final WebServer server = mock(WebServer.class);
    final Result<Unit, Exception> registered = Result.ok(Unit.INSTANCE);
    when(server.registerRoutes()).thenReturn(registered);

    try (MockedStatic<BeanPool> pool = mockStatic(BeanPool.class);
         MockedStatic<Validation> validation = mockStatic(Validation.class);
         MockedStatic<WebServer> loaded = mockStatic(WebServer.class)) {
      pool.when(BeanPool::initialize).thenReturn(Result.ok(Unit.INSTANCE));
      loaded.when(WebServer::load).thenReturn(server);
      loaded.when(WebServer::reload).thenCallRealMethod();

      assertSame(registered, WebServer.reload());
      validation.verify(Validation::reset);
      validation.verify(Validation::initialize);
    }

    verify(server).registerRoutes();
  }

  private static final class StubServer implements WebServer {

    @Override
    public Result<Unit, Exception> start(final int port, final String host) {
      return Result.ok(Unit.INSTANCE);
    }

    @Override
    public Result<Unit, Exception> registerRoutes() {
      return Result.ok(Unit.INSTANCE);
    }

    @Override
    public byte[] exchange(
        final String baseUrl,
        final String method,
        final String path,
        final String contentType,
        final byte[] body) {
      return body == null ? new byte[0] : body;
    }
  }
}
