package cc.asylum.iridium.data.spec.processor.parse;

import cc.asylum.iridium.core.util.Strings;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public final class SpecOps {

  private SpecOps() {
  }

  public static String name(final Object spec) {
    if (!(spec instanceof final TypeMirror type)) {
      return null;
    }
     return canonical(Strings.simpleName(type.toString()));
  }

  public static String canonical(final String name) {
    return switch (name) {
      case "IsNull" -> "Null";
      case "IsNotNull" -> "NotNull";
      case "StartsWith" -> "StartingWith";
      case "EndsWith" -> "EndingWith";
      default -> name;
    };
  }

  public static boolean known(final String op) {
    return switch (op) {
      case "Equal", "NotEqual", "EqualIgnoreCase", "Like", "LikeIgnoreCase", "NotLike",
          "GreaterThan", "GreaterThanOrEqual", "LessThan", "LessThanOrEqual",
          "In", "NotIn", "Null", "NotNull", "Between", "StartingWith", "EndingWith",
          "Empty", "NotEmpty", "True", "False" -> true;
      default -> false;
    };
  }

  public static boolean flag(final String op) {
    return switch (op) {
      case "Null", "NotNull", "Empty", "NotEmpty", "True", "False" -> true;
      default -> false;
    };
  }

  public static boolean collection(final String op) {
    return "In".equals(op) || "NotIn".equals(op);
  }

  public static boolean between(final String op) {
    return "Between".equals(op);
  }

  public static boolean compatible(final String op, final TypeMirror type, final SpecTypes types) {
    final boolean string = types.isString(type);
    final boolean bool = type.getKind() == TypeKind.BOOLEAN || "java.lang.Boolean".equals(types.qualified(type));
    return switch (op) {
      case "Like", "LikeIgnoreCase", "NotLike", "StartingWith", "EndingWith", "EqualIgnoreCase", "Empty", "NotEmpty" -> string;
      case "True", "False" -> bool;
      case "Null", "NotNull" -> true;
      default -> types.parser(type) != null || types.isEnum(type) || string;
    };
  }

  public static String kind(final AnnotationMirror mirror, final SpecMirrors mirrors) {
    final Object value = mirrors.member(mirror, "type");
    if (value instanceof final javax.lang.model.element.VariableElement variable) {
      return variable.getSimpleName().toString();
    }
    return value == null ? "INNER" : value.toString();
  }
}
