package cc.asylum.iridium.web.openapi;

import cc.asylum.iridium.web.openapi.config.OpenAPIConfig;
import cc.asylum.iridium.web.openapi.service.OpenAPIService;
import cc.asylum.iridium.web.response.Response;
import cc.asylum.iridium.web.router.Router;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAPIServiceTest {

  @Test
  void buildsPathsAndPathParametersFromRouter() {
    final Router router = new Router();
    router.register("GET", "/users/{id}", request -> Response.ok("ok"));
    router.register("POST", "/users/{id}", request -> Response.ok("ok"));

    final OpenAPI openAPI = new OpenAPIService().buildOpenAPI(
        new OpenAPIConfig("/api-docs", "Iridium API", "1.0.0"),
        router
    );
    assertEquals("Iridium API", openAPI.getInfo().getTitle());
    assertEquals("1.0.0", openAPI.getInfo().getVersion());

    final PathItem users = openAPI.getPaths().get("/users/{id}");
    assertEquals("id", users.getGet().getParameters().getFirst().getName());
    assertEquals("path", users.getGet().getParameters().getFirst().getIn());
    assertTrue(users.getGet().getParameters().getFirst().getRequired());
    assertEquals(200, Integer.parseInt(users.getPost().getResponses().keySet().iterator().next()));
  }
}
