package cc.asylum.iridium.codegen.writer;

import com.io7m.jodist.ClassName;
import com.io7m.jodist.CodeBlock;
import com.io7m.jodist.MethodSpec;
import com.io7m.jodist.ParameterizedTypeName;
import com.io7m.jodist.TypeName;
import com.io7m.jodist.TypeSpec;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.html.Html;
import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.validation.ConstraintViolation;
import cc.asylum.iridium.core.validation.Escaper;
import cc.asylum.iridium.core.validation.Validator;
import cc.asylum.iridium.core.validation.annotation.AssertFalse;
import cc.asylum.iridium.core.validation.annotation.AssertTrue;
import cc.asylum.iridium.core.validation.annotation.Constraint;
import cc.asylum.iridium.core.validation.annotation.Digits;
import cc.asylum.iridium.core.validation.annotation.Email;
import cc.asylum.iridium.core.validation.annotation.EscapeHtml;
import cc.asylum.iridium.core.validation.annotation.Future;
import cc.asylum.iridium.core.validation.annotation.Max;
import cc.asylum.iridium.core.validation.annotation.Min;
import cc.asylum.iridium.core.validation.annotation.Negative;
import cc.asylum.iridium.core.validation.annotation.NegativeOrZero;
import cc.asylum.iridium.core.validation.annotation.NotBlank;
import cc.asylum.iridium.core.validation.annotation.NotEmpty;
import cc.asylum.iridium.core.validation.annotation.NotNull;
import cc.asylum.iridium.core.validation.annotation.Past;
import cc.asylum.iridium.core.validation.annotation.Pattern;
import cc.asylum.iridium.core.validation.annotation.Positive;
import cc.asylum.iridium.core.validation.annotation.PositiveOrZero;
import cc.asylum.iridium.core.validation.annotation.Size;
import cc.asylum.iridium.codegen.support.Diagnostics;
import cc.asylum.iridium.codegen.support.MirrorSupport;
import cc.asylum.iridium.codegen.support.SourceWriter;
import cc.asylum.iridium.codegen.support.TypeSupport;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Internal
public final class ValidatorWriter {

  private static final String EMAIL_REGEX = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

  private final Types types;
  private final Elements elements;
  private final Messager messager;

  public ValidatorWriter(final Types types, final Elements elements, final Messager messager) {
    this.types = types;
    this.elements = elements;
    this.messager = messager;
  }

  public record ValidatedField(String name, TypeMirror type, Element element, String accessor) {
  }

