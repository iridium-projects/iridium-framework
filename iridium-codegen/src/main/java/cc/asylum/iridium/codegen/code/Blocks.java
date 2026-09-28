package cc.asylum.iridium.codegen.code;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.model.TypeRef;
import cc.asylum.forgery.stmt.Block;
import cc.asylum.forgery.stmt.BlockStmt;
import cc.asylum.forgery.stmt.ExprStmt;
import cc.asylum.forgery.stmt.LocalDeclStmt;
import cc.asylum.forgery.stmt.ReturnStmt;
import cc.asylum.forgery.stmt.Stmt;

import java.util.List;

public final class Blocks {

  private Blocks() {
  }

  public static Stmt expr(final Expr value) {
    return new ExprStmt(value);
  }

  public static Stmt ret(final Expr value) {
    return new ReturnStmt(value);
  }

  public static Stmt ret() {
    return new ReturnStmt(null);
  }

  public static Stmt declare(final TypeRef type, final String name, final Expr value) {
    return new LocalDeclStmt(name, type, value, false);
  }

  public static Stmt declare(final Class<?> type, final String name, final Expr value) {
    return declare(TypeRef.of(type), name, value);
  }

  public static Stmt declare(final TypeRef type, final String name) {
    return new LocalDeclStmt(name, type, null, false);
  }

  public static Stmt declareVar(final String name, final Expr value) {
    return new LocalDeclStmt(name, TypeRef.OBJECT, value, true);
  }

  public static Stmt assign(final String name, final Expr value) {
    return assign(Exprs.name(name), value);
  }

  public static Stmt assign(final Expr target, final Expr value) {
    final Block block = new Block();
    block.assign(target, value);
    return block.statements().getFirst();
  }

  public static Stmt ifThen(final Expr condition, final Stmt... body) {
    final Block block = new Block();
    block.if_(condition, then -> addAll(then, body));
    return block.statements().getFirst();
  }

  public static Stmt tryCatch(
      final List<Stmt> body,
      final TypeRef caught,
      final String name,
      final List<Stmt> handler
  ) {
    final Block block = new Block();
    block.try_(trial -> {
      trial.body(tryBody -> addAll(tryBody, body));
      trial.catch_(caught, name, catchBody -> addAll(catchBody, handler));
    });
    return block.statements().getFirst();
  }

  public static Stmt tryCatch(
      final List<Stmt> body,
      final Class<?> caught,
      final String name,
      final List<Stmt> handler
  ) {
    return tryCatch(body, TypeRef.of(caught), name, handler);
  }

  public static Stmt block(final Stmt... body) {
    return new BlockStmt(List.of(body));
  }

  public static void addAll(final Block block, final Stmt... statements) {
    for (final Stmt statement : statements) {
      if (statement != null) {
        block.add(statement);
      }
    }
  }

  public static void addAll(final Block block, final List<Stmt> statements) {
    for (final Stmt statement : statements) {
      if (statement != null) {
        block.add(statement);
      }
    }
  }
}
