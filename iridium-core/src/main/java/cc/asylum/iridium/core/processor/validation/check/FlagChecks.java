package cc.asylum.iridium.core.processor.validation.check;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.stmt.Stmt;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.validation.annotation.AssertFalse;
import cc.asylum.iridium.core.validation.annotation.AssertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Internal
public final class FlagChecks implements ConstraintCheck {

  @Override
  public List<Stmt> emit(final ValidatedField field, final Map<String, String> patterns) {
    final List<Stmt> statements = new ArrayList<>();

    final boolean primitive = field.type().getKind().isPrimitive();
    if (field.element().getAnnotation(AssertTrue.class) != null) {
      statements.add(flag(field, primitive, true));
    }

    if (field.element().getAnnotation(AssertFalse.class) != null) {
      statements.add(flag(field, primitive, false));
    }

    return statements;
  }

  private Stmt flag(final ValidatedField field, final boolean primitive, final boolean expected) {
    final Expr acc = field.accessor();
    final Expr condition = expected ? acc.not() : acc;
    final Expr full = primitive ? condition : acc.eq(Expr.nil()).or(condition);
    return Violations.when(full, field, expected ? "must be true" : "must be false", acc);
  }
}
