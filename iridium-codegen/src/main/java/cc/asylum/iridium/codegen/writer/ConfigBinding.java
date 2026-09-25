package cc.asylum.iridium.codegen.writer;

import com.io7m.jodist.ClassName;
import com.io7m.jodist.CodeBlock;
import com.io7m.jodist.TypeName;
import cc.asylum.iridium.codegen.support.BindingContext;
import cc.asylum.iridium.codegen.support.Diagnostics;
import cc.asylum.iridium.codegen.support.ModelSupport;
import cc.asylum.iridium.codegen.support.TypeSupport;
import cc.asylum.iridium.config.Config;
import cc.asylum.iridium.config.ConfigError;
import cc.asylum.iridium.config.ConfigurationProperties;
import cc.asylum.iridium.config.Default;
import cc.asylum.iridium.config.Value;
import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.validation.annotation.Nullable;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Internal
public final class ConfigBinding {

  private static final ClassName CONFIG = ClassName.get(Config.class);
  private static final ClassName RESULT = ClassName.get(Result.class);
  private static final Pattern PLACEHOLDER = Pattern.compile("^\\$\\{([^}:]+)(?::(.*))?}$");

  private ConfigBinding() {
  }

  public static CodeBlock bindRoot(
      final BindingContext context,
      final TypeElement type
  ) {
    return new Walk(context).bindRoot(type);
  }

  public static CodeBlock bindValue(
      final BindingContext context,
      final VariableElement parameter
  ) {
    return new Walk(context).bindValue(parameter);
  }

  private static final class Walk {

    private final BindingContext context;
    private final Set<String> visiting = new LinkedHashSet<>();
    private final int[] names;
    private int depth;

    private Walk(final BindingContext context) {
      this(context, new int[1]);
    }

    private Walk(final BindingContext context, final int[] names) {
      this.context = context;
      this.names = names;
    }

    private Walk deeper() {
      final Walk child = new Walk(context, names);
      child.depth = depth + 1;
      child.visiting.addAll(visiting);
      return child;
    }

    private String nextName(final String property) {
      return property + names[0]++;
    }

    private CodeBlock bindRoot(final TypeElement type) {
      if (type.getModifiers().contains(Modifier.ABSTRACT) || !type.getTypeParameters().isEmpty()) {
        error(type, "configuration type must be a concrete, non-generic class or record");
        return null;
      }

      final String prefix = type.getAnnotation(ConfigurationProperties.class).value().trim();
      if (!validPrefix(prefix)) {
        error(type, "invalid configuration prefix '" + prefix + "'");
        return null;
      }

      final CodeBlock created = bindObject(type, Key.literal(prefix), true);
      return created == null ? null : CodeBlock.of("$L.unwrap()", created);
    }

    private CodeBlock bindValue(final VariableElement parameter) {
      final Value value = parameter.getAnnotation(Value.class);
      if (value == null) {
        return null;
      }

      final Matcher matcher = placeholder(parameter, value.value());
      if (matcher == null) {
        return null;
      }

      if (isObject(parameter.asType())) {
        error(parameter, "inject @ConfigurationProperties types as constructor parameters");
        return null;
      }

      final String fallback = matcher.group(2) != null
          ? matcher.group(2)
          : defaultOf(parameter, null);

      final Expr bound = read(
          parameter,
          parameter.asType(),
          Key.literal(matcher.group(1)),
          fallback,
          parameter.getAnnotation(Nullable.class) != null
      );
      if (bound == null) {
        return null;
      }
      return bound.result ? CodeBlock.of("$L.unwrap()", bound.code) : bound.code;
    }

