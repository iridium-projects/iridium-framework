package cc.asylum.iridium.data.spec.processor.emit;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.model.Diagnostics;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.code.Types;
import cc.asylum.iridium.core.util.Strings;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import cc.asylum.iridium.data.spec.processor.node.SpecLeaf;
import cc.asylum.iridium.data.spec.processor.parse.SpecOps;
import cc.asylum.iridium.data.spec.processor.parse.SpecTypes;

public final class SpecLiterals {

  private final SpecTypes types;
  private final Messager messager;

  public SpecLiterals(final SpecTypes types, final Messager messager) {
    this.types = types;
    this.messager = messager;
  }

  public Expr constLiteral(final Element site, final SpecLeaf leaf, final String raw) {
    if (SpecOps.flag(leaf.op())) {
      return null;
    }
    final String type = types.qualified(leaf.type());
    if (types.isEnum(leaf.type())) {
      if (types.constant(types.asType(leaf.type()), raw) == null) {
         error(site, "unknown enum constant " + type + "." + raw);
        return null;
      }
      return Exprs.staticField(Types.of(type), raw);
    }
    if (types.isString(leaf.type())) {
      return Expr.lit(raw);
    }
    if ("java.lang.Boolean".equals(types.boxed(leaf.type())) || leaf.type().getKind() == TypeKind.BOOLEAN) {
      final Boolean parsed = Strings.truthy(raw);
      if (parsed == null) {
         error(site, "invalid boolean constVal '" + raw + "'");
        return null;
      }
      return Expr.lit(parsed);
    }
    try {
      final String trimmed = raw.trim();
      return switch (types.boxed(leaf.type())) {
        case "java.lang.Integer" -> Expr.lit(Integer.parseInt(trimmed));
        case "java.lang.Long" -> Expr.lit(Long.parseLong(trimmed));
        case "java.lang.Double" -> Expr.lit(Double.parseDouble(trimmed));
        case "java.lang.Float" -> Expr.lit(Float.parseFloat(trimmed));
        case "java.lang.Short" -> Exprs.lit(Short.parseShort(trimmed));
        case "java.lang.Byte" -> Exprs.lit(Byte.parseByte(trimmed));
        case "java.math.BigDecimal" -> Exprs.new_(BigDecimal.class, Expr.lit(trimmed));
        case "java.math.BigInteger" -> Exprs.new_(BigInteger.class, Expr.lit(trimmed));
        case "java.util.UUID" -> Exprs.invokeStatic(UUID.class, "fromString", Expr.lit(trimmed));
        case "java.time.LocalDate" -> Exprs.invokeStatic(LocalDate.class, "parse", Expr.lit(trimmed));
        case "java.time.LocalDateTime" -> Exprs.invokeStatic(LocalDateTime.class, "parse", Expr.lit(trimmed));
        case "java.time.LocalTime" -> Exprs.invokeStatic(LocalTime.class, "parse", Expr.lit(trimmed));
        case "java.time.Instant" -> Exprs.invokeStatic(Instant.class, "parse", Expr.lit(trimmed));
        case "java.time.OffsetDateTime" -> Exprs.invokeStatic(OffsetDateTime.class, "parse", Expr.lit(trimmed));
        case "java.time.ZonedDateTime" -> Exprs.invokeStatic(ZonedDateTime.class, "parse", Expr.lit(trimmed));
        default -> {
           error(site, "unsupported constVal for " + type);
          yield null;
        }
      };
    } catch (final RuntimeException exception) {
       error(site, "invalid constVal '" + raw + "'");
      return null;
    }
  }

  private void error(final Element element, final String message) {
    Diagnostics.error(messager, element, message);
  }
}
