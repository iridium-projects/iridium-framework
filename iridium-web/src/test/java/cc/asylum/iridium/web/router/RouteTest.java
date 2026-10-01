package cc.asylum.iridium.web.router;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RouteTest {

  @Test
  void exactRouteMatchesOnlyIdenticalSegments() {
    final Handler handler = request -> null;
    final Route route = new Route("get", "/users", handler);

    assertEquals("GET", route.method());
    assertEquals("/users", route.path());
    assertSame(handler, route.handler());
    assertFalse(route.readsBody());
    assertTrue(route.exact());
    assertSame(Router.NO_VARIABLES, route.match(new String[] {"", "users"}));
    assertNull(route.match(new String[] {"", "other"}));
    assertNull(route.match(new String[] {"", "users", "1"}));
  }

  @Test
  void variableRouteCapturesNamedSegments() {
    final Route route = new Route("DELETE", "/users/{id}/posts/{postId}", new Handler() {
      @Override
      public cc.asylum.iridium.web.response.Response<?> handle(final Request request) {
        return null;
      }

      @Override
      public boolean readsBody() {
        return true;
      }
    });

    assertFalse(route.exact());
    assertTrue(route.readsBody());
    assertEquals(Map.of("id", "4", "postId", "9"), route.match(new String[] {"", "users", "4", "posts", "9"}));
    assertNull(route.match(new String[] {"", "accounts", "4", "posts", "9"}));
  }

  @Test
  void openBraceStillMatchesLiteralSegment() {
    final Route route = new Route("GET", "/files/{name", request -> null);
    assertFalse(route.exact());
    assertSame(Router.NO_VARIABLES, route.match(Request.split("/files/{name")));
    assertNull(route.match(Request.split("/files/other")));
  }
}
