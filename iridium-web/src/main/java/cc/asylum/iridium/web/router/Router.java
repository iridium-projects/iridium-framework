package cc.asylum.iridium.web.router;

import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.bean.BeanPool;
import cc.asylum.iridium.core.util.Paths;
import cc.asylum.iridium.core.component.Component;
import cc.asylum.iridium.web.webserver.WebRegistrar;
import lombok.Getter;
import cc.asylum.iridium.web.middleware.Middleware;
import cc.asylum.iridium.web.middleware.MiddlewareChain;
import cc.asylum.iridium.web.response.Response;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

@Getter
@Internal
@Component
public final class Router {

  public static final Map<String, String> NO_VARIABLES = Map.of();

  private final List<Route> routes = new ArrayList<>();
  private final Map<String, List<Route>> byMethod = new HashMap<>();
  private final List<Middleware> middlewares = new ArrayList<>();

  private List<Middleware> chain;

  public void register(
      final String method,
      final String path,
      final Handler handler) {
    final Route route = new Route(
        method,
        Paths.normalize(path),
        handler);

    routes.add(route);

    byMethod.computeIfAbsent(route.method(), ignored -> new ArrayList<>())
        .add(route);
  }

  public void use(final Middleware middleware) {
    middlewares.add(middleware);
    chain = null;
  }

  public Response<?> dispatch(final Request request) throws Exception {
    final String method = Request.normalizeMethod(request.method());
    final List<Route> routes = byMethod.get(method);

    if (routes == null) {
      return Response.notFound().build();
    }

    final String[] segments = request.segments() != null
        ? request.segments()
        : Request.split(request.path());

    for (final Route route : routes) {
      final Map<String, String> variables = route.match(segments);

      if (variables == null) {
        continue;
      }

      Request matched = variables == NO_VARIABLES
          ? request
          : request.withPathVariables(variables);

      if (route.readsBody() && matched.body() == null) {
        matched = matched.withBody(readBody(matched));
      }

      return invoke(matched, route.handler());
    }

    return Response.notFound().build();
  }

  private static byte[] readBody(final Request request) throws Exception {
    final InputStream input = request.input();

    if (input == null) {
      return new byte[0];
    }

    final var bytes = input.readAllBytes();
    input.close();

    return bytes;

  }

  private Response<?> invoke(
      final Request request,
      final Handler terminal) throws Exception {
    final List<Middleware> chain = middlewareChain();

    if (chain.isEmpty()) {
      return terminal.handle(request);
    }

    return new MiddlewareChain(chain, terminal).invoke(request);
  }

  private List<Middleware> middlewareChain() {
    if (chain == null) {
      chain = middlewares.stream()
          .sorted(Comparator.comparingInt(Middleware::priority))
          .toList();
    }

    return chain;
  }

  public static Router load() {
    BeanPool.initialize();

    final Router router = new Router();

    for (final WebRegistrar registrar : ServiceLoader.load(WebRegistrar.class)) {
      registrar.register(router);
    }

    return router;
  }
}
