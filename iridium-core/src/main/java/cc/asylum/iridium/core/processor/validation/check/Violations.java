package cc.asylum.iridium.core.processor.validation.check;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.stmt.Stmt;
import cc.asylum.iridium.codegen.code.Blocks;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.validation.ConstraintViolation;

@Internal
public final class Violations {

  public static final Expr LIST = Exprs.name("violations");
  public static final Expr VALUE = Exprs.name("value");

  private Violations() {
  }

  public static Stmt add(final Expr path, final Expr message, final Expr rejected) {
    return Blocks.expr(LIST.invoke("add", Exprs.new_(ConstraintViolation.class, path, message, rejected)));
  }

  public static Stmt when(
      final Expr condition,
      final ValidatedField field,
      final String message,
      final Expr rejected
  ) {
    return Blocks.ifThen(condition, add(Expr.lit(field.name()), Expr.lit(message), rejected));
  }
}
