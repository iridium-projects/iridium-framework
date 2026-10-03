package cc.asylum.iridium.config.processor.walk;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.model.Diagnostics;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.codegen.model.TypeMirrors;
import cc.asylum.iridium.codegen.code.Types;
import cc.asylum.iridium.config.Config;
import cc.asylum.iridium.config.ConfigurationProperties;
import cc.asylum.iridium.config.Value;
import cc.asylum.iridium.config.processor.binding.ConfigBound;
import cc.asylum.iridium.config.processor.binding.ConfigPiece;
import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.validation.annotation.Nullable;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cc.asylum.iridium.config.processor.ConfigBinding;

public final class ConfigWalk {

  private final Processing context;
  private final ConfigTypes types;
  private final Set<String> visiting = new LinkedHashSet<>();
  private final int[] names;
  private int depth;

  public ConfigWalk(final Processing context) {
     this(context, new int[1]);
  }

  public ConfigWalk(final Processing context, final int[] names) {
    this.context = context;
    this.types = new ConfigTypes(context);
    this.names = names;
  }

  public ConfigWalk deeper() {
    final ConfigWalk child = new ConfigWalk(context, names);
    child.depth = depth + 1;
    child.visiting.addAll(visiting);
    return child;
  }

  private String nextName(final String property) {
    return property + names[0]++;
  }

  public Expr bindRoot(final TypeElement type) {
    if (type.getModifiers().contains(Modifier.ABSTRACT) || !type.getTypeParameters().isEmpty()) {
       error(type, "configuration type must be a concrete, non-generic class or record");
      return null;
    }
    final String prefix = type.getAnnotation(ConfigurationProperties.class).value().trim();
    if (!ConfigBinding.validPrefix(prefix)) {
       error(type, "invalid configuration prefix '" + prefix + "'");
      return null;
    }
    final Expr created = bindObject(type, ConfigKey.literal(prefix), true);
    return created == null ? null : created.invoke("unwrap");
  }

  public Expr bindValue(final VariableElement parameter) {
    final Value value = parameter.getAnnotation(Value.class);
    if (value == null) {
      return null;
    }
    final Matcher matcher = placeholder(parameter, value.value());
    if (matcher == null) {
      return null;
    }
    if (types.isObject(parameter.asType())) {
       error(parameter, "inject @ConfigurationProperties types as constructor parameters");
      return null;
    }
    final String fallback = matcher.group(2) != null ? matcher.group(2) : ConfigBinding.defaultOf(parameter, null);
    final ConfigBound bound = read(
        parameter,
        parameter.asType(),
        ConfigKey.literal(matcher.group(1)),
        fallback,
        parameter.getAnnotation(Nullable.class) != null);
    if (bound == null) {
      return null;
    }
    return bound.result() ? bound.code().invoke("unwrap") : bound.code();
  }

  private Expr bindObject(final TypeElement type, final ConfigKey prefix, final boolean assumePresent) {
    final String qualified = type.getQualifiedName().toString();
    if (!visiting.add(qualified)) {
       error(type, "cyclic configuration type " + qualified);
      return null;
    }
    final ExecutableElement constructor = ConfigBinding.constructorOf(type);
    if (constructor == null) {
       error(type, "configuration type must have exactly one constructor");
      visiting.remove(qualified);
      return null;
    }
    if (!constructor.getModifiers().contains(Modifier.PUBLIC)) {
       error(constructor, "configuration constructor must be public");
      visiting.remove(qualified);
      return null;
    }
    final List<ConfigPiece> pieces = arguments(type, constructor, prefix);
    visiting.remove(qualified);
    if (pieces == null) {
      return null;
    }
    final Expr creation = compose(type, pieces);
    if (assumePresent) {
      return creation;
    }
    return Exprs.select(
        Exprs.invokeStatic(Config.class, "present", prefix.code()),
        creation,
        Exprs.invokeStatic(Config.class, "missing", prefix.code()));
  }

