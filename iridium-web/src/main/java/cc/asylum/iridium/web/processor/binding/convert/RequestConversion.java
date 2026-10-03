package cc.asylum.iridium.web.processor.binding.convert;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.code.Types;
import cc.asylum.iridium.codegen.model.TypeMirrors;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.web.controller.Parameters;


import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;

@Internal
public final class RequestConversion {

  private final javax.lang.model.util.Types types;

  public RequestConversion(final javax.lang.model.util.Types types) {
    this.types = types;
  }

  public Expr convert(final TypeMirror type, final Expr raw, final boolean body) {
    if (body && type.getKind() == TypeKind.ARRAY && "byte[]".equals(type.toString())) {
      return raw;
    }

    if (body && TypeMirrors.isString(types, type)) {
      return Exprs.invokeStatic(Parameters.class, "text", raw);
    }

    if (type.getKind().isPrimitive()) {
      return primitive(type.getKind(), raw);
    }

    return boxed(TypeMirrors.qualified(types, type), raw, type);
  }

  public Expr optionalWrap(final TypeMirror inner, final Expr raw) {
    final Expr optional = Exprs.invokeStatic(Optional.class, "ofNullable", raw);
    final ParsedConversion parsed = parsed(TypeMirrors.qualified(types, inner));
    return parsed == null ? optional : optional.invoke("map", Exprs.methodRef(parsed.owner(), parsed.method()));
  }

  private Expr boxed(final String name, final Expr raw, final TypeMirror type) {
    return switch (name) {
      case "java.lang.String" -> raw;
      case "java.lang.Boolean" -> Exprs.invokeStatic(Boolean.class, "valueOf", raw);
      case "java.lang.Byte" -> Exprs.invokeStatic(Byte.class, "valueOf", raw);
      case "java.lang.Short" -> Exprs.invokeStatic(Short.class, "valueOf", raw);
      case "java.lang.Integer" -> Exprs.invokeStatic(Integer.class, "valueOf", raw);
      case "java.lang.Long" -> Exprs.invokeStatic(Long.class, "valueOf", raw);
      case "java.lang.Character" -> raw.invoke("charAt", Expr.lit(0));
      case "java.lang.Float" -> Exprs.invokeStatic(Float.class, "valueOf", raw);
      case "java.lang.Double" -> Exprs.invokeStatic(Double.class, "valueOf", raw);
      default -> json(type, raw);
    };
  }

  private Expr primitive(final TypeKind kind, final Expr raw) {
    return switch (kind) {
      case BOOLEAN -> Exprs.invokeStatic(Boolean.class, "parseBoolean", raw);
      case BYTE -> Exprs.invokeStatic(Byte.class, "parseByte", raw);
      case SHORT -> Exprs.invokeStatic(Short.class, "parseShort", raw);
      case INT -> Exprs.invokeStatic(Integer.class, "parseInt", raw);
      case LONG -> Exprs.invokeStatic(Long.class, "parseLong", raw);
      case CHAR -> raw.invoke("charAt", Expr.lit(0));
      case FLOAT -> Exprs.invokeStatic(Float.class, "parseFloat", raw);
      case DOUBLE -> Exprs.invokeStatic(Double.class, "parseDouble", raw);
      default -> raw;
    };
  }

  private Expr json(final TypeMirror type, final Expr raw) {
    return Exprs.invokeStatic(Types.of("io.avaje.jsonb.Jsonb"), "instance")
      .invoke("type", Exprs.classLit(Types.of(types.erasure(type))))
      .invoke("fromJson", raw);
  }

  private ParsedConversion parsed(final String name) {
    return switch (name) {
      case "java.lang.Long" -> new ParsedConversion(Long.class, "parseLong");
      case "java.lang.Integer" -> new ParsedConversion(Integer.class, "parseInt");
      case "java.lang.Double" -> new ParsedConversion(Double.class, "parseDouble");
      case "java.lang.Float" -> new ParsedConversion(Float.class, "parseFloat");
      case "java.lang.Boolean" -> new ParsedConversion(Boolean.class, "parseBoolean");
      case "java.lang.Short" -> new ParsedConversion(Short.class, "parseShort");
      case "java.lang.Byte" -> new ParsedConversion(Byte.class, "parseByte");
      default -> null;
    };
  }
}
