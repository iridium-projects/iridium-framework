package cc.asylum.iridium.core.processor.validation.check;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.stmt.Stmt;
import cc.asylum.iridium.codegen.code.Blocks;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.model.TypeMirrors;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.validation.annotation.Digits;
import cc.asylum.iridium.core.validation.annotation.Max;
import cc.asylum.iridium.core.validation.annotation.Min;
import cc.asylum.iridium.core.validation.annotation.Negative;
import cc.asylum.iridium.core.validation.annotation.NegativeOrZero;
import cc.asylum.iridium.core.validation.annotation.Positive;
import cc.asylum.iridium.core.validation.annotation.PositiveOrZero;

import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Internal
public final class NumericChecks implements ConstraintCheck {

  private final javax.lang.model.util.Types types;
  private final Elements elements;

  public NumericChecks(final javax.lang.model.util.Types types, final Elements elements) {
    this.types = types;
    this.elements = elements;
  }

  @Override
  public List<Stmt> emit(final ValidatedField field, final Map<String, String> patterns) {
    final Element element = field.element();
    final Expr acc = field.accessor();
    final List<Stmt> statements = new ArrayList<>();
    final Min min = element.getAnnotation(Min.class);

    if (min != null) {
      statements.add(bound(field, acc, "must be >= " + min.value(), "<", min.value()));
    }

    final Max max = element.getAnnotation(Max.class);
    if (max != null) {
      statements.add(bound(field, acc, "must be <= " + max.value(), ">", max.value()));
    }

    if (element.getAnnotation(Positive.class) != null) {
      statements.add(sign(field, acc, "must be positive", "<=", 0));
    }

    if (element.getAnnotation(Negative.class) != null) {
      statements.add(sign(field, acc, "must be negative", ">=", 0));
    }

    if (element.getAnnotation(PositiveOrZero.class) != null) {
      statements.add(sign(field, acc, "must be positive or zero", "<", 0));
    }

    if (element.getAnnotation(NegativeOrZero.class) != null) {
      statements.add(sign(field, acc, "must be negative or zero", ">", 0));
    }

    final Digits digits = element.getAnnotation(Digits.class);
    if (digits != null) {
      statements.add(digits(field, acc, digits));
    }

    return statements;
  }

  private Stmt bound(
    final ValidatedField field,
    final Expr acc,
    final String message,
    final String op,
    final long value
  ) {
    final Expr compared = compare(field, acc, op, Expr.lit(value));
    final Expr condition = field.type().getKind().isPrimitive() ? compared : acc.ne(Expr.nil()).and(compared);
    return Violations.when(condition, field, message, acc);
  }

  private Expr compare(final ValidatedField field, final Expr acc, final String op, final Expr bound) {
    if (same(field, BigDecimal.class) || same(field, BigInteger.class)) {
      final Class<?> type = same(field, BigDecimal.class) ? BigDecimal.class : BigInteger.class;
      return binary(acc.invoke("compareTo", Exprs.invokeStatic(type, "valueOf", bound)), op, Expr.lit(0));
    }
    return binary(acc, op, bound);
  }

  private Stmt sign(
    final ValidatedField field,
    final Expr acc,
    final String message,
    final String op,
    final int bound
  ) {
    final Expr probe = same(field, BigDecimal.class) || same(field, BigInteger.class) ? acc.invoke("signum") : acc;
    final Expr condition = binary(probe, op, Expr.lit(bound));
    final Expr full = field.type().getKind().isPrimitive() ? condition : acc.ne(Expr.nil()).and(condition);
    return Violations.when(full, field, message, acc);
  }

  private Stmt digits(final ValidatedField field, final Expr acc, final Digits digits) {
    final String message = "numeric value out of bounds (<" + digits.integer() + " digits>.<" + digits.fraction() + " digits>)";
    final Stmt check = Blocks.block(
      Blocks.declare(BigDecimal.class, "d", Exprs.new_(BigDecimal.class, Exprs.invokeStatic(String.class, "valueOf", acc))),
      Violations.when(
        Exprs.name("d").invoke("precision").minus(Exprs.name("d").invoke("scale")).gt(Expr.lit(digits.integer()))
          .or(Exprs.name("d").invoke("scale").gt(Expr.lit(digits.fraction()))),
        field, message, acc));
    return field.type().getKind().isPrimitive() ? check : Blocks.ifThen(acc.ne(Expr.nil()), check);
  }

  private Expr binary(final Expr left, final String op, final Expr right) {
    return switch (op) {
      case "<" -> left.lt(right);
      case ">" -> left.gt(right);
      case "<=" -> left.le(right);
      case ">=" -> left.ge(right);
      case "==" -> left.eq(right);
      case "!=" -> left.ne(right);
      default -> throw new IllegalArgumentException(op);
    };
  }

  private boolean same(final ValidatedField field, final Class<?> target) {
    return TypeMirrors.isSame(types, elements, field.type(), target);
  }
}
