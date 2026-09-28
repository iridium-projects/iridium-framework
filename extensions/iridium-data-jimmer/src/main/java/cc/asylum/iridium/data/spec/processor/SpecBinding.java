package cc.asylum.iridium.data.spec.processor;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.model.TypeRef;
import cc.asylum.iridium.codegen.write.Body;
import cc.asylum.iridium.codegen.code.Blocks;
import cc.asylum.iridium.codegen.model.Diagnostics;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.code.Types;
import cc.asylum.iridium.web.processor.binding.ParameterBinder;
import cc.asylum.iridium.web.processor.binding.convert.RequestValues;
import org.babyfish.jimmer.sql.ast.query.specification.JSpecification;
import org.babyfish.jimmer.sql.ast.query.specification.SpecificationArgs;

import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import cc.asylum.iridium.data.spec.And;
import cc.asylum.iridium.data.spec.Join;
import cc.asylum.iridium.data.spec.Joins;
import cc.asylum.iridium.data.spec.Not;
import cc.asylum.iridium.data.spec.Or;
import cc.asylum.iridium.data.spec.Spec;
import cc.asylum.iridium.data.spec.processor.emit.SpecEmitter;
import cc.asylum.iridium.data.spec.processor.emit.SpecLiterals;
import cc.asylum.iridium.data.spec.processor.emit.SpecPredicates;
import cc.asylum.iridium.data.spec.processor.node.SpecJoin;
import cc.asylum.iridium.data.spec.processor.node.SpecNode;
import cc.asylum.iridium.data.spec.processor.parse.SpecMirrors;
import cc.asylum.iridium.data.spec.processor.parse.SpecParser;
import cc.asylum.iridium.data.spec.processor.parse.SpecPaths;
import cc.asylum.iridium.data.spec.processor.parse.SpecTypes;

final class SpecBinding implements ParameterBinder {

  private final SpecTypes types;
  private final SpecMirrors mirrors;
  private final SpecPaths paths;
  private final SpecParser parser;
  private final SpecEmitter emitter;
  private final Messager messager;

  SpecBinding(
      final javax.lang.model.util.Types types,
      final Elements elements,
      final Messager messager,
      final RequestValues values
  ) {
    this.types = new SpecTypes(types, elements);
    this.mirrors = new SpecMirrors(elements);
    this.paths = new SpecPaths(this.types, this.mirrors, messager);
    this.parser = new SpecParser(this.types, this.mirrors, this.paths, messager);
    this.emitter = new SpecEmitter(
        this.types,
        new SpecPredicates(this.types),
        new SpecLiterals(this.types, messager),
        values,
        messager);
    this.messager = messager;
  }

  @Override
  public boolean matches(final VariableElement parameter) {
    return mirrors.first(parameter, Spec.class) != null
        || mirrors.first(parameter, And.class) != null
        || mirrors.first(parameter, Or.class) != null
        || mirrors.first(parameter, Not.class) != null
        || mirrors.first(parameter, Join.class) != null
        || mirrors.first(parameter, Joins.class) != null
        || types.assignable(parameter.asType(), JSpecification.class);
  }

  @Override
  public Optional<Expr> emit(final Body body, final VariableElement parameter) {
    final TypeMirror entity = types.entityArgument(parameter.asType());
    if (entity == null) {
      error(parameter, "spec parameter must be " + SpecTypes.JSPEC + "<Entity, ?>");
      return Optional.empty();
    }

    final TypeElement entityElement = types.asType(entity);
    if (entityElement == null) {
      error(parameter, "spec entity type must be a declared type");
      return Optional.empty();
    }

    if (!types.assignable(parameter.asType(), JSpecification.class)) {
      error(parameter, "spec parameter must be " + SpecTypes.JSPEC);
      return Optional.empty();
    }

    final Map<String, SpecJoin> joins = paths.joins(parameter, entityElement);
    if (joins == null) {
      return Optional.empty();
    }

    final SpecNode node = parser.parse(parameter, entityElement, joins);
    if (node == null) {
      return Optional.empty();
    }


    final List<Expr> predicates = new ArrayList<>();
    if (emitter.emit(body, parameter, node, predicates)) {
      return Optional.empty();
    }

    if (predicates.isEmpty()) {
      error(parameter, "spec produced no predicate");
      return Optional.empty();
    }

    final TypeRef entityName = Types.of(entityElement);
    final TypeRef tableType = Types.of(types.tableName(entityElement));
    final TypeRef implemented = Types.parameterized(JSpecification.class, entityName, tableType);
    final String variable = parameter.getSimpleName().toString();
    final TypeRef nested = body.nest(type -> {

      type.implements_(implemented);
      type.method("entityType", method -> {
        method.public_().overrides();
        method.returns(Types.parameterized(Class.class, entityName));
        method.body(block -> block.return_(Exprs.classLit(entityName)));
      });

      type.method("applyTo", method -> {
        method.public_().overrides();
        method.parameter(Types.parameterized(SpecificationArgs.class, entityName, tableType), "args");
        method.body(block -> {
          block.add(Blocks.declare(tableType, "table", Exprs.name("args").invoke("getTable")));
          block.add(Blocks.expr(Exprs.name("args").invoke("where", predicates.get(0))));
        });
      });

    });
    body.add(Blocks.declare(Types.of(parameter.asType()), variable, Exprs.new_(nested)));
    return Optional.of(Exprs.name(variable));
  }

  private void error(final VariableElement parameter, final String message) {
    Diagnostics.error(messager, parameter, message);
  }
}
