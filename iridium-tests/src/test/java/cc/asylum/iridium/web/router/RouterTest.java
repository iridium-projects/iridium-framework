package cc.asylum.iridium.web.router;

import cc.asylum.iridium.web.middleware.Middleware;
import cc.asylum.iridium.web.response.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouterTest {

  @Test
  void matchesMethodCaseInsensitivelyAndCapturesVariables() throws Exception {
    final Router router = new Router();
    router.register("GET", "/users/{id}/posts/{slug}", request -> Response.ok(request.pathVariable("id").orElse("")
        + ":" + request.pathVariable("slug").orElse("")));

    final Response<?> response = router.dispatch(request("get", "/users/42/posts/hello"));
    assertEquals(200, response.status());
    assertEquals("42:hello", response.body());
  }

  @Test
  void normalizesMissingSlashAndReturnsNotFound() throws Exception {
    final Router router = new Router();
    router.register("POST", "echo", request -> Response.ok("hit"));

    assertEquals("hit", router.dispatch(request("POST", "/echo")).body());
    assertEquals(404, router.dispatch(request("GET", "/echo")).status());
    assertEquals(404, router.dispatch(request("POST", "/echo/extra")).status());
    assertEquals(404, router.dispatch(request("POST", "/other")).status());
  }

  @Test
  void firstMatchingRouteWinsAndTrailingSlashStillMatches() throws Exception {
    final Router router = new Router();
    router.register("GET", "/hello", request -> Response.ok("first"));
    router.register("GET", "/hello", request -> Response.ok("second"));

    assertEquals("first", router.dispatch(request("GET", "/hello/")).body());
    assertEquals("first", router.dispatch(request("GET", "/hello")).body());
  }

  @Test
  void emptyPathMatchesRootAndEmptyVariableNameIsCaptured() throws Exception {
    final Router router = new Router();
    router.register("GET", "", request -> Response.ok("root"));
    router.register("GET", "/files/{}", request -> Response.ok(request.pathVariable("").orElse("missing")));

    assertEquals("root", router.dispatch(request("GET", "/")).body());
    assertEquals("readme", router.dispatch(request("GET", "/files/readme")).body());
  }

  @Test
  void runsMiddlewareByPriorityAndPropagatesHandlerFailures() {
    final Router router = new Router();
    final StringBuilder order = new StringBuilder();
    router.use((request, next) -> {
      order.append("late");
      return next.proceed();
    });
    router.use(new Middleware() {
      @Override
      public Response<?> handle(final Request request, final Next next) throws Exception {
        order.append("early");
        return next.proceed();
      }

      @Override
      public int priority() {
        return -10;
      }
    });
    router.register("GET", "/boom", request -> {
      throw new IllegalStateException("boom");
    });

    final IllegalStateException thrown = assertThrows(IllegalStateException.class,
        () -> router.dispatch(request("GET", "/boom")));
    assertEquals("boom", thrown.getMessage());
    assertEquals("earlylate", order.toString());
  }

  @Test
  void middlewareCanShortCircuit() throws Exception {
    final Router router = new Router();
    final AtomicReference<String> seen = new AtomicReference<>();
    router.use((request, next) -> Response.status(401).build());
    router.register("GET", "/secret", request -> {
      seen.set("handler");
      return Response.ok("no");
    });

    assertEquals(401, router.dispatch(request("GET", "/secret")).status());
    assertTrue(seen.get() == null);
  }

  private static Request request(final String method, final String path) {
    return new Request(method, path, Map.of(), Map.of(), Map.of(), null);
  }
}
