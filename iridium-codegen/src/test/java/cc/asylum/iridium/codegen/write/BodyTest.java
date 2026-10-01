package cc.asylum.iridium.codegen.write;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.expr.LocalExpr;
import cc.asylum.forgery.model.ClassRef;
import cc.asylum.forgery.stmt.ReturnStmt;
import cc.asylum.forgery.stmt.Stmt;
import cc.asylum.forgery.type.ClassBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BodyTest {

  @Test
  void addsStatementsAndSkipsNull() {
    final Body body = new Body();
    final Stmt statement = new ReturnStmt(Expr.lit(1));
    assertEquals(body, body.add((Stmt) null).add(statement).add(statement, null).add(List.of(statement)));
    assertEquals(3, body.statements().size());
    assertTrue(body.nested().isEmpty());
  }

  @Test
  void nestsNamedAndAnonymous() {
    final Body body = new Body();
    assertEquals("Holder", ((ClassRef) body.nest("Holder", builder -> { })).canonicalName());
    assertEquals("Nested0", ((ClassRef) body.nest(ClassBuilder::public_)).canonicalName());
    assertEquals("Nested1", ((ClassRef) body.nest(ClassBuilder::final_)).canonicalName());
    assertEquals(3, body.nested().size());
  }

  @Test
  void nameDelegates() {
    assertInstanceOf(LocalExpr.class, Body.name("pool"));
  }
}
