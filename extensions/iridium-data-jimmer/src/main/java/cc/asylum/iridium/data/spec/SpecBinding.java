package cc.asylum.iridium.data.spec;

import com.io7m.jodist.ClassName;
import com.io7m.jodist.MethodSpec;
import com.io7m.jodist.ParameterizedTypeName;
import com.io7m.jodist.TypeName;
import com.io7m.jodist.TypeSpec;
import cc.asylum.iridium.codegen.binding.ParameterBinder;
import cc.asylum.iridium.codegen.binding.RequestValues;
import cc.asylum.iridium.codegen.support.Diagnostics;
import cc.asylum.iridium.codegen.support.TypeSupport;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.specification.JSpecification;
import org.babyfish.jimmer.sql.ast.query.specification.SpecificationArgs;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.lang.annotation.Annotation;
import java.util.regex.Pattern;

final class SpecBinding implements ParameterBinder {

  private static final String JSPEC = JSpecification.class.getCanonicalName();
  private static final String PREDICATE = Predicate.class.getCanonicalName();
  private static final String VALUES = SpecValues.class.getCanonicalName();
  private static final String LIKE_MODE = LikeMode.class.getCanonicalName();
  private static final String JOIN_TYPE = JoinType.class.getCanonicalName();
  private static final ClassName SPEC_TYPE = ClassName.get(JSpecification.class);
  private static final ClassName ARGS_TYPE = ClassName.get(SpecificationArgs.class);
  private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

  private final Types types;
  private final Elements elements;
  private final Messager messager;
  private final RequestValues values;
  private int names;

  SpecBinding(
      final Types types,
      final Elements elements,
      final Messager messager,
      final RequestValues values
  ) {
    this.types = types;
    this.elements = elements;
    this.messager = messager;
    this.values = values;
  }

  @Override
  public boolean matches(final VariableElement parameter) {
    return mirror(parameter, Spec.class) != null
        || mirror(parameter, And.class) != null
        || mirror(parameter, Or.class) != null
        || mirror(parameter, Not.class) != null
        || mirror(parameter, Join.class) != null
        || mirror(parameter, Joins.class) != null
        || TypeSupport.isAssignable(types, elements, types.erasure(parameter.asType()), JSpecification.class);
  }

