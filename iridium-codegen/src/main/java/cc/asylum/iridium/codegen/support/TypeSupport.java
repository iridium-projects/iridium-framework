package cc.asylum.iridium.codegen.support;

import cc.asylum.iridium.core.annotation.Internal;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Optional;

@Internal
public final class TypeSupport {

  private TypeSupport() {
  }

  public static boolean isAssignable(
      final Types types,
      final Elements elements,
      final TypeMirror type,
      final Class<?> target
  ) {
    return isAssignable(types, elements, type, target.getCanonicalName());
  }

  public static boolean isAssignable(
      final Types types,
      final Elements elements,
      final TypeMirror type,
      final String fqcn
  ) {
    final TypeElement target = elements.getTypeElement(fqcn);
    return target != null && types.isAssignable(type, target.asType());
  }

  public static boolean isSameType(
      final Types types,
      final Elements elements,
      final TypeMirror type,
      final Class<?> target
  ) {
    return isSameType(types, elements, type, target.getCanonicalName());
  }

  public static boolean isSameType(
      final Types types,
      final Elements elements,
      final TypeMirror type,
      final String fqcn
  ) {
    final TypeElement target = elements.getTypeElement(fqcn);
    return target != null && types.isSameType(type, target.asType());
  }

  public static Optional<TypeElement> asTypeElement(
      final Types types,
      final TypeMirror type
  ) {
    final Element element = types.asElement(type);
    return element instanceof final TypeElement typeElement
        ? Optional.of(typeElement)
        : Optional.empty();
  }

  public static String qualifiedName(
      final Types types,
      final TypeMirror type
  ) {
    return asTypeElement(types, type)
        .map(element -> element.getQualifiedName().toString())
        .orElse(type.toString());
  }

  public static String boxed(final String name) {
    return switch (name) {
      case "boolean" -> "java.lang.Boolean";
      case "byte" -> "java.lang.Byte";
      case "short" -> "java.lang.Short";
      case "int" -> "java.lang.Integer";
      case "long" -> "java.lang.Long";
      case "float" -> "java.lang.Float";
      case "double" -> "java.lang.Double";
      case "char" -> "java.lang.Character";
      default -> name;
    };
  }

  public static String boxed(final Types types, final TypeMirror type) {
    return switch (type.getKind()) {
      case BOOLEAN -> "java.lang.Boolean";
      case BYTE -> "java.lang.Byte";
      case SHORT -> "java.lang.Short";
      case INT -> "java.lang.Integer";
      case LONG -> "java.lang.Long";
      case FLOAT -> "java.lang.Float";
      case DOUBLE -> "java.lang.Double";
      case CHAR -> "java.lang.Character";
      default -> qualifiedName(types, type);
    };
  }

  public static boolean isBoxed(final String name) {
    return switch (name) {
      case "java.lang.Boolean", "java.lang.Byte", "java.lang.Short", "java.lang.Integer",
          "java.lang.Long", "java.lang.Character", "java.lang.Float", "java.lang.Double" -> true;
      default -> false;
    };
  }

  public static boolean isString(final Types types, final TypeMirror type) {
    final String name = qualifiedName(types, type);
    return "java.lang.String".equals(name) || "java.lang.CharSequence".equals(name);
  }

  public static boolean isEnum(final Types types, final TypeMirror type) {
    return asTypeElement(types, type)
        .filter(element -> element.getKind() == ElementKind.ENUM)
        .isPresent();
  }

  public static Optional<TypeMirror> optionalValueType(
      final Types types,
      final TypeMirror type
  ) {
    if (!(type instanceof final DeclaredType declared)
        || !"java.util.Optional".contentEquals(asTypeElement(types, type)
            .map(element -> element.getQualifiedName().toString())
            .orElse(""))) {
      return Optional.empty();
    }
    final List<? extends TypeMirror> arguments = declared.getTypeArguments();
    return arguments.isEmpty() ? Optional.empty() : Optional.of(arguments.get(0));
  }
}