    private CodeBlock bindObject(
        final TypeElement type,
        final Key prefix,
        final boolean assumePresent
    ) {
      final String qualified = type.getQualifiedName().toString();
      if (!visiting.add(qualified)) {
        error(type, "cyclic configuration type " + qualified);
        return null;
      }

      final ExecutableElement constructor = constructorOf(type);
      if (constructor == null) {
        error(type, "configuration type must have exactly one constructor");
        visiting.remove(qualified);
        return null;
      }

      if (!constructor.getModifiers().contains(Modifier.PUBLIC)) {
        error(constructor, "configuration constructor must be public");
        visiting.remove(qualified);
        return null;
      }

      final List<Piece> pieces = arguments(type, constructor, prefix);
      visiting.remove(qualified);
      if (pieces == null) {
        return null;
      }

      final CodeBlock creation = compose(type, pieces);
      if (assumePresent) {
        return creation;
      }

      return CodeBlock.of(
          "$T.present($L) ? $L : $T.<$T>missing($L)",
          CONFIG,
          prefix.code(),
          creation,
          CONFIG,
          TypeName.get(type.asType()),
          prefix.code()
      );
    }

    private List<Piece> arguments(
        final TypeElement type,
        final ExecutableElement constructor,
        final Key prefix
    ) {
      final List<? extends RecordComponentElement> components = type.getKind() == ElementKind.RECORD
          ? type.getRecordComponents()
          : List.of();
      final List<? extends VariableElement> parameters = constructor.getParameters();
      final List<Piece> pieces = new ArrayList<>();

      for (int i = 0; i < parameters.size(); i++) {
        final VariableElement parameter = parameters.get(i);
        final RecordComponentElement component = i < components.size() ? components.get(i) : null;
        final Piece piece = argument(parameter, component, prefix);
        if (piece == null) {
          return null;
        }
        pieces.add(piece);
      }
      return pieces;
    }

    private CodeBlock compose(
        final TypeElement type,
        final List<Piece> pieces
    ) {
      final CodeBlock.Builder call = CodeBlock.builder().add("new $T(", ClassName.get(type));
      for (int i = 0; i < pieces.size(); i++) {
        if (i > 0) {
          call.add(", ");
        }
        final Piece piece = pieces.get(i);
        call.add(piece.result ? CodeBlock.of("$L", piece.name) : piece.code);
      }
      call.add(")");

      final List<Piece> results = pieces.stream().filter(Piece::result).toList();
      if (results.isEmpty()) {
        return CodeBlock.of("$T.ok($L)", RESULT, call.build());
      }

      CodeBlock chain = call.build();
      for (int i = results.size() - 1; i >= 0; i--) {
        final Piece piece = results.get(i);
        final String op = i == results.size() - 1 ? "map" : "flatMap";
        chain = CodeBlock.builder()
            .add("$L\n", piece.code)
            .add("        .$L($L -> $L)", op, piece.name, chain)
            .build();
      }
      return chain;
    }

    private Piece argument(
        final VariableElement parameter,
        final RecordComponentElement component,
        final Key prefix
    ) {
      final String property = component == null
          ? parameter.getSimpleName().toString()
          : component.getSimpleName().toString();

      if (component == null && property.matches("arg\\d+")) {
        error(parameter, "constructor parameter names are missing; compile with -parameters");
        return null;
      }

      final Value value = annotation(parameter, component, Value.class);
      final Key key;
      final String fallback;

      if (value == null) {
        key = prefix.child(property);
        fallback = defaultOf(parameter, component);
      } else {
        final Matcher matcher = placeholder(parameter, value.value());
        if (matcher == null) {
          return null;
        }
        key = Key.literal(matcher.group(1));
        fallback = matcher.group(2) != null
            ? matcher.group(2)
            : defaultOf(parameter, component);
      }

      final Expr bound = read(
          parameter,
          parameter.asType(),
          key,
          fallback,
          annotation(parameter, component, Nullable.class) != null
      );
      if (bound == null) {
        return null;
      }
      return new Piece(bound.code, bound.result, nextName(property));
    }

