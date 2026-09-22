package de.yyuh.iridium.codegen.processor;

import com.io7m.jodist.AnnotationSpec;
import com.io7m.jodist.ClassName;
import com.io7m.jodist.CodeBlock;
import com.io7m.jodist.JavaFile;
import com.io7m.jodist.MethodSpec;
import com.io7m.jodist.ParameterizedTypeName;
import com.io7m.jodist.TypeName;
import com.io7m.jodist.TypeSpec;
import de.yyuh.iridium.codegen.annotation.Generated;
import de.yyuh.iridium.core.html.Html;
import de.yyuh.iridium.core.result.Result;
import de.yyuh.iridium.core.validation.annotation.AssertFalse;
import de.yyuh.iridium.core.validation.annotation.AssertTrue;
import de.yyuh.iridium.core.validation.ConstraintViolation;
import de.yyuh.iridium.core.validation.annotation.Digits;
import de.yyuh.iridium.core.validation.annotation.Email;
import de.yyuh.iridium.core.validation.annotation.EscapeHtml;
import de.yyuh.iridium.core.validation.Escaper;
import de.yyuh.iridium.core.validation.annotation.Future;
import de.yyuh.iridium.core.validation.annotation.Max;
import de.yyuh.iridium.core.validation.annotation.Min;
import de.yyuh.iridium.core.validation.annotation.Negative;
import de.yyuh.iridium.core.validation.annotation.NegativeOrZero;
import de.yyuh.iridium.core.validation.annotation.NotBlank;
import de.yyuh.iridium.core.validation.annotation.NotEmpty;
import de.yyuh.iridium.core.validation.annotation.NotNull;
import de.yyuh.iridium.core.validation.annotation.Past;
import de.yyuh.iridium.core.validation.annotation.Pattern;
import de.yyuh.iridium.core.validation.annotation.Positive;
import de.yyuh.iridium.core.validation.annotation.PositiveOrZero;
import de.yyuh.iridium.core.validation.annotation.Size;
import de.yyuh.iridium.core.validation.ValidationRegistrar;
import de.yyuh.iridium.core.validation.Validator;
import de.yyuh.iridium.core.validation.ValidatorRegistry;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
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
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("*")
public final class ValidationProcessor extends AbstractProcessor {

    private static final String GENERATED_PACKAGE = "de.yyuh.iridium.gen";
    private static final String REGISTRAR_CLASS = "ValidationRegistrarGenerated";
    private static final String AUTHOR = "Iridium";
    private static final String EMAIL_REGEX = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private Elements elements;
    private Types types;

    private final Set<TypeElement> validatedTypes = new LinkedHashSet<>();
    private boolean registrarGenerated = false;

    @Override
    public synchronized void init(final javax.annotation.processing.ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            generateRegistrar();
            return false;
        }

        for (final Element root : roundEnv.getRootElements()) {
            if (root.getKind() != ElementKind.CLASS && root.getKind() != ElementKind.RECORD) {
                continue;
            }
            final TypeElement type = (TypeElement) root;
            final List<FieldInfo> fields = collectFields(type);
            if (fields.isEmpty()) {
                continue;
            }
            generateValidator(type, fields);
            validatedTypes.add(type);
        }