  public Optional<TypeSpec> validatorFor(final TypeElement type, final String name) {
    final List<ValidatedField> fields = collectFields(type);
    if (fields.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(generateValidator(type, fields, SourceWriter.generatedType(name)));
  }

  private List<ValidatedField> collectFields(final TypeElement type) {
    final List<ValidatedField> result = new ArrayList<>();
    final boolean record = type.getKind() == ElementKind.RECORD;
    for (final Element enclosed : type.getEnclosedElements()) {
      if (enclosed.getKind() != ElementKind.FIELD || !MirrorSupport.hasMeta(enclosed, Constraint.class)) {
        continue;
      }
      final String name = enclosed.getSimpleName().toString();
      final String accessor = record ? "value." + name + "()" : accessor(type, name);
      result.add(new ValidatedField(name, enclosed.asType(), enclosed, accessor));
    }
    return result;
  }

  private String accessor(final TypeElement owner, final String name) {
    final String cap = Character.toUpperCase(name.charAt(0)) + name.substring(1);
    for (final Element enclosed : owner.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.METHOD
          && enclosed.getSimpleName().contentEquals("get" + cap)
          && ((ExecutableElement) enclosed).getParameters().isEmpty()) {
        return "value.get" + cap + "()";
      }
    }
    for (final Element enclosed : owner.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.METHOD
          && enclosed.getSimpleName().contentEquals("is" + cap)
          && ((ExecutableElement) enclosed).getParameters().isEmpty()) {
        return "value.is" + cap + "()";
      }
    }
    return "value." + name;
  }

  private TypeSpec generateValidator(final TypeElement type, final List<ValidatedField> fields,
      final TypeSpec.Builder template) {
    final TypeName typeName = ClassName.get(type);
    final TypeName violationList = ParameterizedTypeName.get(
        ClassName.get(List.class), ClassName.get(ConstraintViolation.class));
    final TypeName resultType = ParameterizedTypeName.get(ClassName.get(Result.class), typeName, violationList);

    final MethodSpec.Builder validate = MethodSpec.methodBuilder("validate")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(typeName, "value")
        .returns(resultType)
        .addStatement("$T<$T> violations = new $T<>()", List.class, ConstraintViolation.class, ArrayList.class)
        .addStatement(
            "if (value == null) { violations.add(new $T($S, $S, null)); return $T.err($T.copyOf(violations)); }",
            ConstraintViolation.class, "", "must not be null", Result.class, List.class);

    for (final ValidatedField field : fields) {
      generateChecks(validate, field);
    }

    validate.addStatement("if (violations.isEmpty()) { return $T.ok(value); }", Result.class)
        .addStatement("return $T.err($T.copyOf(violations))", Result.class, List.class);

    template
        .addSuperinterface(ParameterizedTypeName.get(ClassName.get(Validator.class), typeName))
        .addMethod(validate.build());

    if (hasEscape(fields) && type.getKind() == ElementKind.RECORD) {
      template.addSuperinterface(ParameterizedTypeName.get(ClassName.get(Escaper.class), typeName))
          .addMethod(escapeMethod(type, typeName, escapeFieldNames(fields)));
    }

    return template.build();
  }

  private Set<String> escapeFieldNames(final List<ValidatedField> fields) {
    final Set<String> names = new LinkedHashSet<>();
    for (final ValidatedField field : fields) {
      if (field.element().getAnnotation(EscapeHtml.class) != null) {
        names.add(field.name());
      }
    }
    return names;
  }

  private MethodSpec escapeMethod(final TypeElement type, final TypeName typeName, final Set<String> escapeFields) {
    final CodeBlock.Builder call = CodeBlock.builder().add("return new $T(", typeName);
    final List<? extends RecordComponentElement> components = type.getRecordComponents();
    for (int i = 0; i < components.size(); i++) {
      final RecordComponentElement component = components.get(i);
      if (i > 0) {
        call.add(", ");
      }
      if (escapeFields.contains(component.getSimpleName().toString())) {
        call.add("$T.escape(value.$N())", Html.class, component.getSimpleName());
      } else {
        call.add("value.$N()", component.getSimpleName());
      }
    }
    call.add(")");
    return MethodSpec.methodBuilder("escape")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(typeName, "value")
        .returns(typeName)
        .addStatement(call.build())
        .build();
  }

  private void generateChecks(final MethodSpec.Builder b, final ValidatedField field) {
    final Element element = field.element();
    final String acc = field.accessor();
    final String name = field.name();
    final boolean primitive = field.type().getKind().isPrimitive();

    if (!primitive && element.getAnnotation(NotNull.class) != null) {
      b.addStatement("if ($L == null) { violations.add(new $T($S, $S, null)); }",
          acc, ConstraintViolation.class, name, "must not be null");
    }

    if (element.getAnnotation(NotBlank.class) != null) {
      b.addStatement("if ($L == null || $L.isBlank()) { violations.add(new $T($S, $S, $L)); }",
          acc, acc, ConstraintViolation.class, name, "must not be blank", acc);
    }

    if (element.getAnnotation(NotEmpty.class) != null) {
      b.addStatement("if ($L == null || $L) { violations.add(new $T($S, $S, $L)); }",
          acc, emptyExpression(field), ConstraintViolation.class, name, "must not be empty", acc);
    }

    final Size size = element.getAnnotation(Size.class);
    if (size != null) {
      b.addStatement(
          "if ($L != null) { int size = $L; if (size < $L || size > $L) { violations.add(new $T($S, $S, $L)); } }",
          acc, sizeExpression(field), size.min(), size.max(), ConstraintViolation.class, name,
          "size must be between " + size.min() + " and " + size.max(), acc);
    }

    final Min min = element.getAnnotation(Min.class);
    if (min != null) {
      addNumericBound(b, field, acc, name, "must be >= " + min.value(), "<", min.value());
    }

    final Max max = element.getAnnotation(Max.class);
    if (max != null) {
      addNumericBound(b, field, acc, name, "must be <= " + max.value(), ">", max.value());
    }

    if (element.getAnnotation(Positive.class) != null) {
      addSignCheck(b, field, acc, name, "must be positive", "<= 0");
    }
    if (element.getAnnotation(Negative.class) != null) {
      addSignCheck(b, field, acc, name, "must be negative", ">= 0");
    }
    if (element.getAnnotation(PositiveOrZero.class) != null) {
      addSignCheck(b, field, acc, name, "must be positive or zero", "< 0");
    }
    if (element.getAnnotation(NegativeOrZero.class) != null) {
      addSignCheck(b, field, acc, name, "must be negative or zero", "> 0");
    }

    final Digits digits = element.getAnnotation(Digits.class);
    if (digits != null) {
      final String message = "numeric value out of bounds (<" + digits.integer() + " digits>.<" + digits.fraction()
          + " digits>)";
      addDigitsCheck(b, field, acc, name, message, digits, primitive);
    }

    final Pattern pattern = element.getAnnotation(Pattern.class);
    if (pattern != null) {
      b.addStatement("if ($L != null && !$L.matches($S)) { violations.add(new $T($S, $S, $L)); }",
          acc, acc, pattern.regexp(), ConstraintViolation.class, name, "must match " + pattern.regexp(), acc);
    }

    if (element.getAnnotation(Email.class) != null) {
      b.addStatement("if ($L != null && !$L.matches($S)) { violations.add(new $T($S, $S, $L)); }",
          acc, acc, EMAIL_REGEX, ConstraintViolation.class, name, "must be a valid email address", acc);
    }

    if (element.getAnnotation(Past.class) != null) {
      addTemporalCheck(b, field, acc, name, true);
    }
    if (element.getAnnotation(Future.class) != null) {
      addTemporalCheck(b, field, acc, name, false);
    }

    if (element.getAnnotation(AssertTrue.class) != null) {
      if (primitive) {
        b.addStatement("if (!$L) { violations.add(new $T($S, $S, $L)); }",
            acc, ConstraintViolation.class, name, "must be true", acc);
      } else {
        b.addStatement("if ($L == null || !$L) { violations.add(new $T($S, $S, $L)); }",
            acc, acc, ConstraintViolation.class, name, "must be true", acc);
      }
    }

    if (element.getAnnotation(AssertFalse.class) != null) {
      if (primitive) {
        b.addStatement("if ($L) { violations.add(new $T($S, $S, $L)); }",
            acc, ConstraintViolation.class, name, "must be false", acc);
      } else {
        b.addStatement("if ($L == null || $L) { violations.add(new $T($S, $S, $L)); }",
            acc, acc, ConstraintViolation.class, name, "must be false", acc);
      }
    }
  }

  private void addDigitsCheck(final MethodSpec.Builder b, final ValidatedField field, final String acc,
      final String name, final String message, final Digits digits, final boolean primitive) {
    if (primitive) {
      b.addStatement(
          "{ $T d = new $T($T.valueOf($L)); if (d.precision() - d.scale() > $L || d.scale() > $L)"
              + " { violations.add(new $T($S, $S, $L)); } }",
          java.math.BigDecimal.class, java.math.BigDecimal.class, String.class, acc,
          digits.integer(), digits.fraction(), ConstraintViolation.class, name, message, acc);
    } else {
      b.addStatement(
          "if ($L != null) { $T d = new $T($T.valueOf($L)); if (d.precision() - d.scale() > $L || d.scale() > $L)"
              + " { violations.add(new $T($S, $S, $L)); } }",
          acc, java.math.BigDecimal.class, java.math.BigDecimal.class, String.class, acc,
          digits.integer(), digits.fraction(), ConstraintViolation.class, name, message, acc);
    }
  }

  private void addNumericBound(final MethodSpec.Builder b, final ValidatedField field, final String acc,
      final String name, final String message, final String comparator, final long bound) {
    if (TypeSupport.isSameType(types, elements, field.type(), java.math.BigDecimal.class)) {
      b.addStatement("if ($L != null && $L.compareTo($T.valueOf($L)) $L 0) { violations.add(new $T($S, $S, $L)); }",
          acc, acc, java.math.BigDecimal.class, bound, comparator, ConstraintViolation.class, name, message, acc);
    } else if (TypeSupport.isSameType(types, elements, field.type(), java.math.BigInteger.class)) {
      b.addStatement("if ($L != null && $L.compareTo($T.valueOf($L)) $L 0) { violations.add(new $T($S, $S, $L)); }",
          acc, acc, java.math.BigInteger.class, bound, comparator, ConstraintViolation.class, name, message, acc);
    } else if (field.type().getKind().isPrimitive()) {
      b.addStatement("if ($L $L $L) { violations.add(new $T($S, $S, $L)); }",
          acc, comparator, bound, ConstraintViolation.class, name, message, acc);
    } else {
      b.addStatement("if ($L != null && $L $L $L) { violations.add(new $T($S, $S, $L)); }",
          acc, acc, comparator, bound, ConstraintViolation.class, name, message, acc);
    }
  }

  private void addSignCheck(final MethodSpec.Builder b, final ValidatedField field, final String acc,
      final String name, final String message, final String check) {
    if (TypeSupport.isSameType(types, elements, field.type(), java.math.BigDecimal.class)
        || TypeSupport.isSameType(types, elements, field.type(), java.math.BigInteger.class)) {
      b.addStatement("if ($L != null && $L.signum() $L) { violations.add(new $T($S, $S, $L)); }",
          acc, acc, check, ConstraintViolation.class, name, message, acc);
    } else if (field.type().getKind().isPrimitive()) {
      b.addStatement("if ($L $L) { violations.add(new $T($S, $S, $L)); }",
          acc, check, ConstraintViolation.class, name, message, acc);
    } else {
      b.addStatement("if ($L != null && $L $L) { violations.add(new $T($S, $S, $L)); }",
          acc, acc, check, ConstraintViolation.class, name, message, acc);
    }
  }

  private void addTemporalCheck(final MethodSpec.Builder b, final ValidatedField field, final String acc,
      final String name, final boolean past) {
    if (TypeSupport.isSameType(types, elements, field.type(), java.util.Date.class)) {
      b.addStatement("if ($L != null && !$L.$L(new $T())) { violations.add(new $T($S, $S, $L)); }",
          acc, acc, past ? "before" : "after", java.util.Date.class, ConstraintViolation.class, name,
          past ? "must be in the past" : "must be in the future", acc);
      return;
    }
    final Optional<TypeElement> temporal = TypeSupport.asTypeElement(types, field.type());
    if (temporal.isEmpty()) {
      Diagnostics.error(messager, field.element(),
          "@Past/@Future requires a java.util.Date or a java.time type, but found " + field.type());
      return;
    }
    final ClassName temporalType = ClassName.get(temporal.get());
    b.addStatement("if ($L != null && !$L.$L($T.now())) { violations.add(new $T($S, $S, $L)); }",
        acc, acc, past ? "isBefore" : "isAfter", temporalType, ConstraintViolation.class, name,
        past ? "must be in the past" : "must be in the future", acc);
  }

  private String sizeExpression(final ValidatedField field) {
    final String acc = field.accessor();
    if (field.type().getKind() == TypeKind.ARRAY) {
      return acc + ".length";
    }
    if (TypeSupport.isAssignable(types, elements, field.type(), CharSequence.class)) {
      return acc + ".length()";
    }
    return acc + ".size()";
  }

  private String emptyExpression(final ValidatedField field) {
    final String acc = field.accessor();
    if (field.type().getKind() == TypeKind.ARRAY) {
      return acc + ".length == 0";
    }
    return acc + ".isEmpty()";
  }

  private boolean hasEscape(final List<ValidatedField> fields) {
    for (final ValidatedField field : fields) {
      if (field.element().getAnnotation(EscapeHtml.class) != null) {
        return true;
      }
    }
    return false;
  }
}
