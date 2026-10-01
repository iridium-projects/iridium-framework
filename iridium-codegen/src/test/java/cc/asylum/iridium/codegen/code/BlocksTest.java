package cc.asylum.iridium.codegen.code;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.model.TypeRef;
import cc.asylum.forgery.stmt.AssignStmt;
import cc.asylum.forgery.stmt.Block;
import cc.asylum.forgery.stmt.BlockStmt;
import cc.asylum.forgery.stmt.ExprStmt;
import cc.asylum.forgery.stmt.IfStmt;
import cc.asylum.forgery.stmt.LocalDeclStmt;
import cc.asylum.forgery.stmt.ReturnStmt;
import cc.asylum.forgery.stmt.Stmt;
import cc.asylum.forgery.stmt.TryStmt;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BlocksTest {

  @Test
  void statements() {
    final Expr value = Expr.lit(1);
    assertInstanceOf(ExprStmt.class, Blocks.expr(value));
    assertInstanceOf(ReturnStmt.class, Blocks.ret(value));
    assertInstanceOf(ReturnStmt.class, Blocks.ret());
    assertInstanceOf(LocalDeclStmt.class, Blocks.declare(TypeRef.INT, "n", value));
    assertInstanceOf(LocalDeclStmt.class, Blocks.declare(Integer.class, "n", value));
    assertInstanceOf(LocalDeclStmt.class, Blocks.declare(TypeRef.INT, "n"));
    assertInstanceOf(LocalDeclStmt.class, Blocks.declareVar("n", value));
    assertInstanceOf(AssignStmt.class, Blocks.assign("n", value));
    assertInstanceOf(AssignStmt.class, Blocks.assign(Exprs.name("n"), value));
    assertInstanceOf(IfStmt.class, Blocks.ifThen(Expr.lit(true), Blocks.ret(value)));
    final java.util.ArrayList<Stmt> withNull = new java.util.ArrayList<>();
    withNull.add(Blocks.ret(value));
    withNull.add(null);
    assertInstanceOf(TryStmt.class, Blocks.tryCatch(withNull, TypeRef.of(Exception.class), "ex", List.of()));
    assertInstanceOf(TryStmt.class, Blocks.tryCatch(List.of(Blocks.expr(value)), RuntimeException.class, "ex", List.of(Blocks.ret())));
    assertInstanceOf(BlockStmt.class, Blocks.block(Blocks.ret()));
  }

  @Test
  void addAllSkipsNulls() {
    final Block block = new Block();
    Blocks.addAll(block, Blocks.ret(), null);
    final java.util.ArrayList<Stmt> statements = new java.util.ArrayList<>();
    statements.add(Blocks.expr(Expr.lit(1)));
    statements.add(null);
    Blocks.addAll(block, statements);
    assertTrue(block.statements().size() >= 2);
  }
}
