package cc.asylum.iridium.codegen.write;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.model.TypeRef;
import cc.asylum.forgery.stmt.Block;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import cc.asylum.iridium.codegen.code.Exprs;

public final class Registrar {

  private final String name;
  private final Class<?> implemented;
  private final TypeRef parameterType;
  private final String parameter;
  private final List<Consumer<Block>> statements = new ArrayList<>();
  private final List<Consumer<NestedTypes>> nested = new ArrayList<>();

  private Registrar(
    final String name,
    final Class<?> implemented,
    final Class<?> parameterType,
    final String parameter
  ) {
    this.name = name;
    this.implemented = implemented;
    this.parameterType = TypeRef.of(parameterType);
    this.parameter = parameter;
  }

  public static Registrar of(
    final String name,
    final Class<?> implemented,
    final Class<?> parameter,
    final String parameterName
  ) {
    return new Registrar(name, implemented, parameter, parameterName);
  }

  public Expr pool() {
    return Exprs.name(parameter);
  }

  public Registrar put(final String bean, final Expr value) {
    statements.add(body -> body.invoke(pool(), "put", Expr.lit(bean), value));
    return this;
  }

  public Registrar line(final Expr statement) {
    statements.add(body -> body.expr(statement));
    return this;
  }

  public Registrar nest(final Consumer<NestedTypes> configure) {
    nested.add(configure);
    return this;
  }

  public boolean empty() {
    return statements.isEmpty();
  }

  public void write(final Filer filer, final String pkg, final Element... origins) {
    SourceWriter.writeJava(filer, pkg, name, type -> {
      type.implements_(implemented);

      final NestedTypes nestedTypes = SourceWriter.nested(type);
      for (final Consumer<NestedTypes> configure : nested) {
        configure.accept(nestedTypes);
      }

      type.method("register", method -> {
        method.public_().overrides();
        method.parameter(parameterType, parameter);
        method.body(body -> {
          for (final Consumer<Block> statement : statements) {
            statement.accept(body);
          }
        });
      });

    }, origins);
    SourceWriter.writeService(filer, implemented, pkg + "." + name, origins);
  }
}
