package cc.asylum.iridium.codegen.write;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.model.TypeRef;
import cc.asylum.forgery.stmt.Stmt;
import cc.asylum.forgery.type.ClassBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import cc.asylum.iridium.codegen.code.Exprs;

public final class Body {

  private final List<Stmt> statements = new ArrayList<>();
  private final List<Consumer<NestedTypes>> nested = new ArrayList<>();
  private int nestedIndex;

  public Body add(final Stmt statement) {
    if (statement != null) {
      statements.add(statement);
    }
    return this;
  }

  public Body add(final Stmt... statements) {
    for (final Stmt statement : statements) {
      add(statement);
    }
    return this;
  }

  public Body add(final List<Stmt> statements) {
    for (final Stmt statement : statements) {
      add(statement);
    }
    return this;
  }

  public TypeRef nest(final String name, final Consumer<ClassBuilder> configure) {
    nested.add(types -> types.nest(name, configure));
    return TypeRef.of(name);
  }

  public TypeRef nest(final Consumer<ClassBuilder> configure) {
    return nest("Nested" + nestedIndex++, configure);
  }

  public List<Stmt> statements() {
    return List.copyOf(statements);
  }

  public List<Consumer<NestedTypes>> nested() {
    return List.copyOf(nested);
  }

  public static Expr name(final String name) {
    return Exprs.name(name);
  }
}
