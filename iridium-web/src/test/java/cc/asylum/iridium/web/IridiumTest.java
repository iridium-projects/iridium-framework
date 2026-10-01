package cc.asylum.iridium.web;

import cc.asylum.iridium.core.bean.BeanPool;
import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.result.Unit;
import cc.asylum.iridium.web.support.RecordingConfigInitializer;
import cc.asylum.iridium.web.webserver.WebServer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class IridiumTest {

  @Test
  void rejectsMissingApplicationAnnotation() {
    try (MockedStatic<Banner> banner = mockStatic(Banner.class)) {
      final IllegalStateException failure = assertThrows(
          IllegalStateException.class,
          () -> Iridium.run(String.class, new String[0]));
      assertTrue(failure.getMessage().contains(String.class.getName()));
    }
  }

  @Test
  void preparesConfigBeforeRejectingBeanPoolFailure() {
    final IllegalStateException cause = new IllegalStateException("beans");
    final String[] args = {"--port=1"};
    RecordingConfigInitializer.seen = null;

    try (MockedStatic<Banner> banner = mockStatic(Banner.class);
         MockedStatic<BeanPool> pool = mockStatic(BeanPool.class)) {
      pool.when(BeanPool::initialize).thenReturn(Result.err(cause));

      final IllegalStateException failure = assertThrows(
          IllegalStateException.class,
          () -> Iridium.run(Marked.class, args));
      assertSame(cause, failure.getCause());
      assertEquals("beans", failure.getMessage());
    }

    assertSame(args, RecordingConfigInitializer.seen);
  }

  @Test
  void rejectsServerStartFailure() {
    final IllegalStateException cause = new IllegalStateException("bind");
    final WebServer server = mock(WebServer.class);
    when(server.start(8080, "0.0.0.0")).thenReturn(Result.err(cause));

    try (MockedStatic<Banner> banner = mockStatic(Banner.class);
         MockedStatic<BeanPool> pool = mockStatic(BeanPool.class);
         MockedStatic<WebServer> servers = mockStatic(WebServer.class)) {
      pool.when(BeanPool::initialize).thenReturn(Result.ok(Unit.INSTANCE));
      servers.when(WebServer::load).thenReturn(server);

      final IllegalStateException failure = assertThrows(
          IllegalStateException.class,
          () -> Iridium.run(Marked.class, new String[0]));
      assertSame(cause, failure.getCause());
      assertEquals("bind", failure.getMessage());
    }
  }

  @Test
  void rejectsRouteRegistrationFailure() {
    final IllegalStateException cause = new IllegalStateException("routes");
    final WebServer server = mock(WebServer.class);
    when(server.start(8080, "0.0.0.0")).thenReturn(Result.ok(Unit.INSTANCE));
    when(server.registerRoutes()).thenReturn(Result.err(cause));

    try (MockedStatic<Banner> banner = mockStatic(Banner.class);
         MockedStatic<BeanPool> pool = mockStatic(BeanPool.class);
         MockedStatic<WebServer> servers = mockStatic(WebServer.class)) {
      pool.when(BeanPool::initialize).thenReturn(Result.ok(Unit.INSTANCE));
      servers.when(WebServer::load).thenReturn(server);

      final IllegalStateException failure = assertThrows(
          IllegalStateException.class,
          () -> Iridium.run(Marked.class, new String[0]));
      assertSame(cause, failure.getCause());
    }
  }

  @Test
  void listensUntilInterrupted() throws Exception {
    final WebServer server = mock(WebServer.class);
    when(server.start(9, "127.0.0.1")).thenReturn(Result.ok(Unit.INSTANCE));
    when(server.registerRoutes()).thenReturn(Result.ok(Unit.INSTANCE));

    final Thread runner = new Thread(() -> {
      try (MockedStatic<Banner> banner = mockStatic(Banner.class);
           MockedStatic<BeanPool> pool = mockStatic(BeanPool.class);
           MockedStatic<WebServer> servers = mockStatic(WebServer.class)) {
        pool.when(BeanPool::initialize).thenReturn(Result.ok(Unit.INSTANCE));
        servers.when(WebServer::load).thenReturn(server);
        Iridium.run(Hosted.class, new String[0]);
      }
    });
    runner.start();
    Thread.sleep(200);
    assertTrue(runner.isAlive());
    runner.interrupt();
    runner.join(2000);
    assertTrue(!runner.isAlive());
    verify(server).start(9, "127.0.0.1");
  }

  @WebApplication
  private static final class Marked {
  }

  @WebApplication(host = "127.0.0.1", port = 9)
  private static final class Hosted {
  }
}
