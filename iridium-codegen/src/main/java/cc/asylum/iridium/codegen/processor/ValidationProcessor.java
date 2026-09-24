package cc.asylum.iridium.codegen.processor;

import com.io7m.jodist.ClassName;
import com.io7m.jodist.MethodSpec;
import cc.asylum.iridium.codegen.IridiumProcessor;
import cc.asylum.iridium.codegen.support.ModelSupport;
import cc.asylum.iridium.codegen.support.SourceWriter;
import cc.asylum.iridium.codegen.writer.ValidatorWriter;
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

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ValidationProcessor extends IridiumProcessor {

  private static final String REGISTRAR_CLASS = "ValidationRegistrarGenerated";

  private final Set<TypeElement> validatedTypes = new LinkedHashSet<>();
  private String generatedPackage;
  private boolean registrarGenerated = false;

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Stream.of(
            AssertFalse.class,
            AssertTrue.class,
            Digits.class,
            Email.class,
            EscapeHtml.class,
            Future.class,
            Max.class,
            Min.class,
            Negative.class,
            NegativeOrZero.class,
            NotBlank.class,
            NotEmpty.class,
            NotNull.class,
            Past.class,
            Pattern.class,
            Positive.class,
            PositiveOrZero.class,
            Size.class)
        .map(Class::getCanonicalName)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  protected void processRound(final RoundEnvironment roundEnv) {
    if (generatedPackage == null) {
      generatedPackage = ModelSupport.generatedPackage(elements,
          ModelSupport.rootTypes(roundEnv, ElementKind.CLASS, ElementKind.RECORD));
    }
    final var writer = new ValidatorWriter(types, elements, messager);
    for (final TypeElement type : ModelSupport.rootTypes(roundEnv, ElementKind.CLASS, ElementKind.RECORD)) {
      writer.validatorFor(type, type.getSimpleName() + "Validator")
          .ifPresent(spec -> {
            SourceWriter.writeJava(filer, generatedPackage, spec, type);
            validatedTypes.add(type);
          });
    }
  }

  @Override
  protected void finish() {
    if (validatedTypes.isEmpty() || registrarGenerated) {
      return;
    }
    registrarGenerated = true;

    final MethodSpec.Builder register = MethodSpec.methodBuilder("register")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ClassName.get(ValidatorRegistry.class), "registry");

    for (final TypeElement type : validatedTypes) {
      register.addStatement(
          "registry.register($T.class, new $T())",
          ClassName.get(type),
          ClassName.get(generatedPackage, type.getSimpleName() + "Validator")
      );
    }

    SourceWriter.writeJava(filer, generatedPackage, SourceWriter.generatedType(REGISTRAR_CLASS)
        .addSuperinterface(ClassName.get(ValidationRegistrar.class))
        .addMethod(register.build())
        .build(), validatedTypes.toArray(new TypeElement[0]));
    SourceWriter.writeService(
        filer,
        ValidationRegistrar.class,
        generatedPackage + "." + REGISTRAR_CLASS,
        validatedTypes.toArray(new TypeElement[0])
    );
  }
}
