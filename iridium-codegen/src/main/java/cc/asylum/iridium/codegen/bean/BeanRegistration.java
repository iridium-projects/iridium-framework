package cc.asylum.iridium.codegen.bean;

import cc.asylum.iridium.codegen.write.NestedTypes;
import cc.asylum.iridium.codegen.write.Registrar;

import cc.asylum.iridium.codegen.Processing;

import cc.asylum.forgery.expr.Expr;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Consumer;

import cc.asylum.iridium.codegen.code.Types;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.model.Elements;
import cc.asylum.iridium.codegen.model.Diagnostics;

public final class BeanRegistration {

  public final Processing processing;
  public final Registrar registrar;
  private final List<ArgumentBinder> binders;
  private final Set<String> names = new LinkedHashSet<>();

  public BeanRegistration(final Processing processing, final Registrar registrar) {
    this.processing = processing;
    this.registrar = registrar;
    this.binders = ServiceLoader.load(ArgumentBinder.class, ArgumentBinder.class.getClassLoader())
      .stream()
      .map(ServiceLoader.Provider::get)
      .toList();
  }

  public Set<TypeElement> types(final Class<? extends Annotation> annotation, final ElementKind kind) {
    return Elements.annotatedTypes(processing.round(), annotation, kind);
  }

  public Set<ExecutableElement> methods(final Class<? extends Annotation> annotation) {
    return Elements.annotatedMethods(processing.round(), annotation);
  }

  public String beanName(final TypeElement type) {
    return Elements.decapitalize(type.getSimpleName().toString());
  }

  public boolean claim(final Element element, final String name) {
    if (names.add(name)) {
      return true;
    }

    error(element, "duplicate bean name '" + name + "'");
    return false;
  }

  public boolean claimed(final String name) {
    return names.contains(name);
  }

  public void put(final String name, final Expr value) {
    registrar.put(name, value);
  }

  public void nest(final Consumer<NestedTypes> configure) {
    registrar.nest(configure);
  }

  public void error(final Element element, final String message) {
    Diagnostics.error(processing.messager(), element, message);
  }

  public Expr pool() {
    return registrar.pool();
  }

  public List<Expr> args(final ExecutableElement executable) {
    return args(executable, pool());
  }

  public List<Expr> args(final ExecutableElement executable, final Expr pool) {
    final List<Expr> args = new ArrayList<>();
    if (executable == null) {
      return args;
    }

    for (final VariableElement parameter : executable.getParameters()) {
      args.add(bind(parameter).orElseGet(() -> pool.invoke("get", Exprs.classLit(Types.of(parameter.asType())))));
    }

    return args;
  }

  public Optional<Expr> bind(final VariableElement parameter) {
    for (final ArgumentBinder binder : binders) {
      final Optional<Expr> bound = binder.bind(parameter, processing);
      if (bound.isPresent()) {
        return bound;
      }
    }
    return Optional.empty();
  }

  public Expr construct(final TypeElement type) {
    final ExecutableElement constructor = Elements.constructor(type);
    if (constructor == null) {
      error(type, "type must have exactly one constructor");
      return Expr.nil();
    }

    return Exprs.new_(Types.of(type.asType()), args(constructor));
  }
}
