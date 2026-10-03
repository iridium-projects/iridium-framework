package cc.asylum.iridium.config.processor.walk;

import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.codegen.model.TypeMirrors;
import cc.asylum.iridium.config.Config;
import cc.asylum.iridium.core.result.Result;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public final class ConfigTypes {

  private final Processing context;

  public ConfigTypes(final Processing context) {
    this.context = context;
  }

  public ConfigScalar scalar(final TypeMirror type) {
    return switch (type.getKind()) {
      case BOOLEAN -> ConfigScalar.simple("bool", Exprs.methodRef(Config.class, "parseBool"));
      case INT -> ConfigScalar.simple("integer", Exprs.methodRef(Config.class, "parseInt"));
      case LONG -> ConfigScalar.simple("longValue", Exprs.methodRef(Config.class, "parseLong"));
      case DOUBLE -> ConfigScalar.simple("doubleValue", Exprs.methodRef(Config.class, "parseDouble"));
      case FLOAT -> ConfigScalar.simple("floatValue", Exprs.methodRef(Config.class, "parseFloat"));
      case SHORT -> ConfigScalar.simple("shortValue", Exprs.methodRef(Config.class, "parseShort"));
      case BYTE -> ConfigScalar.simple("byteValue", Exprs.methodRef(Config.class, "parseByte"));
      case DECLARED -> declaredScalar(type);
      default -> null;
    };
  }

  public boolean isCollection(final TypeMirror type) {
    final TypeElement element = TypeMirrors.asTypeElement(context.types(), type).orElse(null);
    if (element == null) {
      return false;
    }
    final String name = element.getQualifiedName().toString();
    return "java.util.List".equals(name)
        || "java.util.Set".equals(name)
        || "java.util.Map".equals(name);
  }

  public boolean isObject(final TypeMirror type) {
     return scalar(type) == null
        && TypeMirrors.optionalValue(context.types(), type).isEmpty()
        && type instanceof javax.lang.model.type.DeclaredType
        && !isCollection(type);
  }

  public boolean same(final TypeMirror type, final Class<?> target) {
    return TypeMirrors.isSame(context.types(), context.elements(), type, target);
  }

  private ConfigScalar declaredScalar(final TypeMirror type) {
    if (same(type, String.class)) {
      return ConfigScalar.simple("string", Exprs.lambda("value", Exprs.invokeStatic(Result.class, "ok", Exprs.name("value"))));
    }
    if (same(type, Boolean.class)) {
      return ConfigScalar.simple("bool", Exprs.methodRef(Config.class, "parseBool"));
    }
    if (same(type, Integer.class)) {
      return ConfigScalar.simple("integer", Exprs.methodRef(Config.class, "parseInt"));
    }
    if (same(type, Long.class)) {
      return ConfigScalar.simple("longValue", Exprs.methodRef(Config.class, "parseLong"));
    }
    if (same(type, Double.class)) {
      return ConfigScalar.simple("doubleValue", Exprs.methodRef(Config.class, "parseDouble"));
    }
    if (same(type, Float.class)) {
      return ConfigScalar.simple("floatValue", Exprs.methodRef(Config.class, "parseFloat"));
    }
    if (same(type, Short.class)) {
      return ConfigScalar.simple("shortValue", Exprs.methodRef(Config.class, "parseShort"));
    }
    if (same(type, Byte.class)) {
      return ConfigScalar.simple("byteValue", Exprs.methodRef(Config.class, "parseByte"));
    }
    final TypeElement element = TypeMirrors.asTypeElement(context.types(), type).orElse(null);
    if (element != null && element.getKind() == ElementKind.ENUM) {
      return ConfigScalar.enumeration(element);
    }
    return null;
  }
}