  private List<ConfigPiece> arguments(
      final TypeElement type,
      final ExecutableElement constructor,
      final ConfigKey prefix
  ) {
    final List<? extends RecordComponentElement> components = type.getKind() == ElementKind.RECORD
        ? type.getRecordComponents()
        : List.of();
    final List<? extends VariableElement> parameters = constructor.getParameters();
    final List<ConfigPiece> pieces = new ArrayList<>();
    for (int i = 0; i < parameters.size(); i++) {
      final VariableElement parameter = parameters.get(i);
      final RecordComponentElement component = i < components.size() ? components.get(i) : null;
      final ConfigPiece piece = argument(parameter, component, prefix);
      if (piece == null) {
        return null;
      }
      pieces.add(piece);
    }
    return pieces;
  }

  private Expr compose(final TypeElement type, final List<ConfigPiece> pieces) {
    final List<Expr> args = new ArrayList<>();
    for (final ConfigPiece piece : pieces) {
      args.add(piece.result() ? Exprs.name(piece.name()) : piece.code());
    }
    final Expr creation = Exprs.new_(Types.of(type), args);
    final List<ConfigPiece> results = pieces.stream().filter(ConfigPiece::result).toList();
    if (results.isEmpty()) {
      return Exprs.invokeStatic(Result.class, "ok", creation);
    }
    Expr chain = creation;
    for (int i = results.size() - 1; i >= 0; i--) {
      final ConfigPiece piece = results.get(i);
      final String op = i == results.size() - 1 ? "map" : "flatMap";
      chain = piece.code().invoke(op, Exprs.lambda(piece.name(), chain));
    }
    return chain;
  }

  private ConfigPiece argument(
      final VariableElement parameter,
      final RecordComponentElement component,
      final ConfigKey prefix
  ) {
    final String property = component == null
        ? parameter.getSimpleName().toString()
        : component.getSimpleName().toString();
    if (component == null && property.matches("arg\\d+")) {
       error(parameter, "constructor parameter names are missing; compile with -parameters");
      return null;
    }
    final Value value = ConfigBinding.annotation(parameter, component, Value.class);
    final ConfigKey key;
    final String fallback;
    if (value == null) {
      key = prefix.child(property);
      fallback = ConfigBinding.defaultOf(parameter, component);
    } else {
      final Matcher matcher = placeholder(parameter, value.value());
      if (matcher == null) {
        return null;
      }
      key = ConfigKey.literal(matcher.group(1));
      fallback = matcher.group(2) != null ? matcher.group(2) : ConfigBinding.defaultOf(parameter, component);
    }
    final ConfigBound bound = read(
        parameter,
        parameter.asType(),
        key,
        fallback,
        ConfigBinding.annotation(parameter, component, Nullable.class) != null);
    return bound == null ? null : new ConfigPiece(bound.code(), bound.result(), nextName(property));
  }

  private ConfigBound read(
      final Element element,
      final TypeMirror type,
      final ConfigKey key,
      final String fallback,
      final boolean nullable
  ) {
    if (nullable && type.getKind().isPrimitive()) {
       error(element, "@Nullable is not valid on a primitive configuration value");
      return null;
    }
    final TypeMirror optional = TypeMirrors.optionalValue(context.types(), type).orElse(null);
    if (optional != null) {
       return optionalOf(element, optional, key, fallback);
    }
    final ConfigScalar scalar = types.scalar(type);
    if (scalar != null) {
      final Expr code = scalar.read(context, element, key, fallback, nullable);
      return code == null ? null : new ConfigBound(code, !nullable);
    }
    if (!(type instanceof final DeclaredType declared)) {
       error(element, "unsupported configuration type " + type);
      return null;
    }
    final TypeElement declaredType = TypeMirrors.asTypeElement(context.types(), type).orElse(null);
    if (declaredType == null) {
       error(element, "unsupported configuration type " + type);
      return null;
    }
    final String qualified = declaredType.getQualifiedName().toString();
    if ("java.util.List".equals(qualified) || "java.util.Set".equals(qualified)) {
       return collection(element, declared, "java.util.Set".equals(qualified), key, fallback);
    }
    if ("java.util.Map".equals(qualified)) {
      final Expr mapped = map(element, declared, key, fallback);
      return mapped == null ? null : new ConfigBound(mapped, true);
    }
    if (fallback != null) {
       error(element, "@Default is not supported for nested configuration objects");
      return null;
    }
     return nested(declaredType, key, nullable);
  }

