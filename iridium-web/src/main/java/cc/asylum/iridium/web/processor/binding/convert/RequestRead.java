package cc.asylum.iridium.web.processor.binding.convert;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.web.controller.Parameters;
import cc.asylum.iridium.web.controller.parameter.BindingSource;
import cc.asylum.iridium.web.response.Response;

@Internal
public final class RequestRead {

  public static final Expr REQUEST = Exprs.name("_request");

  private RequestRead() {
  }

  public static Expr raw(final BindingSource source, final String name, final Expr fallback) {
    if (source == BindingSource.BODY) {
      return Exprs.invokeStatic(Parameters.class, "bodyBytes", REQUEST);
    }

    final String method = switch (source) {
      case PATH -> "pathVariable";
      case HEADER -> "header";
      case COOKIE -> "cookie";
      default -> "query";
    };

    return Exprs.invokeStatic(Parameters.class, method, REQUEST, Expr.lit(name), fallback);
  }

  public static Expr badRequest(final Expr body) {
    return Exprs.invokeStatic(Response.class, "badRequest").invoke("body", body);
  }

  public static String label(final BindingSource source) {
    return switch (source) {
      case PATH -> "path variable";
      case HEADER -> "header";
      case COOKIE -> "cookie";
      case BODY -> "request body";
      default -> "query parameter";
    };
  }
}