    private Expr read(
        final Element element,
        final TypeMirror type,
        final Key key,
        final String fallback,
        final boolean nullable
    ) {
      if (nullable && type.getKind().isPrimitive()) {
        error(element, "@Nullable is not valid on a primitive configuration value");
        return null;
      }

      final TypeMirror optional = TypeSupport.optionalValueType(context.types, type).orElse(null);
      if (optional != null) {
        return optionalOf(element, optional, key, fallback);
      }

      final Scalar scalar = scalar(type);
      if (scalar != null) {
        final CodeBlock code = scalar.read(context, element, key, fallback, nullable);
        if (code == null) {
          return null;
        }
        return new Expr(code, !nullable);
      }

      if (!(type instanceof final DeclaredType declared)) {
        error(element, "unsupported configuration type " + type);
        return null;
      }

      final TypeElement declaredType = TypeSupport.asTypeElement(context.types, type).orElse(null);
      if (declaredType == null) {
        error(element, "unsupported configuration type " + type);
        return null;
      }

      final String qualified = declaredType.getQualifiedName().toString();
      if ("java.util.List".equals(qualified) || "java.util.Set".equals(qualified)) {
        return collection(element, declared, "java.util.Set".equals(qualified), key, fallback);
      }
      if ("java.util.Map".equals(qualified)) {
        final CodeBlock mapped = map(element, declared, key, fallback);
        return mapped == null ? null : new Expr(mapped, true);
      }
      if (fallback != null) {
        error(element, "@Default is not supported for nested configuration objects");
        return null;
      }

      return nested(declaredType, key, nullable);
    }

    private Expr optionalOf(
        final Element element,
        final TypeMirror inner,
        final Key key,
        final String fallback
    ) {
      final Expr value = read(element, inner, key, fallback, false);
      if (value == null) {
        return null;
      }

      final ClassName optional = ClassName.get(java.util.Optional.class);
      final CodeBlock innerValue = value.result ? CodeBlock.of("$L.unwrap()", value.code) : value.code;
      return new Expr(CodeBlock.of(
          "$T.has($L) ? $T.of($L) : $T.empty()",
          CONFIG,
          key.code(),
          optional,
          innerValue,
          optional
      ), false);
    }

    private Expr nested(
        final TypeElement type,
        final Key key,
        final boolean nullable
    ) {
      if (!nullable) {
        final CodeBlock creation = bindObject(type, key, false);
        return creation == null ? null : new Expr(creation, true);
      }

      final CodeBlock creation = bindObject(type, key, true);
      if (creation == null) {
        return null;
      }

      return new Expr(CodeBlock.of(
          "$T.present($L) ? $L.unwrap() : null",
          CONFIG,
          key.code(),
          creation
      ), false);
    }

    private Expr collection(
        final Element element,
        final DeclaredType declared,
        final boolean set,
        final Key key,
        final String fallback
    ) {
      if (fallback != null) {
        error(element, "@Default is not supported for collections");
        return null;
      }
      if (declared.getTypeArguments().size() != 1
          || declared.getTypeArguments().get(0).getKind() == TypeKind.WILDCARD) {
        error(element, "collection item type must be a concrete type argument");
        return null;
      }

      final CodeBlock items = collectionItems(element, declared.getTypeArguments().get(0), key);
      if (items == null) {
        return null;
      }
      if (!set) {
        return new Expr(items, true);
      }
      return new Expr(CodeBlock.of("$L.map($T::copyOf)", items, ClassName.get(Set.class)), true);
    }

    private CodeBlock collectionItems(
        final Element element,
        final TypeMirror item,
        final Key key
    ) {
      final Scalar scalar = scalar(item);
      if (scalar != null) {
        return CodeBlock.of("$T.list($L, $L)", CONFIG, key.code(), scalar.converter);
      }

      final TypeElement itemType = TypeSupport.asTypeElement(context.types, item).orElse(null);
      if (itemType == null || itemType.getKind() == ElementKind.ENUM) {
        error(element, "unsupported collection item type " + item);
        return null;
      }

      final String index = "i" + depth;
      final CodeBlock nested = deeper().bindObject(itemType, key.indexed(index), true);
      if (nested == null) {
        return null;
      }

      return CodeBlock.of("$T.indexed($L, $L -> $L)", CONFIG, key.code(), index, nested);
    }