  private ConfigBound optionalOf(
      final Element element,
      final TypeMirror inner,
      final ConfigKey key,
      final String fallback
  ) {
    final ConfigBound value = read(element, inner, key, fallback, false);
    if (value == null) {
      return null;
    }
    final Expr innerValue = value.result() ? value.code().invoke("unwrap") : value.code();
     return new ConfigBound(Exprs.select(
        Exprs.invokeStatic(Config.class, "has", key.code()),
        Exprs.invokeStatic(Optional.class, "of", innerValue),
        Exprs.invokeStatic(Optional.class, "empty")), false);
  }

  private ConfigBound nested(final TypeElement type, final ConfigKey key, final boolean nullable) {
    if (!nullable) {
      final Expr creation = bindObject(type, key, false);
      return creation == null ? null : new ConfigBound(creation, true);
    }
    final Expr creation = bindObject(type, key, true);
    if (creation == null) {
      return null;
    }
     return new ConfigBound(Exprs.select(
        Exprs.invokeStatic(Config.class, "present", key.code()),
        creation.invoke("unwrap"),
        Expr.nil()), false);
  }

  private ConfigBound collection(
      final Element element,
      final DeclaredType declared,
      final boolean set,
      final ConfigKey key,
      final String fallback
  ) {
    if (fallback != null) {
       error(element, "@Default is not supported for collections");
      return null;
    }
    if (declared.getTypeArguments().size() != 1
        || declared.getTypeArguments().get(0).getKind() == TypeKind.WILDCARD) {
       error(element, "collection item type must be a concrete type argument");
      return null;
    }
    final Expr items = collectionItems(element, declared.getTypeArguments().get(0), key);
    if (items == null) {
      return null;
    }
    if (!set) {
       return new ConfigBound(items, true);
    }
     return new ConfigBound(items.invoke("map", Exprs.methodRef(Set.class, "copyOf")), true);
  }

  private Expr collectionItems(final Element element, final TypeMirror item, final ConfigKey key) {
    final ConfigScalar scalar = types.scalar(item);
    if (scalar != null) {
      return Exprs.invokeStatic(Config.class, "list", key.code(), scalar.converter());
    }
    final TypeElement itemType = TypeMirrors.asTypeElement(context.types(), item).orElse(null);
    if (itemType == null || itemType.getKind() == ElementKind.ENUM) {
       error(element, "unsupported collection item type " + item);
      return null;
    }
    final String index = "i" + depth;
    final Expr nested = deeper().bindObject(itemType, key.indexed(index), true);
    if (nested == null) {
      return null;
    }
    return Exprs.invokeStatic(Config.class, "indexed", key.code(), Exprs.lambda(index, nested));
  }

  private Expr map(
      final Element element,
      final DeclaredType declared,
      final ConfigKey key,
      final String fallback
  ) {
    if (fallback != null) {
       error(element, "@Default is not supported for maps");
      return null;
    }
    if (declared.getTypeArguments().size() != 2) {
       error(element, "raw maps are not supported");
      return null;
    }
    if (!types.same(declared.getTypeArguments().get(0), String.class)) {
       error(element, "map keys must be String");
      return null;
    }
    final ConfigScalar scalar = types.scalar(declared.getTypeArguments().get(1));
    if (scalar == null) {
       error(element, "map values must be scalar");
      return null;
    }
    return Exprs.invokeStatic(Config.class, "map", key.code(), scalar.converter());
  }

  private Matcher placeholder(final Element element, final String raw) {
    final Matcher matcher = ConfigBinding.PLACEHOLDER.matcher(raw.trim());
    if (!matcher.matches() || matcher.group(1).isBlank()) {
       error(element, "@Value must be a placeholder like ${key} or ${key:default}");
      return null;
    }
    return matcher;
  }

  private void error(final Element element, final String message) {
    Diagnostics.error(context.messager(), element, message);
  }
}