        return false;
    }

    private record FieldInfo(String name, TypeMirror type, Element element, String accessor) {
    }

    private List<FieldInfo> collectFields(final TypeElement type) {
        final List<FieldInfo> result = new ArrayList<>();
        final boolean record = type.getKind() == ElementKind.RECORD;
        for (final Element enclosed : type.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.FIELD) {
                continue;
            }
            if (!hasConstraint(enclosed)) {
                continue;
            }
            final String name = enclosed.getSimpleName().toString();
            final String accessor = record ? "value." + name + "()" : accessor(type, name);
            result.add(new FieldInfo(name, enclosed.asType(), enclosed, accessor));
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

    private boolean hasConstraint(final Element element) {
        return element.getAnnotation(NotNull.class) != null
                || element.getAnnotation(NotBlank.class) != null
                || element.getAnnotation(NotEmpty.class) != null
                || element.getAnnotation(Size.class) != null
                || element.getAnnotation(Min.class) != null
                || element.getAnnotation(Max.class) != null
                || element.getAnnotation(Positive.class) != null
                || element.getAnnotation(Negative.class) != null
                || element.getAnnotation(PositiveOrZero.class) != null
                || element.getAnnotation(NegativeOrZero.class) != null
                || element.getAnnotation(Digits.class) != null
                || element.getAnnotation(Pattern.class) != null
                || element.getAnnotation(Email.class) != null
                || element.getAnnotation(Past.class) != null
                || element.getAnnotation(Future.class) != null
                || element.getAnnotation(AssertTrue.class) != null
                || element.getAnnotation(AssertFalse.class) != null
                || element.getAnnotation(EscapeHtml.class) != null;
    }

    private void generateValidator(final TypeElement type, final List<FieldInfo> fields) {
        final String pkg = elements.getPackageOf(type).getQualifiedName().toString();
        final String validatorName = type.getSimpleName() + "Validator";
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
                .addStatement("if (value == null) { violations.add(new $T($S, $S, null)); return $T.err($T.copyOf(violations)); }",
                        ConstraintViolation.class, "", "must not be null", Result.class, List.class);

        for (final FieldInfo field : fields) {
            generateChecks(validate, field);
        }

        validate.addStatement("if (violations.isEmpty()) { return $T.ok(value); }", Result.class)
                .addStatement("return $T.err($T.copyOf(violations))", Result.class, List.class);

        final TypeSpec.Builder builder = TypeSpec.classBuilder(validatorName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(Validator.class), typeName))
                .addAnnotation(generatedAnnotation())
                .addMethod(validate.build());

        if (hasEscape(fields) && type.getKind() == ElementKind.RECORD) {
            builder.addSuperinterface(ParameterizedTypeName.get(ClassName.get(Escaper.class), typeName))
                    .addMethod(escapeMethod(type, typeName, escapeFieldNames(fields)));
        }

        final JavaFile javaFile = JavaFile.builder(pkg, builder.build()).build();
        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (final IOException e) {
            throw new RuntimeException("Failed to generate " + validatorName, e);
        }
    }

    private Set<String> escapeFieldNames(final List<FieldInfo> fields) {
        final Set<String> names = new LinkedHashSet<>();
        for (final FieldInfo field : fields) {
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

    private void generateChecks(final MethodSpec.Builder b, final FieldInfo field) {
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
            b.addStatement("if ($L != null) { int size = $L; if (size < $L || size > $L) { violations.add(new $T($S, $S, $L)); } }",
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
            final String message = "numeric value out of bounds (<" + digits.integer() + " digits>.<" + digits.fraction() + " digits>)";
            if (primitive) {
                b.addStatement("{ $T d = new $T($T.valueOf($L)); if (d.precision() - d.scale() > $L || d.scale() > $L) { violations.add(new $T($S, $S, $L)); } }",
                        java.math.BigDecimal.class, java.math.BigDecimal.class, String.class, acc,
                        digits.integer(), digits.fraction(), ConstraintViolation.class, name, message, acc);
            } else {
                b.addStatement("if ($L != null) { $T d = new $T($T.valueOf($L)); if (d.precision() - d.scale() > $L || d.scale() > $L) { violations.add(new $T($S, $S, $L)); } }",
                        acc, java.math.BigDecimal.class, java.math.BigDecimal.class, String.class, acc,
                        digits.integer(), digits.fraction(), ConstraintViolation.class, name, message, acc);
            }
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

    private void addNumericBound(final MethodSpec.Builder b, final FieldInfo field, final String acc,
                                 final String name, final String message, final String comparator, final long bound) {
        if (isBigDecimal(field.type())) {
            b.addStatement("if ($L != null && $L.compareTo($T.valueOf($L)) $L 0) { violations.add(new $T($S, $S, $L)); }",
                    acc, acc, java.math.BigDecimal.class, bound, comparator, ConstraintViolation.class, name, message, acc);
        } else if (isBigInteger(field.type())) {
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

    private void addSignCheck(final MethodSpec.Builder b, final FieldInfo field, final String acc,
                              final String name, final String message, final String check) {
        if (isBigDecimal(field.type()) || isBigInteger(field.type())) {
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

    private void addTemporalCheck(final MethodSpec.Builder b, final FieldInfo field, final String acc,
                                  final String name, final boolean past) {
        final String typeString = field.type().toString();
        if (typeString.equals("java.util.Date")) {
            final String method = past ? "before" : "after";
            b.addStatement("if ($L != null && !$L.$L(new $T())) { violations.add(new $T($S, $S, $L)); }",
                    acc, acc, method, java.util.Date.class, ConstraintViolation.class, name,
                    past ? "must be in the past" : "must be in the future", acc);
        } else {
            final ClassName temporalType = ClassName.get((TypeElement) types.asElement(field.type()));
            final String method = past ? "isBefore" : "isAfter";
            b.addStatement("if ($L != null && !$L.$L($T.now())) { violations.add(new $T($S, $S, $L)); }",
                    acc, acc, method, temporalType, ConstraintViolation.class, name,
                    past ? "must be in the past" : "must be in the future", acc);
        }
    }

    private String sizeExpression(final FieldInfo field) {
        final String acc = field.accessor();
        if (field.type().getKind() == TypeKind.ARRAY) {
            return acc + ".length";
        }
        if (isAssignable(field.type(), "java.lang.CharSequence")) {
            return acc + ".length()";
        }
        return acc + ".size()";
    }

    private String emptyExpression(final FieldInfo field) {
        final String acc = field.accessor();
        if (field.type().getKind() == TypeKind.ARRAY) {
            return acc + ".length == 0";
        }
        return acc + ".isEmpty()";
    }

    private boolean hasEscape(final List<FieldInfo> fields) {
        for (final FieldInfo field : fields) {
            if (field.element().getAnnotation(EscapeHtml.class) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean isBigDecimal(final TypeMirror type) {
        return isSameType(type, "java.math.BigDecimal");
    }

    private boolean isBigInteger(final TypeMirror type) {
        return isSameType(type, "java.math.BigInteger");
    }

    private boolean isAssignable(final TypeMirror type, final String fqcn) {
        final TypeElement target = elements.getTypeElement(fqcn);
        return target != null && types.isAssignable(type, target.asType());
    }

    private boolean isSameType(final TypeMirror type, final String fqcn) {
        final TypeElement target = elements.getTypeElement(fqcn);
        return target != null && types.isSameType(type, target.asType());
    }

    private AnnotationSpec generatedAnnotation() {
        return AnnotationSpec.builder(ClassName.get(Generated.class))
                .addMember("author", "$S", AUTHOR)
                .addMember("date", "$S", Instant.now().toString())
                .build();
    }

    private void generateRegistrar() {
        if (validatedTypes.isEmpty() || registrarGenerated) {
            return;
        }
        registrarGenerated = true;

        final ClassName registry = ClassName.get(ValidatorRegistry.class);
        final MethodSpec.Builder register = MethodSpec.methodBuilder("register")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(registry, "registry");

        for (final TypeElement type : validatedTypes) {
            final String pkg = elements.getPackageOf(type).getQualifiedName().toString();
            final ClassName typeName = ClassName.get(type);
            final ClassName validatorClass = ClassName.get(pkg, type.getSimpleName() + "Validator");
            register.addStatement("registry.register($T.class, new $T())", typeName, validatorClass);
        }

        final TypeSpec typeSpec = TypeSpec.classBuilder(REGISTRAR_CLASS)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(ClassName.get(ValidationRegistrar.class))
                .addAnnotation(generatedAnnotation())
                .addMethod(register.build())
                .build();

        final JavaFile javaFile = JavaFile.builder(GENERATED_PACKAGE, typeSpec).build();
        try {
            javaFile.writeTo(processingEnv.getFiler());
            writeServiceFile();
        } catch (final IOException e) {
            throw new RuntimeException("Failed to generate " + REGISTRAR_CLASS, e);
        }
    }

    private void writeServiceFile() throws IOException {
        final javax.annotation.processing.Filer filer = processingEnv.getFiler();
        final var serviceFile = filer.createResource(
                javax.tools.StandardLocation.CLASS_OUTPUT,
                "",
                "META-INF/services/" + ValidationRegistrar.class.getName()
        );

        try (final Writer writer = serviceFile.openWriter()) {
            writer.write(GENERATED_PACKAGE + "." + REGISTRAR_CLASS);
            writer.write(System.lineSeparator());
        }
    }
}
