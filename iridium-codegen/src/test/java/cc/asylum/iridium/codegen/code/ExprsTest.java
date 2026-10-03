package cc.asylum.iridium.codegen.code;

import cc.asylum.forgery.expr.ClassLitExpr;
import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.expr.InvokeExpr;
import cc.asylum.forgery.expr.InvokeStaticExpr;
import cc.asylum.forgery.expr.LambdaExpr;
import cc.asylum.forgery.expr.LiteralExpr;
import cc.asylum.forgery.expr.LocalExpr;
import cc.asylum.forgery.expr.MethodRefExpr;
import cc.asylum.forgery.expr.NewExpr;
import cc.asylum.forgery.expr.NullExpr;
import cc.asylum.forgery.expr.ParamExpr;
import cc.asylum.forgery.expr.StaticFieldExpr;
import cc.asylum.forgery.expr.StringConcatExpr;
import cc.asylum.forgery.expr.TernaryExpr;
import cc.asylum.forgery.model.TypeRef;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class ExprsTest {

  @Test
  void nameAndParam() {
    assertInstanceOf(LocalExpr.class, Exprs.name("pool"));
    assertInstanceOf(ParamExpr.class, Exprs.param("pool"));
  }

  @Test
  void literals() {
    assertInstanceOf(NullExpr.class, Exprs.lit(null));
    final Expr existing = Expr.lit(1);
    assertSame(existing, Exprs.lit(existing));
    assertEquals(true, ((LiteralExpr) Exprs.lit(Boolean.TRUE)).value());
    assertEquals(false, ((LiteralExpr) Exprs.lit(false)).value());
    assertInstanceOf(cc.asylum.forgery.expr.CastExpr.class, Exprs.lit((byte) 7));
    assertInstanceOf(cc.asylum.forgery.expr.CastExpr.class, Exprs.lit((short) 8));
    assertEquals(9, ((LiteralExpr) Exprs.lit(9)).value());
    assertEquals(10L, ((LiteralExpr) Exprs.lit(10L)).value());
    assertEquals(1.5f, ((LiteralExpr) Exprs.lit(1.5f)).value());
    assertEquals(2.5d, ((LiteralExpr) Exprs.lit(2.5d)).value());
    assertEquals('z', ((LiteralExpr) Exprs.lit('z')).value());
    assertEquals("ok", ((LiteralExpr) Exprs.lit("ok")).value());
    assertThrows(IllegalArgumentException.class, () -> Exprs.lit(List.of()));
  }

  @Test
  void classLiterals() {
    assertInstanceOf(ClassLitExpr.class, Exprs.classLit(String.class));
    assertInstanceOf(ClassLitExpr.class, Exprs.classLit(TypeRef.of(String.class)));
    final TypeElement type = mock(TypeElement.class);
    final Name name = mock(Name.class);
    when(name.toString()).thenAnswer(invocation -> "java.lang.String");
    when(type.getQualifiedName()).thenAnswer(invocation -> name);
    assertInstanceOf(ClassLitExpr.class, Exprs.classLit(type));
  }

  @Test
  void invocationsAndConstruction() {
    final Expr arg = Expr.lit(1);
    assertInstanceOf(InvokeStaticExpr.class, Exprs.invokeStatic(String.class, "valueOf", arg));
    assertInstanceOf(InvokeStaticExpr.class, Exprs.invokeStatic(TypeRef.of(String.class), "valueOf", arg));
    assertInstanceOf(InvokeStaticExpr.class, Exprs.invokeStatic(String.class, "valueOf", List.of(arg)));
    assertInstanceOf(InvokeStaticExpr.class, Exprs.invokeStatic(TypeRef.of(String.class), "valueOf", List.of(arg)));
    assertInstanceOf(InvokeExpr.class, Exprs.invoke(Exprs.name("pool"), "get", List.of(arg)));
    assertInstanceOf(NewExpr.class, Exprs.new_(String.class, arg));
    assertInstanceOf(NewExpr.class, Exprs.new_(TypeRef.of(String.class), arg));
    assertInstanceOf(NewExpr.class, Exprs.new_(String.class, List.of(arg)));
    assertInstanceOf(NewExpr.class, Exprs.new_(TypeRef.of(String.class), List.of(arg)));
  }

  @Test
  void concatLambdaSelectAndFields() {
    assertEquals("", ((LiteralExpr) Exprs.concat(List.of())).value());
    assertEquals("a", ((LiteralExpr) Exprs.concat(List.of(Expr.lit("a")))).value());
    assertInstanceOf(StringConcatExpr.class, Exprs.concat(List.of(Expr.lit("a"), Expr.lit("b"), Expr.lit("c"))));
    assertInstanceOf(LambdaExpr.class, Exprs.lambda("x", Expr.lit(1)));
    assertInstanceOf(LambdaExpr.class, Exprs.lambda(List.of("x", "y"), Expr.lit(1)));
    assertInstanceOf(MethodRefExpr.class, Exprs.methodRef(String.class, "valueOf"));
    assertInstanceOf(MethodRefExpr.class, Exprs.methodRef(TypeRef.of(String.class), "valueOf"));
    assertInstanceOf(TernaryExpr.class, Exprs.select(Expr.lit(true), Expr.lit(1), Expr.lit(2)));
    assertInstanceOf(StaticFieldExpr.class, Exprs.staticField(String.class, "CASE_INSENSITIVE_ORDER"));
    assertInstanceOf(StaticFieldExpr.class, Exprs.staticField(TypeRef.of(String.class), "CASE_INSENSITIVE_ORDER"));
  }
}
