package cc.asylum.iridium.core.processor.validation.check;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.stmt.Stmt;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.code.Types;
import cc.asylum.iridium.codegen.model.Diagnostics;
import cc.asylum.iridium.codegen.model.TypeMirrors;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.validation.annotation.Future;
import cc.asylum.iridium.core.validation.annotation.Past;

import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Internal
public final class TemporalChecks implements ConstraintCheck {

  private final javax.lang.model.util.Types types;
  private final Elements elements;
  private final Messager messager;

  public TemporalChecks(final javax.lang.model.util.Types types, final Elements elements, final Messager messager) {
    this.types = types;
    this.elements = elements;
    this.messager = messager;
  }

  @Override
  public List<Stmt> emit(final ValidatedField field, final Map<String, String> patterns) {
    final List<Stmt> statements = new ArrayList<>();
    if (field.element().getAnnotation(Past.class) != null) {
      statements.add(check(field, true));
    }

    if (field.element().getAnnotation(Future.class) != null) {
      statements.add(check(field, false));
    }

    return statements;
  }

  private Stmt check(final ValidatedField field, final boolean past) {
    final Expr acc = field.accessor();
    final String message = past ? "must be in the past" : "must be in the future";
    if (TypeMirrors.isSame(types, elements, field.type(), Date.class)) {
      return Violations.when(
          acc.ne(Expr.nil()).and(acc.invoke(past ? "before" : "after", Exprs.new_(Date.class)).not()),
          field, message, acc);
    }

    final Optional<TypeElement> temporal = TypeMirrors.asTypeElement(types, field.type());
    if (temporal.isEmpty()) {
      Diagnostics.error(messager, field.element(),
          "@Past/@Future requires a java.util.Date or a java.time type, but found " + field.type());
      return null;
    }

    return Violations.when(
        acc.ne(Expr.nil()).and(acc.invoke(past ? "isBefore" : "isAfter", Exprs.invokeStatic(Types.of(temporal.get().asType()), "now")).not()),
        field, message, acc);
  }
}
