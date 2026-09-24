package cc.asylum.iridium.web.openapi.service;

import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.web.openapi.config.OpenAPIConfig;
import cc.asylum.iridium.web.router.Router;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class OpenAPIService {

  public OpenAPI buildOpenAPI(final OpenAPIConfig openAPIConfig, final Router router) {
    final OpenAPI openAPI = new OpenAPI().info(new Info()
        .title(openAPIConfig.title())
        .version(openAPIConfig.version()));

    for (final Router.Route route : router.getRoutes()) {
      final var methodResult = httpMethod(route.method());

      if (methodResult.isErr()) {
        log.error("Failed to find HTTP method for {}. {}", route.path(), methodResult.unwrapErr());
        continue;
      }

      final PathItem pathItem = pathItem(openAPI, route.path());
      pathItem.operation(methodResult.unwrap(), operation(route));
      openAPI.path(route.path(), pathItem);
    }

    return openAPI;
  }

  private PathItem pathItem(final OpenAPI openAPI, final String path) {
    if (openAPI.getPaths() != null && openAPI.getPaths().containsKey(path)) {
      return openAPI.getPaths().get(path);
    }

    return new PathItem();
  }

  private Operation operation(final Router.Route route) {
    final Operation operation = new Operation()
        .operationId(operationId(route))
        .responses(new ApiResponses()
            .addApiResponse("200", new ApiResponse().description("OK")));

    for (final String segment : route.path().split("/")) {
      if (segment.startsWith("{") && segment.endsWith("}") && segment.length() > 2) {
        operation.addParametersItem(new Parameter()
            .name(segment.substring(1, segment.length() - 1))
            .in("path")
            .required(true)
            .schema(new StringSchema()));
      }
    }

    return operation;
  }

  private String operationId(final Router.Route route) {
    return route.method().toLowerCase() + route.path().replaceAll("[^A-Za-z0-9]+", "_");
  }

  private Result<PathItem.HttpMethod, Exception> httpMethod(final String method) {
    return Result.of(() -> PathItem.HttpMethod.valueOf(method.toUpperCase()));
  }
}
