package cc.asylum.iridium.core.processor.validation.check;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.stmt.Stmt;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.validation.annotation.Email;
import cc.asylum.iridium.core.validation.annotation.Pattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Internal
public final class TextChecks implements ConstraintCheck {

  public static final String EMAIL_REGEX = "^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$";

  @Override
  public List<Stmt> emit(final ValidatedField field, final Map<String, String> patterns) {
    final List<Stmt> statements = new ArrayList<>();
    final Pattern pattern = field.element().getAnnotation(Pattern.class);
    if (pattern != null) {
      statements.add(match(field, named(patterns, pattern.regexp()), "must match " + pattern.regexp()));
    }

    if (field.element().getAnnotation(Email.class) != null) {
      statements.add(match(field, named(patterns, EMAIL_REGEX), "must be a valid email address"));
    }

    return statements;
  }

  private Stmt match(final ValidatedField field, final String pattern, final String message) {
    final Expr acc = field.accessor();
    return Violations.when(
        acc.ne(Expr.nil()).and(Exprs.name(pattern).invoke("matcher", acc).invoke("matches").not()),
        field,
        message,
        acc);
  }

  private static String named(final Map<String, String> patterns, final String regexp) {
    for (final Map.Entry<String, String> entry : patterns.entrySet()) {
      if (entry.getValue().equals(regexp)) {
        return entry.getKey();
      }
    }

    throw new IllegalStateException(regexp);
  }
}
