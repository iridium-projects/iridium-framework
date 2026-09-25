package cc.asylum.iridium.web.openapi;

import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.bean.BeanPool;
import cc.asylum.iridium.core.util.Paths;
import cc.asylum.iridium.core.util.Strings;
import cc.asylum.iridium.web.openapi.config.OpenAPIConfig;
import cc.asylum.iridium.web.openapi.service.OpenAPIService;
import cc.asylum.iridium.web.response.Response;
import cc.asylum.iridium.web.router.Router;
import cc.asylum.iridium.web.webserver.WebRegistrar;
import io.swagger.v3.core.util.Json;

@Internal
public final class OpenAPIWebRegistrar implements WebRegistrar {

  @Override
  public void register(final Router router) {
    final OpenAPIConfig config = BeanPool.instance().get(OpenAPIConfig.class);
    final OpenAPIService service = new OpenAPIService();
    final String uiPath = config.path();
    final String specPath = specPath(uiPath);
    final String ui = swaggerUi(specPath);
    final String[] spec = new String[1];

    router.register("GET", uiPath, request -> Response.ok()
        .header("Content-Type", "text/html; charset=utf-8")
        .body(ui));

    router.register("GET", specPath, request -> {
      if (spec[0] == null) {
        spec[0] = Json.pretty(service.buildOpenAPI(config, router));
      }

      return Response.ok()
          .header("Content-Type", "application/json")
          .body(spec[0]);
    });
  }

  private String specPath(final String path) {
    if (Strings.blank(path) || "/".equals(path)) {
      return "/openapi.json";
    }

    return Paths.normalize(path) + "/openapi.json";
  }

  private String swaggerUi(final String specPath) {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <title>Swagger UI</title>
          <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css">
        </head>
        <body>
          <div id="swagger-ui"></div>
          <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
          <script>
            SwaggerUIBundle({ url: '%s', dom_id: '#swagger-ui' });
          </script>
        </body>
        </html>
        """.formatted(specPath);
  }
}
