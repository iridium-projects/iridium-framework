package cc.asylum.iridium.core.processor.validation.check;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.stmt.Stmt;
import cc.asylum.iridium.codegen.code.Blocks;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.model.TypeMirrors;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.validation.annotation.NotBlank;
import cc.asylum.iridium.core.validation.annotation.NotEmpty;
import cc.asylum.iridium.core.validation.annotation.NotNull;
import cc.asylum.iridium.core.validation.annotation.Size;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Internal
public final class PresenceChecks implements ConstraintCheck {

  private final javax.lang.model.util.Types types;
  private final Elements elements;

  public PresenceChecks(final javax.lang.model.util.Types types, final Elements elements) {
    this.types = types;
    this.elements = elements;
  }

  @Override
  public List<Stmt> emit(final ValidatedField field, final Map<String, String> patterns) {
    final Element element = field.element();
    final Expr acc = field.accessor();
    final List<Stmt> statements = new ArrayList<>();
    if (!field.type().getKind().isPrimitive() && element.getAnnotation(NotNull.class) != null) {
      statements.add(Violations.when(acc.eq(Expr.nil()), field, "must not be null", Expr.nil()));
    }

    if (element.getAnnotation(NotBlank.class) != null) {
      statements.add(Violations.when(acc.eq(Expr.nil()).or(acc.invoke("isBlank")), field, "must not be blank", acc));
    }

    if (element.getAnnotation(NotEmpty.class) != null) {
      statements.add(Violations.when(acc.eq(Expr.nil()).or(empty(field, acc)), field, "must not be empty", acc));
    }

    final Size size = element.getAnnotation(Size.class);
    if (size != null) {
      statements.add(Blocks.ifThen(
          acc.ne(Expr.nil()),
          Blocks.declare(int.class, "size", sizeOf(field, acc)),
          Violations.when(Exprs.name("size").lt(Expr.lit(size.min())).or(Exprs.name("size").gt(Expr.lit(size.max()))),
              field, "size must be between " + size.min() + " and " + size.max(), acc)));
    }

    return statements;
  }

  private Expr sizeOf(final ValidatedField field, final Expr acc) {
    if (field.type().getKind() == TypeKind.ARRAY) {
      return acc.field("length");
    }

    if (TypeMirrors.isAssignable(types, elements, field.type(), CharSequence.class)) {
      return acc.invoke("length");
    }

    return acc.invoke("size");
  }

  private Expr empty(final ValidatedField field, final Expr acc) {
    if (field.type().getKind() == TypeKind.ARRAY) {
      return acc.field("length").eq(Expr.lit(0));
    }

    return acc.invoke("isEmpty");
  }
}
