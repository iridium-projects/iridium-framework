package cc.asylum.iridium.data.spec.processor.parse;

import cc.asylum.forgery.model.TypeRef;
import cc.asylum.iridium.codegen.model.Mirrors;
import cc.asylum.iridium.codegen.model.TypeMirrors;
import cc.asylum.iridium.codegen.code.Types;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.ast.query.specification.JSpecification;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.regex.Pattern;

public final class SpecTypes {

  public static final String JSPEC = JSpecification.class.getCanonicalName();
  public static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

  private final javax.lang.model.util.Types types;
  private final Elements elements;

  public SpecTypes(final javax.lang.model.util.Types types, final Elements elements) {
    this.types = types;
    this.elements = elements;
  }

  public boolean assignable(final TypeMirror type, final Class<?> target) {
    return TypeMirrors.isAssignable(types, elements, types.erasure(type), target);
  }

  public TypeMirror entityArgument(final TypeMirror type) {
    if (!(type instanceof final DeclaredType declared) || declared.getTypeArguments().isEmpty()) {
      return null;
    }
    final TypeMirror entity = declared.getTypeArguments().get(0);
    return entity.getKind() == TypeKind.WILDCARD ? null : entity;
  }

  public TypeElement asType(final TypeMirror type) {
    final Element element = types.asElement(types.erasure(type));
    return element instanceof final TypeElement typeElement ? typeElement : null;
  }

  public String qualified(final TypeMirror type) {
    final TypeElement element = asType(type);
    return element == null ? type.toString() : element.getQualifiedName().toString();
  }

  public String boxed(final TypeMirror type) {
    return TypeMirrors.boxed(types, type);
  }

  public boolean isString(final TypeMirror type) {
    return TypeMirrors.isString(types, type);
  }

  public boolean isEnum(final TypeMirror type) {
    final TypeElement element = asType(type);
    return element != null && element.getKind() == ElementKind.ENUM;
  }

  public boolean isEntity(final TypeMirror type) {
    final TypeElement element = asType(type);
    if (element == null) {
      return false;
    }
    for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {
      if (Mirrors.qualified(mirror).equals(Entity.class.getCanonicalName())) {
        return true;
      }
    }
    return false;
  }

  public Element constant(final TypeElement type, final String name) {
    if (type == null) {
      return null;
    }
    for (final Element enclosed : type.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.ENUM_CONSTANT && enclosed.getSimpleName().contentEquals(name)) {
        return enclosed;
      }
    }
    return null;
  }

  public String tableName(final TypeElement entity) {
    final String pkg = elements.getPackageOf(entity).getQualifiedName().toString();
    final String simple = entity.getSimpleName() + "Table";
    return pkg.isEmpty() ? simple : pkg + "." + simple;
  }

  public TypeRef listType(final TypeMirror type) {
    final String element = isEnum(type) || !type.getKind().isPrimitive() ? qualified(type) : boxed(type);
    return Types.list(Types.of(element));
  }

  public String parser(final TypeMirror type) {
    if (isEnum(type)) {
      return "enumeration";
    }
    if (isString(type)) {
      return "text";
    }
    final TypeKind kind = type.getKind();
    if (kind.isPrimitive()) {
      return switch (kind) {
        case BOOLEAN -> "bool";
        case BYTE -> "byteValue";
        case SHORT -> "shortValue";
        case INT -> "integer";
        case LONG -> "longValue";
        case FLOAT -> "floatValue";
        case DOUBLE -> "doubleValue";
        default -> null;
      };
    }
    return switch (qualified(type)) {
      case "java.lang.Integer" -> "integer";
      case "java.lang.Long" -> "longValue";
      case "java.lang.Double" -> "doubleValue";
      case "java.lang.Float" -> "floatValue";
      case "java.lang.Short" -> "shortValue";
      case "java.lang.Byte" -> "byteValue";
      case "java.lang.Boolean" -> "bool";
      case "java.math.BigDecimal" -> "decimal";
      case "java.math.BigInteger" -> "integerBig";
      case "java.util.UUID" -> "uuid";
      case "java.time.LocalDate" -> "localDate";
      case "java.time.LocalDateTime" -> "localDateTime";
      case "java.time.LocalTime" -> "localTime";
      case "java.time.Instant" -> "instant";
      case "java.time.OffsetDateTime" -> "offsetDateTime";
      case "java.time.ZonedDateTime" -> "zonedDateTime";
      default -> null;
    };
  }

  public String collectionParser(final TypeMirror type) {
    if (isEnum(type)) {
      return "enumerations";
    }
    if (isString(type)) {
      return "texts";
    }
    return switch (boxed(type)) {
      case "java.lang.Integer" -> "integers";
      case "java.lang.Long" -> "longs";
      case "java.lang.Double" -> "doubles";
      case "java.lang.Float" -> "floats";
      case "java.lang.Short" -> "shorts";
      case "java.lang.Byte" -> "bytes";
      case "java.lang.Boolean" -> "bools";
      case "java.math.BigDecimal" -> "decimals";
      case "java.math.BigInteger" -> "integerBigs";
      case "java.util.UUID" -> "uuids";
      default -> null;
    };
  }

  public boolean validPath(final String path) {
    if (path == null || path.isEmpty()) {
      return false;
    }
    for (final String segment : path.split("\\.", -1)) {
      if (!IDENTIFIER.matcher(segment).matches()) {
        return false;
      }
    }
    return true;
  }

  public boolean validName(final String name) {
    return IDENTIFIER.matcher(name).matches();
  }
}
