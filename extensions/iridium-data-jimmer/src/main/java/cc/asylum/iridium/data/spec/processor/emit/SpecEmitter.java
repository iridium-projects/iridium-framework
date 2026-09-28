package cc.asylum.iridium.data.spec.processor.emit;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.write.Body;
import cc.asylum.iridium.codegen.code.Blocks;
import cc.asylum.iridium.codegen.model.Diagnostics;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.code.Types;
import cc.asylum.iridium.core.util.Lists;
import cc.asylum.iridium.core.util.Strings;
import cc.asylum.iridium.web.processor.binding.convert.RequestValues;
import org.babyfish.jimmer.sql.ast.Predicate;

import javax.annotation.processing.Messager;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;
import cc.asylum.iridium.data.spec.processor.node.SpecAnd;
import cc.asylum.iridium.data.spec.processor.node.SpecLeaf;
import cc.asylum.iridium.data.spec.processor.node.SpecNode;
import cc.asylum.iridium.data.spec.processor.node.SpecNot;
import cc.asylum.iridium.data.spec.processor.node.SpecOr;
import cc.asylum.iridium.data.spec.processor.parse.SpecOps;
import cc.asylum.iridium.data.spec.processor.parse.SpecTypes;

public final class SpecEmitter {

  private final SpecTypes types;
  private final SpecPredicates predicates;
  private final SpecLiterals literals;
  private final RequestValues values;
  private final Messager messager;
  private int names;

  public SpecEmitter(
      final SpecTypes types,
      final SpecPredicates predicates,
      final SpecLiterals literals,
      final RequestValues values,
      final Messager messager
  ) {
    this.types = types;
    this.predicates = predicates;
    this.literals = literals;
    this.values = values;
    this.messager = messager;
  }

  public boolean emit(final Body body, final VariableElement parameter, final SpecNode node, final List<Expr> into) {
    return !switch (node) {
      case SpecLeaf leaf -> emitLeaf(body, parameter, leaf, into);
      case SpecAnd and -> emitGroup(body, parameter, "and", and.children(), into);
      case SpecOr or -> emitGroup(body, parameter, "or", or.children(), into);
      case SpecNot not -> {
        final List<Expr> inner = new ArrayList<>();
        if (emit(body, parameter, not.child(), inner)) {
          yield false;
        }
        final Expr child = Lists.first(inner);
        into.add(predicates.not(child == null ? Expr.nil() : child));
        yield true;
      }
    };
  }

  private boolean emitGroup(
      final Body body,
      final VariableElement parameter,
      final String operator,
      final List<SpecNode> children,
      final List<Expr> into
  ) {
    final List<Expr> parts = new ArrayList<>();
    for (final SpecNode child : children) {
      if (emit(body, parameter, child, parts)) {
        return false;
      }
    }
    if (parts.isEmpty()) {
       error(parameter, "@" + Strings.capitalize(operator) + " requires at least one spec");
      return false;
    }
    into.add(parts.size() == 1 ? parts.get(0) : Exprs.invokeStatic(Predicate.class, operator, parts));
    return true;
  }

  private boolean emitLeaf(
      final Body body,
      final VariableElement parameter,
      final SpecLeaf leaf,
      final List<Expr> into
  ) {
    if (leaf.collection()) {
       return emitCollection(body, parameter, leaf, into);
    }
    if (!leaf.constVal().isEmpty()) {
      if (SpecOps.flag(leaf.op())) {
        final Boolean apply = Strings.truthy(leaf.constVal());
        if (apply == null) {
           error(parameter, "invalid boolean constVal '" + leaf.constVal() + "'");
          return false;
        }
        into.add(apply ? predicates.wrap(leaf, predicates.call(leaf, Expr.nil())) : Expr.nil());
        return true;
      }
      final Expr literal = literals.constLiteral(parameter, leaf, leaf.constVal());
      if (literal == null) {
        return false;
      }
      into.add(predicates.wrap(leaf, predicates.call(leaf, literal)));
      return true;
    }
    if (leaf.params().size() == 2) {
       return emitBetween(body, parameter, leaf, into);
    }
    final String raw = next("raw");
    body.add(Blocks.declare(String.class, raw, read(leaf, leaf.params().get(0), false)));
    if (SpecOps.flag(leaf.op())) {
      into.add(predicates.wrap(leaf, predicates.flagCall(leaf, Exprs.name(raw))));
      return true;
    }
    final String value = next("value");
    if (!assign(body, parameter, leaf, raw, value, leaf.param())) {
      return false;
    }
    into.add(predicates.wrap(leaf, predicates.call(leaf, Exprs.name(value))));
    return true;
  }

