package cc.asylum.iridium.web.middleware;

import cc.asylum.iridium.web.response.Response;
import cc.asylum.iridium.web.router.Request;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MiddlewareChainTest {

  @Test
  void emptyChainInvokesTheTerminalHandler() throws Exception {
    final MiddlewareChain chain = new MiddlewareChain(List.of(), request -> Response.ok("done"));
    assertEquals("done", chain.invoke(request()).body());
  }

  @Test
  void copiesTheMiddlewareListAndPropagatesExceptions() {
    final List<Middleware> middlewares = new java.util.ArrayList<>();
    middlewares.add((request, next) -> next.proceed());
    final MiddlewareChain chain = new MiddlewareChain(middlewares, request -> {
      throw new IllegalArgumentException("fail");
    });
    middlewares.clear();

    assertThrows(IllegalArgumentException.class, () -> chain.invoke(request()));
    assertThrows(NullPointerException.class, () -> new MiddlewareChain(null, request -> Response.ok("x")));
  }

  private static Request request() {
    return new Request("GET", "/", Map.of(), Map.of(), Map.of(), null);
  }
}
