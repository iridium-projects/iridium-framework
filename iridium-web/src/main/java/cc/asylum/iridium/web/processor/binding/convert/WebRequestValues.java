package cc.asylum.iridium.web.processor.binding.convert;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.web.controller.Parameters;
import cc.asylum.iridium.web.response.Response;

@Internal
public final class WebRequestValues implements RequestValues {

  private static final Expr REQUEST = Exprs.name("_request");

  @Override
  public Expr one(final boolean header, final String name, final Expr fallback) {
    return Exprs.invokeStatic(Parameters.class, header ? "header" : "query", REQUEST, Expr.lit(name), fallback);
  }

  @Override
  public Expr many(final boolean header, final String name) {
    return Exprs.invokeStatic(Parameters.class, header ? "headers" : "queries", REQUEST, Expr.lit(name));
  }

  @Override
  public Expr invalid(final Expr raw, final String param) {
    final Expr message = raw == null
        ? Expr.lit("Invalid value for '" + param + "'")
        : Expr.lit("Invalid value '").concat(raw).concat(Expr.lit("' for '" + param + "'"));
    return Exprs.invokeStatic(Response.class, "badRequest").invoke("body", message);
  }
}
