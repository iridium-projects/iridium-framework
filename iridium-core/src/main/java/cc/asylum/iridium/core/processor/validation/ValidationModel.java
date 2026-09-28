package cc.asylum.iridium.core.processor.validation;

import cc.asylum.iridium.codegen.model.Elements;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.codegen.write.Registrar;
import cc.asylum.iridium.codegen.code.Types;
import cc.asylum.iridium.core.validation.ValidationRegistrar;
import cc.asylum.iridium.core.validation.ValidatorRegistry;
import cc.asylum.iridium.core.validation.annotation.AssertFalse;
import cc.asylum.iridium.core.validation.annotation.AssertTrue;
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

import javax.annotation.processing.Filer;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ValidationModel {

  private static final String REGISTRAR = "ValidationRegistrarGenerated";

  private final Set<TypeElement> validated = new LinkedHashSet<>();
  private String pkg;
  private boolean written;

  public static Set<String> annotations() {
    return Stream.of(
        AssertFalse.class, AssertTrue.class, Digits.class, Email.class, EscapeHtml.class,
        Future.class, Max.class, Min.class, Negative.class, NegativeOrZero.class,
        NotBlank.class, NotEmpty.class, NotNull.class, Past.class, Pattern.class,
        Positive.class, PositiveOrZero.class, Size.class)
      .map(Class::getCanonicalName)
      .collect(Collectors.toUnmodifiableSet());
  }

  public void round(final Processing processing) {
    if (pkg == null) {
      pkg = Elements.generatedPackage(
        processing.elements(),
        Elements.rootTypes(processing.round(), ElementKind.CLASS, ElementKind.RECORD));
    }

    final var writer = new ValidatorWriter(processing.types(), processing.elements(), processing.messager());
    for (final TypeElement type : Elements.rootTypes(processing.round(), ElementKind.CLASS, ElementKind.RECORD)) {
      if (writer.write(processing.filer(), pkg, type, type.getSimpleName() + "Validator")) {
        validated.add(type);
      }
    }
  }

  public void finish(final Filer filer) {
    if (validated.isEmpty() || written) {
      return;
    }

    written = true;
    final Registrar registrar = Registrar.of(REGISTRAR, ValidationRegistrar.class, ValidatorRegistry.class, "registry");
    for (final TypeElement type : validated) {
      registrar.line(registrar.pool().invoke(
        "register",
        Exprs.classLit(type),
        Exprs.new_(Types.of(pkg, type.getSimpleName() + "Validator"))));
    }

    registrar.write(filer, pkg, validated.toArray(new TypeElement[0]));
  }
}
