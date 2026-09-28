package cc.asylum.iridium.data.spec.processor.emit;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.code.Types;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.Predicate;
import cc.asylum.iridium.data.spec.processor.node.SpecLeaf;
import cc.asylum.iridium.data.spec.processor.parse.SpecTypes;

public final class SpecPredicates {

  private final SpecTypes types;

  public SpecPredicates(final SpecTypes types) {
    this.types = types;
  }

  public Expr call(final SpecLeaf leaf, final Expr value) {
    final Expr expr = leaf.expr();
    return switch (leaf.op()) {
      case "Equal" -> expr.invoke("eqIf", value);
      case "NotEqual" -> expr.invoke("neIf", value);
      case "EqualIgnoreCase" -> expr.invoke("ilikeIf", value, like(LikeMode.EXACT));
      case "Like" -> expr.invoke("likeIf", value, like(LikeMode.ANYWHERE));
      case "LikeIgnoreCase" -> expr.invoke("ilikeIf", value, like(LikeMode.ANYWHERE));
      case "NotLike" -> not(expr.invoke("likeIf", value, like(LikeMode.ANYWHERE)));
      case "StartingWith" -> expr.invoke("likeIf", value, like(LikeMode.START));
      case "EndingWith" -> expr.invoke("likeIf", value, like(LikeMode.END));
      case "GreaterThan" -> expr.invoke("gtIf", value);
      case "GreaterThanOrEqual" -> expr.invoke("geIf", value);
      case "LessThan" -> expr.invoke("ltIf", value);
      case "LessThanOrEqual" -> expr.invoke("leIf", value);
      case "Null" -> expr.invoke("isNull");
      case "NotNull" -> expr.invoke("isNotNull");
      case "Empty" -> Exprs.invokeStatic(Predicate.class, "or", expr.invoke("isNull"), expr.invoke("eq", Expr.lit("")));
      case "NotEmpty" -> Exprs.invokeStatic(Predicate.class, "and", expr.invoke("isNotNull"), expr.invoke("ne", Expr.lit("")));
      case "True" -> expr.invoke("eq", Expr.lit(true));
      case "False" -> expr.invoke("eq", Expr.lit(false));
      default -> expr.invoke("eqIf", value);
    };
  }

  public Expr flagCall(final SpecLeaf leaf, final Expr raw) {
    return Exprs.select(
        Exprs.invokeStatic(SpecValues.class, "truthy", raw).eq(Exprs.staticField(Boolean.class, "TRUE")),
         call(leaf, Expr.nil()),
        Expr.nil());
  }

  public Expr collectionCall(final SpecLeaf leaf, final Expr raw) {
    if (types.isEnum(leaf.type())) {
      return Exprs.invokeStatic(SpecValues.class, "enumerations", raw, Exprs.classLit(Types.of(types.qualified(leaf.type()))));
    }
    final String parser = types.collectionParser(leaf.type());
    return parser == null ? null : Exprs.invokeStatic(SpecValues.class, parser, raw);
  }

  public Expr parseCall(final SpecLeaf leaf, final Expr raw) {
    final String parser = types.parser(leaf.type());
    if (parser == null) {
      return null;
    }
    if ("enumeration".equals(parser)) {
      return Exprs.invokeStatic(SpecValues.class, "enumeration", raw, Exprs.classLit(Types.of(types.qualified(leaf.type()))));
    }
    return Exprs.invokeStatic(SpecValues.class, parser, raw);
  }

  public Expr wrap(final SpecLeaf leaf, final Expr predicate) {
    return leaf.not() ? not(predicate) : predicate;
  }

  public Expr not(final Expr predicate) {
    return Exprs.invokeStatic(Predicate.class, "not", predicate);
  }

  private Expr like(final LikeMode mode) {
    return Exprs.staticField(LikeMode.class, mode.name());
  }
}
