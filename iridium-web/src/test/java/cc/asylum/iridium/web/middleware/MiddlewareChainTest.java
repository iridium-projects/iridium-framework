package cc.asylum.iridium.web.middleware;

import cc.asylum.iridium.web.response.Response;
import cc.asylum.iridium.web.router.Handler;
import cc.asylum.iridium.web.router.Request;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MiddlewareChainTest {

  @Test
  void emptyChainInvokesTerminal() throws Exception {
    final Handler terminal = request -> Response.ok(request.path());
    final MiddlewareChain chain = new MiddlewareChain(List.of(), terminal);
    final Response<?> response = chain.invoke(new Request("GET", "/only", Map.of(), Map.of(), Map.of(), null));
    assertEquals("/only", response.body());
  }

  @Test
  void middlewareOrderAndShortCircuit() throws Exception {
    final List<String> seen = new ArrayList<>();
    final Middleware first = (request, next) -> {
      seen.add("first");
      final Response<?> response = next.proceed();
      seen.add("first-after");
      return response;
    };
    final Middleware second = new Middleware() {
      @Override
      public Response<?> handle(final Request request, final Next next) {
        seen.add("second");
        return Response.status(403).build();
      }

      @Override
      public int priority() {
        return 5;
      }
    };
    final Handler terminal = request -> {
      seen.add("terminal");
      return Response.ok("no");
    };

    final Response<?> blocked = new MiddlewareChain(List.of(first, second), terminal)
      .invoke(new Request("GET", "/", Map.of(), Map.of(), Map.of(), null));
    assertEquals(403, blocked.status());
    assertEquals(List.of("first", "second", "first-after"), seen);
    assertEquals(0, new Middleware() {
      @Override
      public Response<?> handle(final Request request, final Next next) {
        return null;
      }
    }.priority());
  }

  @Test
  void terminalExceptionPropagates() {
    final MiddlewareChain chain = new MiddlewareChain(
      List.of((request, next) -> next.proceed()),
      request -> {
        throw new IllegalArgumentException("bad");
      });
    assertThrows(IllegalArgumentException.class, () -> chain.invoke(new Request("GET", "/", Map.of(), Map.of(), Map.of(), null)));
  }
}
