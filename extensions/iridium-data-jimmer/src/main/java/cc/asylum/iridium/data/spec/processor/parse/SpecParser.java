package cc.asylum.iridium.data.spec.processor.parse;

import cc.asylum.iridium.codegen.model.Diagnostics;
import cc.asylum.iridium.core.util.Strings;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import cc.asylum.iridium.data.spec.And;
import cc.asylum.iridium.data.spec.Not;
import cc.asylum.iridium.data.spec.Or;
import cc.asylum.iridium.data.spec.Spec;
import cc.asylum.iridium.data.spec.processor.node.SpecAnd;
import cc.asylum.iridium.data.spec.processor.node.SpecJoin;
import cc.asylum.iridium.data.spec.processor.node.SpecLeaf;
import cc.asylum.iridium.data.spec.processor.node.SpecNode;
import cc.asylum.iridium.data.spec.processor.node.SpecNot;
import cc.asylum.iridium.data.spec.processor.node.SpecOr;
import cc.asylum.iridium.data.spec.processor.node.SpecResolved;

public final class SpecParser {

  private final SpecTypes types;
  private final SpecMirrors mirrors;
  private final SpecPaths paths;
  private final Messager messager;

  public SpecParser(
      final SpecTypes types,
      final SpecMirrors mirrors,
      final SpecPaths paths,
      final Messager messager
  ) {
    this.types = types;
    this.mirrors = mirrors;
    this.paths = paths;
    this.messager = messager;
  }

  public SpecNode parse(
      final VariableElement parameter,
      final TypeElement entity,
      final Map<String, SpecJoin> joins
  ) {
    final boolean negate = mirrors.first(parameter, Not.class) != null;
    final AnnotationMirror or = mirrors.first(parameter, Or.class);
    final AnnotationMirror and = mirrors.first(parameter, And.class);
    final List<AnnotationMirror> specs = mirrors.all(parameter, Spec.class);
    SpecNode node;
    if (or != null && and != null) {
      final SpecNode orNode = group(parameter, or, entity, joins, true);
      final SpecNode andNode = group(parameter, and, entity, joins, false);
      if (orNode == null || andNode == null) {
        return null;
      }
      node = new SpecAnd(List.of(andNode, orNode));
    } else if (or != null) {
      node = group(parameter, or, entity, joins, true);
    } else if (and != null) {
      node = group(parameter, and, entity, joins, false);
    } else if (!specs.isEmpty()) {
      final List<SpecNode> leaves = new ArrayList<>();
      for (final AnnotationMirror spec : specs) {
        final SpecNode leaf = leaf(parameter, spec, entity, joins);
        if (leaf == null) {
          return null;
        }
        leaves.add(leaf);
      }
      node = leaves.size() == 1 ? leaves.get(0) : new SpecAnd(leaves);
    } else {
       error(parameter, "spec parameter requires @Spec, @And or @Or");
      return null;
    }
    if (node == null) {
      return null;
    }
     return negate ? new SpecNot(node) : node;
  }

  private SpecNode group(
      final VariableElement parameter,
      final AnnotationMirror mirror,
      final TypeElement entity,
      final Map<String, SpecJoin> joins,
      final boolean or
  ) {
    final List<SpecNode> children = new ArrayList<>();
    for (final AnnotationMirror spec : mirrors.nested(mirror, "value")) {
      final SpecNode leaf = leaf(parameter, spec, entity, joins);
      if (leaf == null) {
        return null;
      }
      children.add(leaf);
    }
    if (children.isEmpty()) {
       error(parameter, (or ? "@Or" : "@And") + " requires at least one spec");
      return null;
    }
     return or ? new SpecOr(children) : new SpecAnd(children);
  }

  private SpecNode leaf(
      final VariableElement parameter,
      final AnnotationMirror mirror,
      final TypeElement entity,
      final Map<String, SpecJoin> joins
  ) {
    final String path = mirrors.string(mirror, "path");
    if (!types.validPath(path)) {
       error(parameter, "invalid spec path '" + path + "'");
      return null;
    }
    final String op = SpecOps.name(mirrors.member(mirror, "spec"));
    if (op == null) {
       error(parameter, "@Spec requires spec()");
      return null;
    }
    if (!SpecOps.known(op)) {
       error(parameter, "unknown spec '" + op + "'");
      return null;
    }
    final List<String> params = mirrors.strings(mirror, "params");
    final List<String> headers = mirrors.strings(mirror, "headers");
    if (!params.isEmpty() && !headers.isEmpty()) {
       error(parameter, "@Spec cannot read both params and headers");
      return null;
    }
    final boolean header = !headers.isEmpty();
    final List<String> names = header ? headers : params;
    final SpecResolved resolved = paths.resolve(parameter, entity, path, joins);
    if (resolved == null) {
      return null;
    }
    if (!SpecOps.compatible(op, resolved.type(), types)) {
       error(parameter, op + " is not valid for " + resolved.type());
      return null;
    }
    final List<String> bound = names.isEmpty() ? defaults(op, path) : names;
    if (SpecOps.between(op) && bound.size() != 2) {
       error(parameter, "Between requires two params");
      return null;
    }
    if (!SpecOps.between(op) && !SpecOps.collection(op) && bound.size() > 1) {
       error(parameter, op + " accepts a single param");
      return null;
    }
    for (final String name : bound) {
      if (!types.validName(name)) {
         error(parameter, "invalid spec param '" + name + "'");
        return null;
      }
    }
    final String constVal = mirrors.string(mirror, "constVal").trim();
    if (!constVal.isEmpty() && (SpecOps.between(op) || SpecOps.collection(op))) {
       error(parameter, op + " does not support constVal");
      return null;
    }
     return new SpecLeaf(
        resolved.expr(),
        resolved.type(),
        op,
        bound,
        header,
        constVal,
        mirrors.string(mirror, "defaultVal"),
        mirrors.bool(mirror, "not"),
        SpecOps.collection(op)
    );
  }

  private List<String> defaults(final String op, final String path) {
    final String leaf = Strings.simpleName(path);
    if (SpecOps.between(op)) {
      return List.of(leaf + "From", leaf + "To");
    }
    return List.of(leaf);
  }

  private void error(final VariableElement parameter, final String message) {
    Diagnostics.error(messager, parameter, message);
  }
}
