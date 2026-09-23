package cc.asylum.iridium.test;

import cc.asylum.iridium.web.router.Request;
import cc.asylum.iridium.web.router.Router;
import cc.asylum.iridium.web.response.Response;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RoutingTest {

    @Test
    void queryParameterBinding() throws Exception {
        final Router router = Router.load();
        final Request request = new Request(
                "GET", "/echo",
                Map.of(),
                Map.of("value", List.of("iridium"), "count", List.of("3")),
                Map.of(),
                new byte[0]
        );

        final Response<?> response = router.dispatch(request);

        assertEquals(200, response.status());
        assertEquals("required=iridium count=3 flag=true", response.body());
    }

    @Test
    void pathVariableAndHeaderBinding() throws Exception {
        final Router router = Router.load();
        final Request request = new Request(
                "GET", "/users/42/profile",
                Map.of("Authorization", List.of("token")),
                Map.of(),
                Map.of(),
                new byte[0]
        );

        final Response<?> response = router.dispatch(request);

        assertEquals(200, response.status());
        assertEquals("profile of 42 auth=token session=none verbose=false", response.body());
    }

    @Test
    void unknownRouteReturns404() throws Exception {
        final Router router = Router.load();
        final Request request = new Request(
                "GET", "/missing",
                Map.of(),
                Map.of(),
                Map.of(),
                new byte[0]
        );

        final Response<?> response = router.dispatch(request);

        assertEquals(404, response.status());
    }
}
