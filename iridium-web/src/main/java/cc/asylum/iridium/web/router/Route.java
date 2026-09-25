package cc.asylum.iridium.web.router;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.asylum.iridium.core.util.Paths;

public final class Route {

  private final String method;
  private final String path;
  private final String[] segments;
  private final String[] names;
  private final boolean[] variables;
  private final Handler handler;
  private final boolean readsBody;
  private final boolean exact;

  protected Route(final String method, final String path, final Handler handler) {
    this.method = Request.normalizeMethod(method);
    this.path = path;
    this.segments = Request.split(path);
    this.names = new String[segments.length];
    this.variables = new boolean[segments.length];
    this.handler = handler;
    this.readsBody = handler.readsBody();
    this.exact = !path.contains("{");

    for (int i = 0; i < segments.length; i++) {
      final String variable = Paths.variable(segments[i]);

      if (variable == null) {
        continue;
      }

      names[i] = variable;
      variables[i] = true;
    }
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

  public boolean readsBody() {
    return readsBody;
  }

  public boolean exact() {
    return exact;
  }

  public Map<String, String> match(final String[] actual) {
    if (segments.length != actual.length) {
      return null;
    }

    Map<String, String> captured = null;

    for (int i = 0; i < segments.length; i++) {
      if (variables[i]) {
        if (captured == null) {
          captured = new LinkedHashMap<>();
        }

        captured.put(names[i], actual[i]);
        continue;
      }

      if (!segments[i].equals(actual[i])) {
        return null;
      }
    }

    return captured == null ? Router.NO_VARIABLES : captured;
  }
}