  @Override
  public Optional<String> emit(final MethodSpec.Builder handle, final VariableElement parameter) {
    final TypeMirror entity = entityType(parameter);
    if (entity == null) {
      return Optional.empty();
    }
    final TypeElement entityElement = asType(entity);
    if (entityElement == null) {
      error(parameter, "spec entity type must be a declared type");
      return Optional.empty();
    }

    final Map<String, JoinDef> joins = joins(parameter, entityElement);
    if (joins == null) {
      return Optional.empty();
    }
    final Node node = node(parameter, entityElement, joins);
    if (node == null) {
      return Optional.empty();
    }

    final List<String> predicates = new ArrayList<>();
    if (!emitNode(handle, parameter, node, predicates)) {
      return Optional.empty();
    }
    if (predicates.isEmpty()) {
      error(parameter, "spec produced no predicate");
      return Optional.empty();
    }
    final String combined = predicates.get(0);

    final String table = tableName(entityElement);
    final ClassName entityName = ClassName.get(entityElement);
    final ClassName tableName = ClassName.bestGuess(table);
    final TypeName implemented = ParameterizedTypeName.get(SPEC_TYPE, entityName, tableName);
    final String variable = parameter.getSimpleName().toString();
    final TypeSpec anonymous = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(implemented)
        .addMethod(MethodSpec.methodBuilder("entityType")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(ParameterizedTypeName.get(ClassName.get(Class.class), entityName))
            .addStatement("return $T.class", entityName)
            .build())
        .addMethod(MethodSpec.methodBuilder("applyTo")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ParameterizedTypeName.get(ARGS_TYPE, entityName, tableName), "args")
            .addStatement("final $T table = args.getTable()", tableName)
            .addStatement("args.where($L)", combined)
            .build())
        .build();
    handle.addStatement("$L $L = $L", parameter.asType().toString(), variable, anonymous);
    return Optional.of(variable);
  }

  private boolean emitNode(
      final MethodSpec.Builder handle,
      final VariableElement parameter,
      final Node node,
      final List<String> into
  ) {
    return switch (node) {
      case LeafNode leaf -> emitLeaf(handle, parameter, leaf, into);
      case AndNode and -> emitGroup(handle, parameter, "and", and.children(), into);
      case OrNode or -> emitGroup(handle, parameter, "or", or.children(), into);
      case NotNode not -> {
        final List<String> inner = new ArrayList<>();
        if (!emitNode(handle, parameter, not.child(), inner)) {
          yield false;
        }
        into.add(not(inner.isEmpty() ? "null" : inner.get(0)));
        yield true;
      }
    };
  }

  private boolean emitGroup(
      final MethodSpec.Builder handle,
      final VariableElement parameter,
      final String operator,
      final List<Node> children,
      final List<String> into
  ) {
    final List<String> parts = new ArrayList<>();
    for (final Node child : children) {
      if (!emitNode(handle, parameter, child, parts)) {
        return false;
      }
    }
    if (parts.isEmpty()) {
      error(parameter, "@" + operator.substring(0, 1).toUpperCase(Locale.ROOT) + operator.substring(1) + " requires at least one spec");
      return false;
    }
    into.add(parts.size() == 1 ? parts.get(0) : PREDICATE + "." + operator + "(" + String.join(", ", parts) + ")");
    return true;
  }

  private boolean emitLeaf(
      final MethodSpec.Builder handle,
      final VariableElement parameter,
      final LeafNode leaf,
      final List<String> into
  ) {
    if (leaf.collection()) {
      return emitCollection(handle, parameter, leaf, into);
    }
    if (!leaf.constVal().isEmpty()) {
      if (flag(leaf.op())) {
        final Boolean apply = truthy(leaf.constVal());
        if (apply == null) {
          error(parameter, "invalid boolean constVal '" + leaf.constVal() + "'");
          return false;
        }
        into.add(apply ? wrap(leaf, call(leaf, "null")) : "null");
        return true;
      }
      final String literal = constLiteral(parameter, leaf, leaf.constVal());
      if (literal == null) {
        return false;
      }
      into.add(wrap(leaf, call(leaf, literal)));
      return true;
    }
    if (leaf.params().size() == 2) {
      return emitBetween(handle, parameter, leaf, into);
    }
    final String raw = next("raw");
    handle.addStatement("final java.lang.String $L = $L", raw, read(leaf, leaf.params().get(0), false));
    if (flag(leaf.op())) {
      into.add(wrap(leaf, flagCall(leaf, raw)));
      return true;
    }
    final String value = next("value");
    if (!assign(handle, parameter, leaf, raw, value, leaf.param())) {
      return false;
    }
    into.add(wrap(leaf, call(leaf, value)));
    return true;
  }

  private boolean emitBetween(
      final MethodSpec.Builder handle,
      final VariableElement parameter,
      final LeafNode leaf,
      final List<String> into
  ) {
    final String minRaw = next("raw");
    final String maxRaw = next("raw");
    final String min = next("value");
    final String max = next("value");
    handle.addStatement("final java.lang.String $L = $L", minRaw, read(leaf, leaf.params().get(0), false));
    handle.addStatement("final java.lang.String $L = $L", maxRaw, read(leaf, leaf.params().get(1), false));
    if (!assign(handle, parameter, leaf, minRaw, min, leaf.params().get(0))) {
      return false;
    }
    if (!assign(handle, parameter, leaf, maxRaw, max, leaf.params().get(1))) {
      return false;
    }
    into.add(wrap(leaf, leaf.expr() + ".betweenIf(" + min + ", " + max + ")"));
    return true;
  }

  private boolean emitCollection(
      final MethodSpec.Builder handle,
      final VariableElement parameter,
      final LeafNode leaf,
      final List<String> into
  ) {
    final String raw = next("raw");
    final String value = next("value");
    handle.addStatement("final java.util.List<java.lang.String> $L = $L.parts($L)", raw, VALUES, read(leaf, leaf.param(), true));
    final String listType = listType(leaf.type());
    final String call = collectionCall(leaf, raw);
    if (call == null) {
      error(parameter, "unsupported spec property type " + leaf.type());
      return false;
    }
    handle.addStatement("final $L $L", listType, value);
    handle.addCode("""
        try {
          $L = $L;
        } catch (final IllegalArgumentException ignored) {
          return $L;
        }
        """, value, call, values.invalid(null, leaf.param()));
    final String method = "NotIn".equals(leaf.op()) ? "notInIf" : "inIf";
    into.add(wrap(leaf, leaf.expr() + "." + method + "(" + value + ")"));
    return true;
  }

  private boolean assign(
      final MethodSpec.Builder handle,
      final VariableElement parameter,
      final LeafNode leaf,
      final String raw,
      final String value,
      final String param
  ) {
    final String parser = parser(leaf.type());
    if (parser == null) {
      error(parameter, "unsupported spec property type " + leaf.type());
      return false;
    }
    final String call = "enumeration".equals(parser)
        ? VALUES + ".enumeration(" + raw + ", " + qualified(leaf.type()) + ".class)"
        : VALUES + "." + parser + "(" + raw + ")";
    handle.addStatement("final $L $L", boxed(leaf.type()), value);
    handle.addCode("""
        try {
          $L = $L;
        } catch (final IllegalArgumentException ignored) {
          return $L;
        }
        """, value, call, values.invalid(raw, param));
    return true;
  }

  private String constLiteral(final Element site, final LeafNode leaf, final String raw) {
    if (flag(leaf.op())) {
      return null;
    }
    final String type = qualified(leaf.type());
    if (isEnum(leaf.type())) {
      if (constant(asType(leaf.type()), raw) == null) {
        error(site, "unknown enum constant " + type + "." + raw);
        return null;
      }
      return type + "." + raw;
    }
    if (isString(leaf.type())) {
      return quote(raw);
    }
    if ("java.lang.Boolean".equals(boxed(leaf.type())) || leaf.type().getKind() == TypeKind.BOOLEAN) {
      return switch (raw.trim().toLowerCase(Locale.ROOT)) {
        case "true", "yes", "on", "1" -> "true";
        case "false", "no", "off", "0" -> "false";
        default -> {
          error(site, "invalid boolean constVal '" + raw + "'");
          yield null;
        }
      };
    }
    try {
      return switch (boxed(leaf.type())) {
        case "java.lang.Integer" -> Integer.toString(Integer.parseInt(raw.trim()));
        case "java.lang.Long" -> Long.parseLong(raw.trim()) + "L";
        case "java.lang.Double" -> Double.toString(Double.parseDouble(raw.trim())) + "d";
        case "java.lang.Float" -> Float.parseFloat(raw.trim()) + "f";
        case "java.lang.Short" -> "(short) " + Short.parseShort(raw.trim());
        case "java.lang.Byte" -> "(byte) " + Byte.parseByte(raw.trim());
        case "java.math.BigDecimal" -> "new java.math.BigDecimal(" + quote(raw.trim()) + ")";
        case "java.math.BigInteger" -> "new java.math.BigInteger(" + quote(raw.trim()) + ")";
        case "java.util.UUID" -> "java.util.UUID.fromString(" + quote(raw.trim()) + ")";
        case "java.time.LocalDate" -> "java.time.LocalDate.parse(" + quote(raw.trim()) + ")";
        case "java.time.LocalDateTime" -> "java.time.LocalDateTime.parse(" + quote(raw.trim()) + ")";
        case "java.time.LocalTime" -> "java.time.LocalTime.parse(" + quote(raw.trim()) + ")";
        case "java.time.Instant" -> "java.time.Instant.parse(" + quote(raw.trim()) + ")";
        case "java.time.OffsetDateTime" -> "java.time.OffsetDateTime.parse(" + quote(raw.trim()) + ")";
        case "java.time.ZonedDateTime" -> "java.time.ZonedDateTime.parse(" + quote(raw.trim()) + ")";
        default -> {
          error(site, "unsupported constVal for " + type);
          yield null;
        }
      };
    } catch (final RuntimeException exception) {
      error(site, "invalid constVal '" + raw + "'");
      return null;
    }
  }

  private Boolean truthy(final String raw) {
    return switch (raw.trim().toLowerCase(Locale.ROOT)) {
      case "true", "yes", "on", "1" -> true;
      case "false", "no", "off", "0" -> false;
      default -> null;
    };
  }

  private String call(final LeafNode leaf, final String value) {
    final String expr = leaf.expr();
    return switch (leaf.op()) {
      case "Equal" -> expr + ".eqIf(" + value + ")";
      case "NotEqual" -> expr + ".neIf(" + value + ")";
      case "EqualIgnoreCase" -> expr + ".ilikeIf(" + value + ", " + LIKE_MODE + ".EXACT)";
      case "Like" -> expr + ".likeIf(" + value + ", " + LIKE_MODE + ".ANYWHERE)";
      case "LikeIgnoreCase" -> expr + ".ilikeIf(" + value + ", " + LIKE_MODE + ".ANYWHERE)";
      case "NotLike" -> not(expr + ".likeIf(" + value + ", " + LIKE_MODE + ".ANYWHERE)");
      case "StartingWith" -> expr + ".likeIf(" + value + ", " + LIKE_MODE + ".START)";
      case "EndingWith" -> expr + ".likeIf(" + value + ", " + LIKE_MODE + ".END)";
      case "GreaterThan" -> expr + ".gtIf(" + value + ")";
      case "GreaterThanOrEqual" -> expr + ".geIf(" + value + ")";
      case "LessThan" -> expr + ".ltIf(" + value + ")";
      case "LessThanOrEqual" -> expr + ".leIf(" + value + ")";
      case "Null" -> expr + ".isNull()";
      case "NotNull" -> expr + ".isNotNull()";
      case "Empty" -> PREDICATE + ".or(" + expr + ".isNull(), " + expr + ".eq(\"\"))";
      case "NotEmpty" -> PREDICATE + ".and(" + expr + ".isNotNull(), " + expr + ".ne(\"\"))";
      case "True" -> expr + ".eq(true)";
      case "False" -> expr + ".eq(false)";
      default -> expr + ".eqIf(" + value + ")";
    };
  }

  private String flagCall(final LeafNode leaf, final String raw) {
    final String predicate = call(leaf, "null");
    return VALUES + ".truthy(" + raw + ") == Boolean.TRUE ? " + predicate + " : null";
  }

  private String collectionCall(final LeafNode leaf, final String raw) {
    if (isEnum(leaf.type())) {
      return VALUES + ".enumerations(" + raw + ", " + qualified(leaf.type()) + ".class)";
    }
    if (isString(leaf.type())) {
      return VALUES + ".texts(" + raw + ")";
    }
    final String parser = switch (boxed(leaf.type())) {
      case "java.lang.Integer" -> "integers";
      case "java.lang.Long" -> "longs";
      case "java.lang.Double" -> "doubles";
      case "java.lang.Float" -> "floats";
      case "java.lang.Short" -> "shorts";
      case "java.lang.Byte" -> "bytes";
      case "java.lang.Boolean" -> "bools";
      case "java.math.BigDecimal" -> "decimals";
      case "java.math.BigInteger" -> "integerBigs";
      case "java.util.UUID" -> "uuids";
      default -> null;
    };
    return parser == null ? null : VALUES + "." + parser + "(" + raw + ")";
  }

  private String read(final LeafNode leaf, final String name, final boolean many) {
    if (many) {
      return values.many(leaf.header(), name);
    }
    final String fallback = leaf.defaultVal().isEmpty() ? "null" : quote(leaf.defaultVal());
    return values.one(leaf.header(), name, fallback);
  }

  private Node node(
      final VariableElement parameter,
      final TypeElement entity,
      final Map<String, JoinDef> joins
  ) {
    final boolean negate = mirror(parameter, Not.class) != null;
    final AnnotationMirror or = mirror(parameter, Or.class);
    final AnnotationMirror and = mirror(parameter, And.class);
    final List<AnnotationMirror> specs = mirrors(parameter, Spec.class);
    Node node;
    if (or != null && and != null) {
      final Node orNode = group(parameter, or, entity, joins, true);
      final Node andNode = group(parameter, and, entity, joins, false);
      if (orNode == null || andNode == null) {
        return null;
      }
      node = new AndNode(List.of(andNode, orNode));
    } else if (or != null) {
      node = group(parameter, or, entity, joins, true);
    } else if (and != null) {
      node = group(parameter, and, entity, joins, false);
    } else if (!specs.isEmpty()) {
      final List<Node> leaves = new ArrayList<>();
      for (final AnnotationMirror spec : specs) {
        final Node leaf = leaf(parameter, spec, entity, joins);
        if (leaf == null) {
          return null;
        }
        leaves.add(leaf);
      }
      node = leaves.size() == 1 ? leaves.get(0) : new AndNode(leaves);
    } else {
      error(parameter, "spec parameter requires @Spec, @And or @Or");
      return null;
    }
    if (node == null) {
      return null;
    }
    return negate ? new NotNode(node) : node;
  }

  private Node group(
      final VariableElement parameter,
      final AnnotationMirror mirror,
      final TypeElement entity,
      final Map<String, JoinDef> joins,
      final boolean or
  ) {
    final List<Node> children = new ArrayList<>();
    for (final AnnotationMirror spec : annotations(mirror, "value")) {
      final Node leaf = leaf(parameter, spec, entity, joins);
      if (leaf == null) {
        return null;
      }
      children.add(leaf);
    }
    if (children.isEmpty()) {
      error(parameter, (or ? "@Or" : "@And") + " requires at least one spec");
      return null;
    }
    return or ? new OrNode(children) : new AndNode(children);
  }

  private Node leaf(
      final VariableElement parameter,
      final AnnotationMirror mirror,
      final TypeElement entity,
      final Map<String, JoinDef> joins
  ) {
    final String path = string(mirror, "path");
    if (!validPath(path)) {
      error(parameter, "invalid spec path '" + path + "'");
      return null;
    }
    final String op = op(mirror);
    if (op == null) {
      error(parameter, "@Spec requires spec()");
      return null;
    }
    if (!known(op)) {
      error(parameter, "unknown spec '" + op + "'");
      return null;
    }
    final List<String> params = strings(mirror, "params");
    final List<String> headers = strings(mirror, "headers");
    if (!params.isEmpty() && !headers.isEmpty()) {
      error(parameter, "@Spec cannot read both params and headers");
      return null;
    }
    final boolean header = !headers.isEmpty();
    final List<String> names = header ? headers : params;
    final Resolved resolved = resolve(parameter, entity, path, joins);
    if (resolved == null) {
      return null;
    }
    if (!compatible(op, resolved.type())) {
      error(parameter, op + " is not valid for " + resolved.type());
      return null;
    }
    final List<String> bound = names.isEmpty() ? defaults(op, path) : names;
    if ("Between".equals(op) && bound.size() != 2) {
      error(parameter, "Between requires two params");
      return null;
    }
    if (!"Between".equals(op) && !"In".equals(op) && !"NotIn".equals(op) && bound.size() > 1) {
      error(parameter, op + " accepts a single param");
      return null;
    }
    for (final String name : bound) {
      if (!IDENTIFIER.matcher(name).matches()) {
        error(parameter, "invalid spec param '" + name + "'");
        return null;
      }
    }
    final String constVal = string(mirror, "constVal").trim();
    if (!constVal.isEmpty() && ("Between".equals(op) || "In".equals(op) || "NotIn".equals(op))) {
      error(parameter, op + " does not support constVal");
      return null;
    }
    return new LeafNode(
        resolved.expr(),
        resolved.type(),
        op,
        bound,
        header,
        constVal,
        string(mirror, "defaultVal"),
        bool(mirror, "not"),
        "In".equals(op) || "NotIn".equals(op)
    );
  }

  private List<String> defaults(final String op, final String path) {
    final String leaf = path.substring(path.lastIndexOf('.') + 1);
    if ("Between".equals(op)) {
      return List.of(leaf + "From", leaf + "To");
    }
    return List.of(leaf);
  }

  private Map<String, JoinDef> joins(final VariableElement parameter, final TypeElement entity) {
    final Map<String, JoinDef> joins = new LinkedHashMap<>();
    final List<AnnotationMirror> mirrors = new ArrayList<>(mirrors(parameter, Join.class));
    final AnnotationMirror container = mirror(parameter, Joins.class);
    if (container != null) {
      mirrors.addAll(annotations(container, "value"));
    }
    for (final AnnotationMirror join : mirrors) {
      final String alias = string(join, "alias");
      final String path = string(join, "path");
      if (!IDENTIFIER.matcher(alias).matches() || !validPath(path)) {
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
      joins.put(alias, new JoinDef(path, kind(join), target));
    }
    return joins;
  }

  private Resolved resolve(
      final VariableElement parameter,
      final TypeElement entity,
      final String path,
      final Map<String, JoinDef> joins
  ) {
    final String[] segments = path.split("\\.");
    String expr = "table";
    TypeMirror current = entity.asType();
    int start = 0;
    final JoinDef aliased = joins.get(segments[0]);
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
      if (!isEntity(next)) {
        error(parameter, "'" + segments[i] + "' is not an association");
        return null;
      }
      expr = expr + "." + segments[i] + "()";
      current = next;
    }
    final String leaf = segments[segments.length - 1];
    final TypeMirror type = property(parameter, current, leaf);
    if (type == null) {
      return null;
    }
    return new Resolved(expr + "." + leaf + "()", type);
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
      if (association && !isEntity(next)) {
        error(site, "'" + segment + "' is not an association");
        return null;
      }
      current = next;
    }
    return current;
  }

  private TypeMirror property(final Element site, final TypeMirror owner, final String name) {
    final TypeElement type = asType(owner);
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
      final TypeElement parent = asType(superType);
      if (parent != null) {
        final ExecutableElement found = method(parent, name);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private String navigate(final String path, final String kind) {
    final String[] segments = path.split("\\.");
    String expr = "table";
    for (int i = 0; i < segments.length; i++) {
      if (i == segments.length - 1 && !"INNER".equals(kind)) {
        expr = expr + "." + segments[i] + "(" + JOIN_TYPE + "." + kind + ")";
      } else {
        expr = expr + "." + segments[i] + "()";
      }
    }
    return expr;
  }

  private TypeMirror entityType(final VariableElement parameter) {
    if (!(parameter.asType() instanceof final DeclaredType declared) || declared.getTypeArguments().isEmpty()) {
      error(parameter, "spec parameter must be " + JSPEC + "<Entity, ?>");
      return null;
    }
    final TypeMirror entity = declared.getTypeArguments().get(0);
    if (entity.getKind() == TypeKind.WILDCARD) {
      error(parameter, "spec parameter must declare an entity type");
      return null;
    }
    if (!TypeSupport.isAssignable(types, elements, types.erasure(parameter.asType()), JSpecification.class)) {
      error(parameter, "spec parameter must be " + JSPEC);
      return null;
    }
    return entity;
  }

  private boolean compatible(final String op, final TypeMirror type) {
    final boolean string = isString(type);
    final boolean bool = type.getKind() == TypeKind.BOOLEAN || "java.lang.Boolean".equals(qualified(type));
    return switch (op) {
      case "Like", "LikeIgnoreCase", "NotLike", "StartingWith", "EndingWith", "EqualIgnoreCase", "Empty", "NotEmpty" -> string;
      case "True", "False" -> bool;
      case "Null", "NotNull" -> true;
      default -> parser(type) != null || isEnum(type) || string;
    };
  }

  private String parser(final TypeMirror type) {
    if (isEnum(type)) {
      return "enumeration";
    }
    if (isString(type)) {
      return "text";
    }
    final TypeKind kind = type.getKind();
    if (kind.isPrimitive()) {
      return switch (kind) {
        case BOOLEAN -> "bool";
        case BYTE -> "byteValue";
        case SHORT -> "shortValue";
        case INT -> "integer";
        case LONG -> "longValue";
        case FLOAT -> "floatValue";
        case DOUBLE -> "doubleValue";
        default -> null;
      };
    }
    return switch (qualified(type)) {
      case "java.lang.Integer" -> "integer";
      case "java.lang.Long" -> "longValue";
      case "java.lang.Double" -> "doubleValue";
      case "java.lang.Float" -> "floatValue";
      case "java.lang.Short" -> "shortValue";
      case "java.lang.Byte" -> "byteValue";
      case "java.lang.Boolean" -> "bool";
      case "java.math.BigDecimal" -> "decimal";
      case "java.math.BigInteger" -> "integerBig";
      case "java.util.UUID" -> "uuid";
      case "java.time.LocalDate" -> "localDate";
      case "java.time.LocalDateTime" -> "localDateTime";
      case "java.time.LocalTime" -> "localTime";
      case "java.time.Instant" -> "instant";
      case "java.time.OffsetDateTime" -> "offsetDateTime";
      case "java.time.ZonedDateTime" -> "zonedDateTime";
      default -> null;
    };
  }

  private String listType(final TypeMirror type) {
    final String element = isEnum(type) || !type.getKind().isPrimitive()
        ? qualified(type)
        : boxed(type);
    return "java.util.List<" + element + ">";
  }

  private String boxed(final TypeMirror type) {
    return switch (type.getKind()) {
      case BOOLEAN -> "java.lang.Boolean";
      case BYTE -> "java.lang.Byte";
      case SHORT -> "java.lang.Short";
      case INT -> "java.lang.Integer";
      case LONG -> "java.lang.Long";
      case FLOAT -> "java.lang.Float";
      case DOUBLE -> "java.lang.Double";
      case CHAR -> "java.lang.Character";
      default -> qualified(type);
    };
  }

  private boolean isString(final TypeMirror type) {
    final String name = qualified(type);
    return "java.lang.String".equals(name) || "java.lang.CharSequence".equals(name);
  }

  private boolean isEnum(final TypeMirror type) {
    final TypeElement element = asType(type);
    return element != null && element.getKind() == ElementKind.ENUM;
  }

  private boolean isEntity(final TypeMirror type) {
    final TypeElement element = asType(type);
    if (element == null) {
      return false;
    }
    for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {
      if (qualified(mirror).equals(org.babyfish.jimmer.sql.Entity.class.getCanonicalName())) {
        return true;
      }
    }
    return false;
  }

  private String tableName(final TypeElement entity) {
    final String pkg = elements.getPackageOf(entity).getQualifiedName().toString();
    final String simple = entity.getSimpleName() + "Table";
    return pkg.isEmpty() ? simple : pkg + "." + simple;
  }

  private String op(final AnnotationMirror mirror) {
    final Object value = member(mirror, "spec");
    if (!(value instanceof final TypeMirror type)) {
      return null;
    }
    final String name = type.toString();
    final int dot = name.lastIndexOf('.');
    return canonical(dot < 0 ? name : name.substring(dot + 1));
  }

  private String canonical(final String name) {
    return switch (name) {
      case "IsNull" -> "Null";
      case "IsNotNull" -> "NotNull";
      case "StartsWith" -> "StartingWith";
      case "EndsWith" -> "EndingWith";
      default -> name;
    };
  }

  private boolean known(final String op) {
    return switch (op) {
      case "Equal", "NotEqual", "EqualIgnoreCase", "Like", "LikeIgnoreCase", "NotLike",
          "GreaterThan", "GreaterThanOrEqual", "LessThan", "LessThanOrEqual",
          "In", "NotIn", "Null", "NotNull", "Between", "StartingWith", "EndingWith",
          "Empty", "NotEmpty", "True", "False" -> true;
      default -> false;
    };
  }

  private boolean flag(final String op) {
    return switch (op) {
      case "Null", "NotNull", "Empty", "NotEmpty", "True", "False" -> true;
      default -> false;
    };
  }

  private String kind(final AnnotationMirror mirror) {
    final Object value = member(mirror, "type");
    if (value instanceof final VariableElement variable) {
      return variable.getSimpleName().toString();
    }
    return value == null ? "INNER" : value.toString();
  }

  private String wrap(final LeafNode leaf, final String predicate) {
    return leaf.not() ? not(predicate) : predicate;
  }

  private String not(final String predicate) {
    return PREDICATE + ".not(" + predicate + ")";
  }

  private String next(final String prefix) {
    return prefix + names++;
  }

  private boolean validPath(final String path) {
    if (path == null || path.isEmpty()) {
      return false;
    }
    for (final String segment : path.split("\\.", -1)) {
      if (!IDENTIFIER.matcher(segment).matches()) {
        return false;
      }
    }
    return true;
  }

  private String quote(final String value) {
    return "\"" + value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        + "\"";
  }

  private AnnotationMirror mirror(final Element element, final Class<? extends Annotation> type) {
    final List<AnnotationMirror> found = mirrors(element, type);
    return found.isEmpty() ? null : found.get(0);
  }

  private List<AnnotationMirror> mirrors(final Element element, final Class<? extends Annotation> type) {
    final String name = type.getCanonicalName();
    final List<AnnotationMirror> found = new ArrayList<>();
    for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {
      if (qualified(mirror).equals(name)) {
        found.add(mirror);
      }
    }
    return found;
  }

  private List<AnnotationMirror> annotations(final AnnotationMirror mirror, final String member) {
    final Object value = member(mirror, member);
    if (!(value instanceof final List<?> list)) {
      return List.of();
    }
    final List<AnnotationMirror> mirrors = new ArrayList<>();
    for (final Object item : list) {
      final Object unwrapped = item instanceof final AnnotationValue annotation ? annotation.getValue() : item;
      if (unwrapped instanceof final AnnotationMirror nested) {
        mirrors.add(nested);
      }
    }
    return mirrors;
  }

  private List<String> strings(final AnnotationMirror mirror, final String member) {
    final Object value = member(mirror, member);
    if (!(value instanceof final List<?> list)) {
      return List.of();
    }
    final List<String> values = new ArrayList<>();
    for (final Object item : list) {
      final Object unwrapped = item instanceof final AnnotationValue annotation ? annotation.getValue() : item;
      if (unwrapped != null) {
        values.add(unwrapped.toString());
      }
    }
    return values;
  }

  private String string(final AnnotationMirror mirror, final String member) {
    final Object value = member(mirror, member);
    return value == null ? "" : value.toString();
  }

  private boolean bool(final AnnotationMirror mirror, final String member) {
    final Object value = member(mirror, member);
    return value instanceof Boolean bool && bool;
  }

  private Object member(final AnnotationMirror mirror, final String name) {
    for (final Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
        : elements.getElementValuesWithDefaults(mirror).entrySet()) {
      if (entry.getKey().getSimpleName().contentEquals(name)) {
        return entry.getValue().getValue();
      }
    }
    return null;
  }

  private String qualified(final AnnotationMirror mirror) {
    return ((TypeElement) mirror.getAnnotationType().asElement()).getQualifiedName().toString();
  }

  private String qualified(final TypeMirror type) {
    final TypeElement element = asType(type);
    return element == null ? type.toString() : element.getQualifiedName().toString();
  }

  private TypeElement asType(final TypeMirror type) {
    final Element element = types.asElement(types.erasure(type));
    return element instanceof final TypeElement typeElement ? typeElement : null;
  }

  private Element constant(final TypeElement type, final String name) {
    if (type == null) {
      return null;
    }
    for (final Element enclosed : type.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.ENUM_CONSTANT && enclosed.getSimpleName().contentEquals(name)) {
        return enclosed;
      }
    }
    return null;
  }

  private void error(final Element element, final String message) {
    Diagnostics.error(messager, element, message);
  }

  private sealed interface Node {
  }

  private record LeafNode(
      String expr,
      TypeMirror type,
      String op,
      List<String> params,
      boolean header,
      String constVal,
      String defaultVal,
      boolean not,
      boolean collection
  ) implements Node {
    private String param() {
      return params.isEmpty() ? "" : params.get(0);
    }
  }

  private record AndNode(List<Node> children) implements Node {
  }

  private record OrNode(List<Node> children) implements Node {
  }

  private record NotNode(Node child) implements Node {
  }

  private record JoinDef(String path, String kind, TypeMirror target) {
  }

  private record Resolved(String expr, TypeMirror type) {
  }
}
