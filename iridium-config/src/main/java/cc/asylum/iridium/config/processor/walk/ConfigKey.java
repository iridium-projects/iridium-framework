package cc.asylum.iridium.config.processor.walk;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.code.Exprs;

public record ConfigKey(String literal, String index, String suffix, Expr expression) {

  public static ConfigKey literal(final String value) {
     return new ConfigKey(value, null, "", null);
  }

  private static ConfigKey expression(final Expr expression) {
     return new ConfigKey(null, null, "", expression);
  }

  public ConfigKey child(final String name) {
    if (expression != null) {
       return expression(expression.concat(Expr.lit("." + name)));
    }
    if (index == null) {
       return literal(literal.isEmpty() ? name : literal + "." + name);
    }
    if (suffix.isEmpty()) {
       return new ConfigKey(literal, index, name, null);
    }
     return new ConfigKey(literal, index, suffix + "." + name, null);
  }

  public ConfigKey indexed(final String indexName) {
    if (expression != null || index != null) {
       return expression(code().concat(Expr.lit("[")).concat(Exprs.name(indexName)).concat(Expr.lit("]")));
    }
     return new ConfigKey(literal, indexName, "", null);
  }

  public Expr code() {
    if (expression != null) {
      return expression;
    }
    if (index == null) {
      return Expr.lit(literal);
    }
    if (suffix.isEmpty()) {
      return Expr.lit(literal + "[").concat(Exprs.name(index)).concat(Expr.lit("]"));
    }
    return Expr.lit(literal + "[").concat(Exprs.name(index)).concat(Expr.lit("]." + suffix));
  }
}
