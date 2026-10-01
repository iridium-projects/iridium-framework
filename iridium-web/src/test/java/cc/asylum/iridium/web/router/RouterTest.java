package cc.asylum.iridium.web.router;

import cc.asylum.iridium.web.middleware.Middleware;
import cc.asylum.iridium.web.response.Response;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RouterTest {

  @Test
  void exactRouteDispatchesWithoutReadingBody() throws Exception {
    final Router router = new Router();
    final AtomicReference<Request> seen = new AtomicReference<>();
    router.register("get", "/users", request -> {
      seen.set(request);
      return Response.ok("listed");
    });

    final Request request = new Request("GET", "/users", Map.of(), Map.of(), Map.of(), null);
    final Response<?> response = router.dispatch(request);

    assertEquals(200, response.status());
    assertEquals("listed", response.body());
    assertSame(request, seen.get());
    assertFalse(router.readsBody("GET", "/users"));
    assertEquals(1, router.getRoutes().size());
  }

  @Test
  void variableRouteCapturesAndSkipsExactEntries() throws Exception {
    final Router router = new Router();
    router.register("POST", "/static", request -> Response.ok("static"));
    router.register("post", "items/{id}/edit", request -> Response.ok(request.pathVariable("id")));

    final Response<?> matched = router.dispatch(request("POST", "/items/9/edit", new String[] {"", "items", "9", "edit"}));
    assertEquals("9", matched.body());

    final Response<?> missing = router.dispatch(request("POST", "/items/9", null));
    assertEquals(404, missing.status());
    assertNull(missing.body());
  }

  @Test
  void unknownMethodAndQueryStripping() throws Exception {
    final Router router = new Router();
    router.register("PUT", "/files/{name}", request -> Response.ok(request.pathVariable("name")));

    assertEquals(404, router.dispatch(request("DELETE", "/files/a", null)).status());

    final Response<?> matched = router.dispatch(request("put", "/files/a?download=1", null));
    assertEquals("a", matched.body());
    assertFalse(router.readsBody("PUT", null));
    assertFalse(router.readsBody("PUT", "/files/a?x=1"));
  }

  @Test
  void readsBodyFromInputAndClosesStream() throws Exception {
    final Router router = new Router();
    router.register("POST", "/echo", new Handler() {
      @Override
      public Response<?> handle(final Request request) {
        return Response.ok(request.body());
      }

      @Override
      public boolean readsBody() {
        return true;
      }
    });

    assertTrue(router.readsBody("post", "/echo?x=1"));
    final TrackingStream input = new TrackingStream("payload".getBytes());
    final Request request = new Request(
      "POST",
      "/echo",
      Request.split("/echo"),
      Map.of(),
      Map.of(),
      () -> input);

    final Response<?> response = router.dispatch(request);
    assertArrayEquals("payload".getBytes(), (byte[]) response.body());
    assertTrue(input.closed);
  }

  @Test
  void readsEmptyBodyWhenInputMissingAndKeepsExistingBody() throws Exception {
    final Router router = new Router();
    router.register("PATCH", "/raw", new Handler() {
      @Override
      public Response<?> handle(final Request request) {
        return Response.ok(request.body());
      }

      @Override
      public boolean readsBody() {
        return true;
      }
    });

    final Response<?> empty = router.dispatch(new Request(
      "PATCH",
      "/raw",
      Request.split("/raw"),
      Map.of(),
      Map.of(),
      () -> null));
    assertEquals(0, ((byte[]) empty.body()).length);

    final byte[] existing = {1, 2};
    final Response<?> kept = router.dispatch(new Request("PATCH", "/raw", Map.of(), Map.of(), Map.of(), existing));
    assertSame(existing, kept.body());
  }

  @Test
  void middlewareRunsInPriorityOrderAndCanShortCircuit() throws Exception {
    final Router router = new Router();
    final List<String> order = new ArrayList<>();
    router.use(new Middleware() {
      @Override
      public Response<?> handle(final Request request, final Next next) throws Exception {
        order.add("late");
        return next.proceed();
      }
    });
    router.use(new Middleware() {
      @Override
      public Response<?> handle(final Request request, final Next next) throws Exception {
        order.add("early");
        return next.proceed();
      }

      @Override
      public int priority() {
        return -10;
      }
    });
    router.register("GET", "/chain", request -> {
      order.add("handler");
      return Response.ok("done");
    });

    assertEquals("done", router.dispatch(request("GET", "/chain", null)).body());
    assertEquals(List.of("early", "late", "handler"), order);

    router.use((request, next) -> Response.status(401).build());
    final Response<?> blocked = router.dispatch(request("GET", "/chain", null));
    assertEquals(401, blocked.status());
  }

  @Test
  void handlerExceptionPropagates() {
    final Router router = new Router();
    router.register("GET", "/boom", request -> {
      throw new IllegalStateException("nope");
    });

    final IllegalStateException error = assertThrows(
      IllegalStateException.class,
      () -> router.dispatch(request("GET", "/boom", null)));
    assertEquals("nope", error.getMessage());
  }

  @Test
  void loadReadsServiceRegistrars() {
    final Router router = Router.load();
    assertTrue(router.getRoutes().stream().anyMatch(route -> "GET".equals(route.method()) && "/loaded".equals(route.path())));
  }

  private static Request request(final String method, final String path, final String[] segments) {
    return new Request(method, path, segments, Map.of(), Map.of(), () -> null);
  }

  private static final class TrackingStream extends ByteArrayInputStream {

    private boolean closed;

    private TrackingStream(final byte[] buf) {
      super(buf);
    }

    @Override
    public void close() throws IOException {
      closed = true;
      super.close();
    }
  }
}