    private CodeBlock map(
        final Element element,
        final DeclaredType declared,
        final Key key,
        final String fallback
    ) {
      if (fallback != null) {
        error(element, "@Default is not supported for maps");
        return null;
      }

      if (declared.getTypeArguments().size() != 2) {
        error(element, "raw maps are not supported");
        return null;
      }

      if (!same(declared.getTypeArguments().get(0), String.class)) {
        error(element, "map keys must be String");
        return null;
      }

      final Scalar scalar = scalar(declared.getTypeArguments().get(1));
      if (scalar == null) {
        error(element, "map values must be scalar");
        return null;
      }

      return CodeBlock.of("$T.map($L, $L)", CONFIG, key.code(), scalar.converter);
    }

    private boolean isObject(final TypeMirror type) {
      return scalar(type) == null
          && TypeSupport.optionalValueType(context.types, type).isEmpty()
          && type instanceof DeclaredType
          && !isCollection(type);
    }

    private boolean isCollection(final TypeMirror type) {
      final TypeElement element = TypeSupport.asTypeElement(context.types, type).orElse(null);
      if (element == null) {
        return false;
      }

      final String name = element.getQualifiedName().toString();
      return "java.util.List".equals(name)
          || "java.util.Set".equals(name)
          || "java.util.Map".equals(name);
    }

    private Scalar scalar(final TypeMirror type) {
      return switch (type.getKind()) {
        case BOOLEAN -> Scalar.simple("bool", CodeBlock.of("$T::parseBool", CONFIG));
        case INT -> Scalar.simple("integer", CodeBlock.of("$T::parseInt", CONFIG));
        case LONG -> Scalar.simple("longValue", CodeBlock.of("$T::parseLong", CONFIG));
        case DOUBLE -> Scalar.simple("doubleValue", CodeBlock.of("$T::parseDouble", CONFIG));
        case FLOAT -> Scalar.simple("floatValue", CodeBlock.of("$T::parseFloat", CONFIG));
        case SHORT -> Scalar.simple("shortValue", CodeBlock.of("$T::parseShort", CONFIG));
        case BYTE -> Scalar.simple("byteValue", CodeBlock.of("$T::parseByte", CONFIG));
        case DECLARED -> declaredScalar(type);
        default -> null;
      };
    }

    private Scalar declaredScalar(final TypeMirror type) {
      if (same(type, String.class)) {
        return Scalar.simple("string", CodeBlock.of("value -> $T.ok(value)", RESULT));
      }

      if (same(type, Boolean.class)) {
        return Scalar.simple("bool", CodeBlock.of("$T::parseBool", CONFIG));
      }

      if (same(type, Integer.class)) {
        return Scalar.simple("integer", CodeBlock.of("$T::parseInt", CONFIG));
      }

      if (same(type, Long.class)) {
        return Scalar.simple("longValue", CodeBlock.of("$T::parseLong", CONFIG));
      }

      if (same(type, Double.class)) {
        return Scalar.simple("doubleValue", CodeBlock.of("$T::parseDouble", CONFIG));
      }

      if (same(type, Float.class)) {
        return Scalar.simple("floatValue", CodeBlock.of("$T::parseFloat", CONFIG));
      }

      if (same(type, Short.class)) {
        return Scalar.simple("shortValue", CodeBlock.of("$T::parseShort", CONFIG));
      }

      if (same(type, Byte.class)) {
        return Scalar.simple("byteValue", CodeBlock.of("$T::parseByte", CONFIG));
      }

      final TypeElement element = TypeSupport.asTypeElement(context.types, type).orElse(null);
      if (element != null && element.getKind() == ElementKind.ENUM) {
        return Scalar.enumeration(element);
      }
      return null;
    }