  private boolean emitBetween(
      final Body body,
      final VariableElement parameter,
      final SpecLeaf leaf,
      final List<Expr> into
  ) {
    final String minRaw = next("raw");
    final String maxRaw = next("raw");
    final String min = next("value");
    final String max = next("value");
    body.add(Blocks.declare(String.class, minRaw, read(leaf, leaf.params().get(0), false)));
    body.add(Blocks.declare(String.class, maxRaw, read(leaf, leaf.params().get(1), false)));
    if (!assign(body, parameter, leaf, minRaw, min, leaf.params().get(0))) {
      return false;
    }
    if (!assign(body, parameter, leaf, maxRaw, max, leaf.params().get(1))) {
      return false;
    }
    into.add(predicates.wrap(leaf, leaf.expr().invoke("betweenIf", Exprs.name(min), Exprs.name(max))));
    return true;
  }

  private boolean emitCollection(
      final Body body,
      final VariableElement parameter,
      final SpecLeaf leaf,
      final List<Expr> into
  ) {
    final String raw = next("raw");
    final String value = next("value");
    body.add(Blocks.declare(Types.list(cc.asylum.forgery.model.TypeRef.STRING), raw, Exprs.invokeStatic(SpecValues.class, "parts", read(leaf, leaf.param(), true))));
    final Expr call = predicates.collectionCall(leaf, Exprs.name(raw));
    if (call == null) {
       error(parameter, "unsupported spec property type " + leaf.type());
      return false;
    }
    body.add(Blocks.declare(types.listType(leaf.type()), value));
    body.add(Blocks.tryCatch(
        List.of(Blocks.assign(value, call)),
        IllegalArgumentException.class,
        "ignored",
        List.of(Blocks.ret(values.invalid(null, leaf.param())))));
    final String method = "NotIn".equals(leaf.op()) ? "notInIf" : "inIf";
    into.add(predicates.wrap(leaf, leaf.expr().invoke(method, Exprs.name(value))));
    return true;
  }

  private boolean assign(
      final Body body,
      final VariableElement parameter,
      final SpecLeaf leaf,
      final String raw,
      final String value,
      final String param
  ) {
    final Expr call = predicates.parseCall(leaf, Exprs.name(raw));
    if (call == null) {
       error(parameter, "unsupported spec property type " + leaf.type());
      return false;
    }
    body.add(Blocks.declare(Types.of(types.boxed(leaf.type())), value));
    body.add(Blocks.tryCatch(
        List.of(Blocks.assign(value, call)),
        IllegalArgumentException.class,
        "ignored",
        List.of(Blocks.ret(values.invalid(Exprs.name(raw), param)))));
    return true;
  }

  private Expr read(final SpecLeaf leaf, final String name, final boolean many) {
    if (many) {
      return values.many(leaf.header(), name);
    }
    final Expr fallback = leaf.defaultVal().isEmpty() ? Expr.nil() : Expr.lit(leaf.defaultVal());
    return values.one(leaf.header(), name, fallback);
  }

  private String next(final String prefix) {
    return prefix + names++;
  }

  private void error(final VariableElement parameter, final String message) {
    Diagnostics.error(messager, parameter, message);
  }
}
