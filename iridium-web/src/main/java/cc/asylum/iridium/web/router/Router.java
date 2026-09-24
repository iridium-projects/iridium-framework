package cc.asylum.iridium.web.router;

import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.bean.BeanPool;
import cc.asylum.iridium.core.component.Component;
import cc.asylum.iridium.web.webserver.WebRegistrar;
import lombok.Getter;
import cc.asylum.iridium.web.middleware.Middleware;
import cc.asylum.iridium.web.middleware.MiddlewareChain;
import cc.asylum.iridium.web.response.Response;

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

  private final List<Route> routes = new ArrayList<>();
  private final List<MiddlewareRegistration> middlewares = new ArrayList<>();

  public void register(final String method, final String path, final Handler handler) {
    routes.add(new Route(method, normalize(path), handler));
  }

  public void use(final Middleware middleware) {
    middlewares.add(new MiddlewareRegistration(middleware, middleware.priority()));
  }

  public Response<?> dispatch(final Request request) throws Exception {
    for (final Route route : routes) {
      if (!route.method.equalsIgnoreCase(request.method())) {
        continue;
      }
      final Map<String, String> variables = route.match(request.path());
      if (variables == null) {
        continue;
      }
      return invoke(request.withPathVariables(variables), route.handler());
    }
    return Response.notFound().build();
  }

  private Response<?> invoke(final Request request, final Handler terminal) throws Exception {
    final List<Middleware> chain = middlewares.stream()
        .sorted(Comparator.comparingInt(MiddlewareRegistration::priority))
        .map(MiddlewareRegistration::middleware)
        .toList();
    return new MiddlewareChain(chain, terminal).invoke(request);
  }

  public static Router load() {
    BeanPool.initialize();
    final Router router = new Router();

    for (final WebRegistrar registrar : ServiceLoader.load(WebRegistrar.class)) {
      registrar.register(router);
    }

    return router;
  }

  private record MiddlewareRegistration(Middleware middleware, int priority) {
  }

  public static final class Route {

    private final String method;
    private final String path;
    private final String[] segments;
    private final Handler handler;

    private Route(final String method, final String path, final Handler handler) {
      this.method = method;
      this.path = path;
      this.segments = path.split("/");
      this.handler = handler;
    }

    public String method() {
      return method;
    }

    public String path() {
      return path;
    }

    public Handler handler() {
      return handler;
    }

    private Map<String, String> match(final String requestPath) {
      final String[] actual = requestPath.split("/");
      if (segments.length != actual.length) {
        return null;
      }
      final Map<String, String> variables = new HashMap<>();
      for (int i = 0; i < segments.length; i++) {
        final String segment = segments[i];
        if (segment.startsWith("{") && segment.endsWith("}")) {
          variables.put(segment.substring(1, segment.length() - 1), actual[i]);
        } else if (!segment.equals(actual[i])) {
          return null;
        }
      }
      return variables;
    }
  }

  private static String normalize(final String path) {
    if (path == null || path.isEmpty()) {
      return "/";
    }
    return path.startsWith("/") ? path : "/" + path;
  }
}
