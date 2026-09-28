package cc.asylum.iridium.codegen.code;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.expr.LocalExpr;
import cc.asylum.forgery.expr.ParamExpr;
import cc.asylum.forgery.model.TypeRef;

import javax.lang.model.element.TypeElement;
import java.util.List;

public final class Exprs {

  private Exprs() {
  }

  public static Expr name(final String name) {
    return new LocalExpr(name, TypeRef.OBJECT);
  }

  public static Expr param(final String name) {
    return new ParamExpr(name, TypeRef.OBJECT);
  }

  public static Expr lit(final Object value) {
    return switch (value) {
      case null -> Expr.nil();
      case Expr expr -> expr;
      case Boolean bool -> Expr.lit(bool.booleanValue());
      case Byte number -> Expr.lit(number.intValue()).cast(byte.class);
      case Short number -> Expr.lit(number.intValue()).cast(short.class);
      case Integer number -> Expr.lit(number.intValue());
      case Long number -> Expr.lit(number.longValue());
      case Float number -> Expr.lit(number.floatValue());
      case Double number -> Expr.lit(number.doubleValue());
      case Character character -> Expr.lit(character.charValue());
      case String text -> Expr.lit(text);
      default -> throw new IllegalArgumentException("unsupported literal: " + value.getClass().getName());
    };
  }

  public static Expr classLit(final TypeRef type) {
    return Expr.classLit(type);
  }

  public static Expr classLit(final Class<?> type) {
    return Expr.classLit(type);
  }

  public static Expr classLit(final TypeElement type) {
    return Expr.classLit(Types.of(type));
  }

  public static Expr invokeStatic(final Class<?> owner, final String name, final Expr... arguments) {
    return Expr.invokeStatic(owner, name, arguments);
  }

  public static Expr invokeStatic(final TypeRef owner, final String name, final Expr... arguments) {
    return Expr.invokeStatic(owner, name, arguments);
  }

  public static Expr invokeStatic(final Class<?> owner, final String name, final List<Expr> arguments) {
    return Expr.invokeStatic(owner, name, arguments.toArray(Expr[]::new));
  }

  public static Expr invokeStatic(final TypeRef owner, final String name, final List<Expr> arguments) {
    return Expr.invokeStatic(owner, name, arguments.toArray(Expr[]::new));
  }

  public static Expr invoke(final Expr receiver, final String name, final List<Expr> arguments) {
    return receiver.invoke(name, arguments.toArray(Expr[]::new));
  }

  public static Expr new_(final Class<?> type, final Expr... arguments) {
    return Expr.new_(type, arguments);
  }

  public static Expr new_(final TypeRef type, final Expr... arguments) {
    return Expr.new_(type, arguments);
  }

  public static Expr new_(final Class<?> type, final List<Expr> arguments) {
    return Expr.new_(TypeRef.of(type), arguments.toArray(Expr[]::new));
  }

  public static Expr new_(final TypeRef type, final List<Expr> arguments) {
    return Expr.new_(type, arguments.toArray(Expr[]::new));
  }

  public static Expr concat(final List<Expr> parts) {
    if (parts.isEmpty()) {
      return Expr.lit("");
    }

    Expr result = parts.getFirst();
    for (int i = 1; i < parts.size(); i++) {
      result = result.concat(parts.get(i));
    }

    return result;
  }

  public static Expr lambda(final String param, final Expr body) {
    return Expr.lambda(List.of(param), body);
  }

  public static Expr lambda(final List<String> params, final Expr body) {
    return Expr.lambda(params, body);
  }

  public static Expr methodRef(final Class<?> owner, final String name) {
    return Expr.methodRef(TypeRef.of(owner), name);
  }

  public static Expr methodRef(final TypeRef owner, final String name) {
    return Expr.methodRef(owner, name);
  }

  public static Expr select(final Expr condition, final Expr whenTrue, final Expr whenFalse) {
    return condition.ternary(whenTrue, whenFalse);
  }

  public static Expr staticField(final Class<?> owner, final String name) {
    return Expr.staticField(owner, name);
  }

  public static Expr staticField(final TypeRef owner, final String name) {
    return Expr.staticField(owner, name);
  }
}
