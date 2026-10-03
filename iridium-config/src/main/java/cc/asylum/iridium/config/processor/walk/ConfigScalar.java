package cc.asylum.iridium.config.processor.walk;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.model.Diagnostics;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.codegen.code.Types;
import cc.asylum.iridium.config.Config;
import cc.asylum.iridium.config.ConfigError;
import cc.asylum.iridium.core.result.Result;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

public record ConfigScalar(String method, Expr converter, TypeElement enumType) {

  public static ConfigScalar simple(final String method, final Expr converter) {
     return new ConfigScalar(method, converter, null);
  }

  public static ConfigScalar enumeration(final TypeElement enumType) {
     return new ConfigScalar(
        "enumeration",
        Exprs.lambda("value", Exprs.invokeStatic(
            Config.class,
            "parseEnum",
            Exprs.name("value"),
            Exprs.classLit(Types.of(enumType)))),
        enumType);
  }

  public Expr read(
      final Processing context,
      final Element element,
      final ConfigKey key,
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
      return Exprs.invokeStatic(Config.class, "find", key.code());
    }
    if (nullable) {
      return Exprs.select(
          Exprs.invokeStatic(Config.class, "has", key.code()),
          Exprs.invokeStatic(Config.class, method, key.code()).invoke("unwrap"),
          Expr.nil());
    }
    return Exprs.invokeStatic(Config.class, method, key.code());
  }

  private Expr withFallback(
      final Processing context,
      final Element element,
      final ConfigKey key,
      final String fallback
  ) {
    final Expr literal = literal(context, element, fallback);
    if (literal == null) {
      return null;
    }
    if (enumType != null) {
      return Exprs.invokeStatic(
          Config.class,
          "enumeration",
          key.code(),
          Exprs.classLit(Types.of(enumType)),
          literal);
    }
    return Exprs.invokeStatic(Config.class, method, key.code(), literal);
  }

  private Expr enumValue(final ConfigKey key, final boolean nullable) {
    final Expr value = Exprs.invokeStatic(
        Config.class,
        "enumeration",
        key.code(),
        Exprs.classLit(Types.of(enumType)));
    if (!nullable) {
      return value;
    }
    return Exprs.select(
        Exprs.invokeStatic(Config.class, "has", key.code()),
        value.invoke("unwrap"),
        Expr.nil());
  }

  private Expr literal(final Processing context, final Element element, final String fallback) {
    try {
      if (enumType != null) {
         return enumLiteral(context, element, fallback);
      }
      return switch (method) {
        case "string" -> Expr.lit(fallback);
        case "bool" -> boolLiteral(context, element, fallback);
        case "integer" -> Expr.lit(Integer.parseInt(fallback.trim()));
        case "longValue" -> Expr.lit(Long.parseLong(fallback.trim()));
        case "doubleValue" -> Expr.lit(Double.parseDouble(fallback.trim()));
        case "floatValue" -> Expr.lit(Float.parseFloat(fallback.trim()));
        case "shortValue" -> Exprs.lit((short) Short.parseShort(fallback.trim()));
        case "byteValue" -> Exprs.lit((byte) Byte.parseByte(fallback.trim()));
        default -> {
          Diagnostics.error(context.messager(), element, "invalid default '" + fallback + "'");
          yield null;
        }
      };
    } catch (final RuntimeException exception) {
      Diagnostics.error(context.messager(), element, "invalid default '" + fallback + "'");
      return null;
    }
  }

  private Expr boolLiteral(final Processing context, final Element element, final String fallback) {
    final Result<Boolean, ConfigError> parsed = Config.parseBool(fallback);
    if (parsed.isErr()) {
      Diagnostics.error(context.messager(), element, "invalid default '" + fallback + "'");
      return null;
    }
    return Expr.lit(parsed.unwrap());
  }

  private Expr enumLiteral(final Processing context, final Element element, final String fallback) {
    for (final Element enclosed : enumType.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.ENUM_CONSTANT
          && enclosed.getSimpleName().contentEquals(fallback)) {
        return Exprs.staticField(Types.of(enumType), fallback);
      }
    }
    Diagnostics.error(context.messager(), element, "invalid default '" + fallback + "'");
    return null;
  }
}
