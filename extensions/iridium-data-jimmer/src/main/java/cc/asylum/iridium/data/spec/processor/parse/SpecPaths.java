package cc.asylum.iridium.data.spec.processor.parse;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.model.Diagnostics;
import cc.asylum.iridium.codegen.code.Exprs;
import org.babyfish.jimmer.sql.JoinType;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.LinkedHashMap;
import java.util.Map;
import cc.asylum.iridium.data.spec.Join;
import cc.asylum.iridium.data.spec.Joins;
import cc.asylum.iridium.data.spec.processor.node.SpecJoin;
import cc.asylum.iridium.data.spec.processor.node.SpecResolved;

public final class SpecPaths {

  private static final Expr TABLE = Exprs.name("table");

  private final SpecTypes types;
  private final SpecMirrors mirrors;
  private final Messager messager;

  public SpecPaths(final SpecTypes types, final SpecMirrors mirrors, final Messager messager) {
    this.types = types;
    this.mirrors = mirrors;
    this.messager = messager;
  }

  public Map<String, SpecJoin> joins(final VariableElement parameter, final TypeElement entity) {
    final Map<String, SpecJoin> joins = new LinkedHashMap<>();
    final var annotations = new java.util.ArrayList<>(mirrors.all(parameter, Join.class));
    final var container = mirrors.first(parameter, Joins.class);
    if (container != null) {
      annotations.addAll(mirrors.nested(container, "value"));
    }
    for (final var join : annotations) {
      final String alias = mirrors.string(join, "alias");
      final String path = mirrors.string(join, "path");
      if (!types.validName(alias) || !types.validPath(path)) {
         error(parameter, "invalid @Join");
        return null;
      }
      if (joins.containsKey(alias)) {
         error(parameter, "duplicate join alias '" + alias + "'");
        return null;
      }
      final TypeMirror target = walk(parameter, entity.asType(), path.split("\\."), true);
      if (target == null) {
        return null;
      }
      joins.put(alias, new SpecJoin(path, SpecOps.kind(join, mirrors), target));
    }
    return joins;
  }

  public SpecResolved resolve(
      final VariableElement parameter,
      final TypeElement entity,
      final String path,
      final Map<String, SpecJoin> joins
  ) {
    final String[] segments = path.split("\\.");
    Expr expr = TABLE;
    TypeMirror current = entity.asType();
    int start = 0;
    final SpecJoin aliased = joins.get(segments[0]);
    if (aliased != null) {
      expr = navigate(aliased.path(), aliased.kind());
      current = aliased.target();
      start = 1;
    }
    if (start == segments.length) {
       error(parameter, "spec path '" + path + "' does not select a property");
      return null;
    }
    for (int i = start; i < segments.length - 1; i++) {
      final TypeMirror next = property(parameter, current, segments[i]);
      if (next == null) {
        return null;
      }
      if (!types.isEntity(next)) {
         error(parameter, "'" + segments[i] + "' is not an association");
        return null;
      }
      expr = expr.invoke(segments[i]);
      current = next;
    }
    final String leaf = segments[segments.length - 1];
    final TypeMirror type = property(parameter, current, leaf);
    return type == null ? null : new SpecResolved(expr.invoke(leaf), type);
  }

  private TypeMirror walk(
      final Element site,
      final TypeMirror start,
      final String[] segments,
      final boolean association
  ) {
    TypeMirror current = start;
    for (final String segment : segments) {
      final TypeMirror next = property(site, current, segment);
      if (next == null) {
        return null;
      }
      if (association && !types.isEntity(next)) {
         error(site, "'" + segment + "' is not an association");
        return null;
      }
      current = next;
    }
    return current;
  }

  private TypeMirror property(final Element site, final TypeMirror owner, final String name) {
    final TypeElement type = types.asType(owner);
    if (type == null) {
       error(site, "cannot resolve property '" + name + "'");
      return null;
    }
    final ExecutableElement method = method(type, name);
    if (method == null) {
       error(site, "unknown property '" + name + "' on " + type.getQualifiedName());
      return null;
    }
    return method.getReturnType();
  }

  private ExecutableElement method(final TypeElement type, final String name) {
    for (final Element enclosed : type.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.METHOD) {
        final ExecutableElement method = (ExecutableElement) enclosed;
        if (method.getParameters().isEmpty() && method.getSimpleName().contentEquals(name)) {
          return method;
        }
      }
    }
    for (final TypeMirror superType : type.getInterfaces()) {
      final TypeElement parent = types.asType(superType);
      if (parent != null) {
        final ExecutableElement found = method(parent, name);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private Expr navigate(final String path, final String kind) {
    final String[] segments = path.split("\\.");
    Expr expr = TABLE;
    for (int i = 0; i < segments.length; i++) {
      if (i == segments.length - 1 && !"INNER".equals(kind)) {
        expr = expr.invoke(segments[i], Exprs.staticField(JoinType.class, kind));
      } else {
        expr = expr.invoke(segments[i]);
      }
    }
    return expr;
  }

  private void error(final Element element, final String message) {
    Diagnostics.error(messager, element, message);
  }
}
