package cc.asylum.iridium.web.openapi;

import cc.asylum.iridium.core.bean.BeanPool;
import cc.asylum.iridium.web.openapi.config.OpenAPIConfig;
import cc.asylum.iridium.web.response.Response;
import cc.asylum.iridium.web.router.Request;
import cc.asylum.iridium.web.router.Router;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAPIWebRegistrarTest {

  @Test
  void servesSwaggerUiAndSpecAtConfiguredPath() throws Exception {
    BeanPool.instance().put("openAPIConfig", new OpenAPIConfig("/api-docs", "Iridium API", "1.0.0"));

    final Router router = new Router();
    new OpenAPIWebRegistrar().register(router);

    final Response<?> ui = router.dispatch(request("GET", "/api-docs"));
    assertEquals(200, ui.status());
    assertEquals("text/html; charset=utf-8", ui.headers().get("Content-Type"));
    assertTrue(ui.body().toString().contains("/api-docs/openapi.json"));
    assertTrue(ui.body().toString().contains("SwaggerUIBundle"));

    final Response<?> spec = router.dispatch(request("GET", "/api-docs/openapi.json"));
    assertEquals(200, spec.status());
    assertEquals("application/json", spec.headers().get("Content-Type"));
    assertTrue(spec.body().toString().contains("Iridium API"));
  }

  private static Request request(final String method, final String path) {
    return new Request(method, path, Map.of(), Map.of(), Map.of(), null);
  }
}
