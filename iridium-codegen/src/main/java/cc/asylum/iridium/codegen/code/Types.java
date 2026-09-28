package cc.asylum.iridium.codegen.code;

import cc.asylum.forgery.model.TypeRef;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import java.util.List;

public final class Types {

  private Types() {
  }

  public static TypeRef of(final Class<?> type) {
    return TypeRef.of(type);
  }

  public static TypeRef of(final TypeElement type) {
    return TypeRef.of(type.getQualifiedName().toString());
  }

  public static TypeRef of(final TypeMirror type) {
    return switch (type.getKind()) {
      case BOOLEAN -> TypeRef.BOOLEAN;
      case BYTE -> TypeRef.BYTE;
      case SHORT -> TypeRef.SHORT;
      case INT -> TypeRef.INT;
      case LONG -> TypeRef.LONG;
      case CHAR -> TypeRef.CHAR;
      case FLOAT -> TypeRef.FLOAT;
      case DOUBLE -> TypeRef.DOUBLE;
      case VOID -> TypeRef.VOID;
      case ARRAY -> of(((ArrayType) type).getComponentType()).array();
      case DECLARED -> declared((DeclaredType) type);
      case TYPEVAR -> TypeRef.typeVar(((TypeVariable) type).asElement().getSimpleName().toString());
      case WILDCARD -> wildcard((WildcardType) type);
      default -> TypeRef.of(type.toString());
    };
  }

  public static TypeRef of(final String packageName, final String simpleName) {
    if (packageName == null || packageName.isEmpty()) {
      return TypeRef.of(simpleName);
    }
    return TypeRef.of(packageName + "." + simpleName);
  }

  public static TypeRef of(final String canonicalName) {
    return TypeRef.of(canonicalName);
  }

  public static TypeRef parameterized(final Class<?> raw, final TypeRef... arguments) {
    return TypeRef.parameterized(raw, arguments);
  }

  public static TypeRef parameterized(final TypeRef raw, final TypeRef... arguments) {
    return TypeRef.parameterized(raw, arguments);
  }

  public static TypeRef list(final TypeRef element) {
    return TypeRef.parameterized(List.class, element);
  }

  public static TypeRef optional(final TypeRef element) {
    return TypeRef.parameterized(java.util.Optional.class, element);
  }

  public static TypeRef wildcardExtends(final TypeRef bound) {
    return TypeRef.wildcardExtends(bound);
  }

  private static TypeRef declared(final DeclaredType type) {
    final Element element = type.asElement();
    final TypeRef raw = element instanceof final TypeElement typeElement
      ? of(typeElement)
      : TypeRef.of(type.toString());

    if (type.getTypeArguments().isEmpty()) {
      return raw;
    }

    final TypeRef[] arguments = type.getTypeArguments().stream().map(Types::of).toArray(TypeRef[]::new);
    return TypeRef.parameterized(raw, arguments);
  }

  private static TypeRef wildcard(final WildcardType type) {
    if (type.getExtendsBound() != null) {
      return TypeRef.wildcardExtends(of(type.getExtendsBound()));
    }
    if (type.getSuperBound() != null) {
      return TypeRef.wildcardSuper(of(type.getSuperBound()));
    }
    return TypeRef.wildcard();
  }
}