    private boolean same(final TypeMirror type, final Class<?> target) {
      return TypeSupport.isSameType(context.types, context.elements, type, target);
    }

    private Matcher placeholder(final Element element, final String raw) {
      final Matcher matcher = PLACEHOLDER.matcher(raw.trim());
      if (!matcher.matches() || matcher.group(1).isBlank()) {
        error(element, "@Value must be a placeholder like ${key} or ${key:default}");
        return null;
      }
      return matcher;
    }

    private void error(final Element element, final String message) {
      Diagnostics.error(context.messager, element, message);
    }
  }

  private static String defaultOf(
      final VariableElement parameter,
      final RecordComponentElement component
  ) {
    final Default annotation = annotation(parameter, component, Default.class);
    return annotation == null ? null : annotation.value();
  }

  private static ExecutableElement constructorOf(final TypeElement type) {
    if (type.getKind() != ElementKind.RECORD) {
      return ModelSupport.resolveConstructor(type);
    }

    final int count = type.getRecordComponents().size();
    for (final Element enclosed : type.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.CONSTRUCTOR
          && ((ExecutableElement) enclosed).getParameters().size() == count) {
        return (ExecutableElement) enclosed;
      }
    }
    return null;
  }

  private static <A extends java.lang.annotation.Annotation> A annotation(
      final VariableElement parameter,
      final RecordComponentElement component,
      final Class<A> type
  ) {
    final A onParameter = parameter.getAnnotation(type);
    if (onParameter != null) {
      return onParameter;
    }
    return component == null ? null : component.getAnnotation(type);
  }

  private static boolean validPrefix(final String prefix) {
    if (prefix.isEmpty()) {
      return true;
    }
    if (prefix.startsWith(".") || prefix.endsWith(".") || prefix.contains("..")) {
      return false;
    }

    for (int i = 0; i < prefix.length(); i++) {
      final char c = prefix.charAt(i);
      if (!Character.isLetterOrDigit(c) && c != '.' && c != '-' && c != '_') {
        return false;
      }
    }
    return true;
  }

  private record Expr(CodeBlock code, boolean result) {
  }

  private record Piece(CodeBlock code, boolean result, String name) {
  }

  private record Key(String literal, String index, String suffix, CodeBlock expression) {

    private static Key literal(final String value) {
      return new Key(value, null, "", null);
    }

    private static Key expression(final CodeBlock expression) {
      return new Key(null, null, "", expression);
    }

    private Key child(final String name) {
      if (expression != null) {
        return expression(CodeBlock.of("$L + $S", expression, "." + name));
      }
      if (index == null) {
        return literal(literal.isEmpty() ? name : literal + "." + name);
      }
      if (suffix.isEmpty()) {
        return new Key(literal, index, name, null);
      }
      return new Key(literal, index, suffix + "." + name, null);
    }

    private Key indexed(final String indexName) {
      if (expression != null || index != null) {
        return expression(CodeBlock.of("$L + $S + $L + $S", code(), "[", indexName, "]"));
      }
      return new Key(literal, indexName, "", null);
    }

    private CodeBlock code() {
      if (expression != null) {
        return expression;
      }
      if (index == null) {
        return CodeBlock.of("$S", literal);
      }
      if (suffix.isEmpty()) {
        return CodeBlock.of("$S + $L + $S", literal + "[", index, "]");
      }
      return CodeBlock.of("$S + $L + $S", literal + "[", index, "]." + suffix);
    }
  }

  private record Scalar(String method, CodeBlock converter, TypeElement enumType) {

    private static Scalar simple(final String method, final CodeBlock converter) {
      return new Scalar(method, converter, null);
    }

    private static Scalar enumeration(final TypeElement enumType) {
      return new Scalar(
          "enumeration",
          CodeBlock.of("value -> $T.parseEnum(value, $T.class)", CONFIG, ClassName.get(enumType)),
          enumType
      );
    }

    private CodeBlock read(
        final BindingContext context,
        final Element element,
        final Key key,
        final String fallback,
        final boolean nullable
    ) {
      if (fallback != null) {
        return withFallback(context, element, key, fallback);
      }

      if (enumType != null) {
        return enumValue(key, nullable);
      }

      if (nullable && "string".equals(method)) {
        return CodeBlock.of("$T.find($L)", CONFIG, key.code());
      }

      if (nullable) {
        return CodeBlock.of(
            "$T.has($L) ? $T.$L($L).unwrap() : null",
            CONFIG,
            key.code(),
            CONFIG,
            method,
            key.code()
        );
      }
      return CodeBlock.of("$T.$L($L)", CONFIG, method, key.code());
    }

    private CodeBlock withFallback(
        final BindingContext context,
        final Element element,
        final Key key,
        final String fallback
    ) {
      final CodeBlock literal = literal(context, element, fallback);
      if (literal == null) {
        return null;
      }
      if (enumType != null) {
        return CodeBlock.of(
            "$T.enumeration($L, $T.class, $L)",
            CONFIG,
            key.code(),
            ClassName.get(enumType),
            literal
        );
      }
      return CodeBlock.of("$T.$L($L, $L)", CONFIG, method, key.code(), literal);
    }

    private CodeBlock enumValue(final Key key, final boolean nullable) {
      final CodeBlock value = CodeBlock.of(
          "$T.enumeration($L, $T.class)",
          CONFIG,
          key.code(),
          ClassName.get(enumType)
      );
      if (!nullable) {
        return value;
      }
      return CodeBlock.of("$T.has($L) ? $L.unwrap() : null", CONFIG, key.code(), value);
    }

    private CodeBlock literal(
        final BindingContext context,
        final Element element,
        final String fallback
    ) {
      try {
        if (enumType != null) {
          return enumLiteral(context, element, fallback);
        }

        return switch (method) {
          case "string" -> CodeBlock.of("$S", fallback);
          case "bool" -> boolLiteral(context, element, fallback);
          case "integer" -> CodeBlock.of("$L", Integer.parseInt(fallback.trim()));
          case "longValue" -> CodeBlock.of("$LL", Long.parseLong(fallback.trim()));
          case "doubleValue" -> CodeBlock.of("$L", Double.parseDouble(fallback.trim()));
          case "floatValue" -> CodeBlock.of("$Lf", Float.parseFloat(fallback.trim()));
          case "shortValue" -> CodeBlock.of("(short) $L", Short.parseShort(fallback.trim()));
          case "byteValue" -> CodeBlock.of("(byte) $L", Byte.parseByte(fallback.trim()));
          default -> {
            Diagnostics.error(context.messager, element, "invalid default '" + fallback + "'");
            yield null;
          }
        };
      } catch (final RuntimeException exception) {
        Diagnostics.error(context.messager, element, "invalid default '" + fallback + "'");
        return null;
      }
    }

    private CodeBlock boolLiteral(
        final BindingContext context,
        final Element element,
        final String fallback
    ) {
      final Result<Boolean, ConfigError> parsed = Config.parseBool(fallback);
      if (parsed.isErr()) {
        Diagnostics.error(context.messager, element, "invalid default '" + fallback + "'");
        return null;
      }
      return CodeBlock.of("$L", parsed.unwrap());
    }

    private CodeBlock enumLiteral(
        final BindingContext context,
        final Element element,
        final String fallback
    ) {
      for (final Element enclosed : enumType.getEnclosedElements()) {
        if (enclosed.getKind() == ElementKind.ENUM_CONSTANT
            && enclosed.getSimpleName().contentEquals(fallback)) {
          return CodeBlock.of("$T.$L", ClassName.get(enumType), fallback);
        }
      }

      Diagnostics.error(context.messager, element, "invalid default '" + fallback + "'");
      return null;
    }
  }
}
