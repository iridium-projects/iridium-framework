package cc.asylum.iridium.core.processor.validation;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.model.TypeRef;
import cc.asylum.forgery.stmt.Stmt;
import cc.asylum.iridium.codegen.code.Blocks;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.code.Types;
import cc.asylum.iridium.codegen.model.Mirrors;
import cc.asylum.iridium.codegen.write.SourceWriter;
import cc.asylum.iridium.core.processor.validation.check.ConstraintCheck;
import cc.asylum.iridium.core.processor.validation.check.ConstraintChecks;
import cc.asylum.iridium.core.processor.validation.check.TextChecks;
import cc.asylum.iridium.core.processor.validation.check.ValidatedField;
import cc.asylum.iridium.core.processor.validation.check.Violations;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.html.Html;
import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.util.Strings;
import cc.asylum.iridium.core.validation.ConstraintViolation;
import cc.asylum.iridium.core.validation.Escaper;
import cc.asylum.iridium.core.validation.Validator;
import cc.asylum.iridium.core.validation.annotation.Constraint;
import cc.asylum.iridium.core.validation.annotation.Email;
import cc.asylum.iridium.core.validation.annotation.EscapeHtml;
import cc.asylum.iridium.core.validation.annotation.Pattern;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Internal
public final class ValidatorWriter {

  private final List<ConstraintCheck> checks;

  public ValidatorWriter(final javax.lang.model.util.Types types, final Elements elements, final Messager messager) {
    this.checks = ConstraintChecks.all(types, elements, messager);
  }

  public boolean write(final Filer filer, final String pkg, final TypeElement type, final String name) {
    final List<ValidatedField> fields = fields(type);
    if (fields.isEmpty()) {
      return false;
    }
    SourceWriter.writeJava(filer, pkg, name, generated -> generate(generated, type, fields), type);
    return true;
  }

  private List<ValidatedField> fields(final TypeElement type) {
    final List<ValidatedField> result = new ArrayList<>();
    final boolean record = type.getKind() == ElementKind.RECORD;
    for (final Element enclosed : type.getEnclosedElements()) {
      if (enclosed.getKind() != ElementKind.FIELD || !Mirrors.hasMeta(enclosed, Constraint.class)) {
        continue;
      }
      final String name = enclosed.getSimpleName().toString();
      result.add(new ValidatedField(name, enclosed.asType(), enclosed, accessor(type, name, record)));
    }
    return result;
  }

  private Expr accessor(final TypeElement owner, final String name, final boolean record) {
    if (record) {
      return Violations.VALUE.invoke(name);
    }

    final String cap = Strings.capitalize(name);
    if (hasGetter(owner, "get" + cap)) {
      return Violations.VALUE.invoke("get" + cap);
    }

    if (hasGetter(owner, "is" + cap)) {
      return Violations.VALUE.invoke("is" + cap);
    }

    return Violations.VALUE.field(name);
  }

  private boolean hasGetter(final TypeElement owner, final String name) {
    for (final Element enclosed : owner.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.METHOD
        && enclosed.getSimpleName().contentEquals(name)
        && ((ExecutableElement) enclosed).getParameters().isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private void generate(
    final cc.asylum.forgery.type.ClassBuilder type,
    final TypeElement source,
    final List<ValidatedField> fields
  ) {
    final TypeRef typeName = Types.of(source.asType());
    final TypeRef violations = Types.list(Types.of(ConstraintViolation.class));
    final Map<String, String> patterns = patterns(fields);
    final List<Stmt> body = new ArrayList<>();
    body.add(Blocks.declare(violations, "violations", Exprs.new_(Types.parameterized(ArrayList.class, Types.of(ConstraintViolation.class)))));
    body.add(Blocks.ifThen(
      Violations.VALUE.eq(Expr.nil()),
      Violations.add(Expr.lit(""), Expr.lit("must not be null"), Expr.nil()),
      Blocks.ret(err())));

    for (final ValidatedField field : fields) {
      for (final ConstraintCheck check : checks) {
        body.addAll(check.emit(field, patterns));
      }
    }

    body.add(Blocks.ifThen(Violations.LIST.invoke("isEmpty"), Blocks.ret(Exprs.invokeStatic(Result.class, "ok", Violations.VALUE))));
    body.add(Blocks.ret(err()));

    type.implements_(Types.parameterized(Validator.class, typeName));
    type.method("validate", method -> {
      method.public_().overrides();
      method.parameter(typeName, "value");
      method.returns(Types.parameterized(Result.class, typeName, violations));
      method.body(block -> Blocks.addAll(block, body));
    });

    for (final Map.Entry<String, String> pattern : patterns.entrySet()) {
      type.field(java.util.regex.Pattern.class, pattern.getKey(), field -> field
        .private_().static_().final_()
        .init(Exprs.invokeStatic(java.util.regex.Pattern.class, "compile", Expr.lit(pattern.getValue()))));
    }

    if (hasEscape(fields) && source.getKind() == ElementKind.RECORD) {
      type.implements_(Types.parameterized(Escaper.class, typeName));
      type.method("escape", method -> {
        method.public_().overrides();
        method.parameter(typeName, "value");
        method.returns(typeName);
        method.body(block -> block.return_(Exprs.new_(typeName, escapeArgs(source, fields))));
      });
    }
  }

  private List<Expr> escapeArgs(final TypeElement type, final List<ValidatedField> fields) {
    final Set<String> names = new LinkedHashSet<>();
    for (final ValidatedField field : fields) {
      if (field.element().getAnnotation(EscapeHtml.class) != null) {
        names.add(field.name());
      }
    }

    final List<Expr> args = new ArrayList<>();
    for (final RecordComponentElement component : type.getRecordComponents()) {
      final Expr read = Violations.VALUE.invoke(component.getSimpleName().toString());
      args.add(names.contains(component.getSimpleName().toString()) ? Exprs.invokeStatic(Html.class, "escape", read) : read);
    }

    return args;
  }

  private Map<String, String> patterns(final List<ValidatedField> fields) {
    final Map<String, String> patterns = new LinkedHashMap<>();
    int index = 0;
    for (final ValidatedField field : fields) {
      final Pattern pattern = field.element().getAnnotation(Pattern.class);
      if (pattern != null) {
        patterns.put("PATTERN_" + index, pattern.regexp());
      }

      if (field.element().getAnnotation(Email.class) != null) {
        patterns.put("EMAIL_" + index, TextChecks.EMAIL_REGEX);
      }

      index++;
    }

    return patterns;
  }

  private boolean hasEscape(final List<ValidatedField> fields) {
    for (final ValidatedField field : fields) {
      if (field.element().getAnnotation(EscapeHtml.class) != null) {
        return true;
      }
    }
    return false;
  }

  private Expr err() {
    return Exprs.invokeStatic(Result.class, "err", Exprs.invokeStatic(List.class, "copyOf", Violations.LIST));
  }
}
