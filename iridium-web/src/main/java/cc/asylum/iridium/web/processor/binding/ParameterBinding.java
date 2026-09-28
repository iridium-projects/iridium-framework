package cc.asylum.iridium.web.processor.binding;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.model.TypeRef;
import cc.asylum.forgery.stmt.Stmt;
import cc.asylum.iridium.codegen.write.Body;
import cc.asylum.iridium.web.processor.binding.convert.RequestConversion;
import cc.asylum.iridium.web.processor.binding.convert.RequestRead;
import cc.asylum.iridium.codegen.code.Blocks;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.code.Types;
import cc.asylum.iridium.codegen.model.Mirrors;
import cc.asylum.iridium.codegen.model.TypeMirrors;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.web.controller.parameter.BindingSource;
import cc.asylum.iridium.web.controller.parameter.RequestBinding;
import cc.asylum.iridium.web.router.Request;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.List;
import java.util.Optional;

@Internal
public final class ParameterBinding {

  private final javax.lang.model.util.Types types;
  private final Elements elements;
  private final RequestConversion conversion;
  private final List<ParameterBinder> binders;

  public ParameterBinding(
    final javax.lang.model.util.Types types,
    final Elements elements,
    final RequestConversion conversion,
    final List<ParameterBinder> binders
  ) {
    this.types = types;
    this.elements = elements;
    this.conversion = conversion;
    this.binders = binders;
  }

  public Optional<Expr> emit(final Body body, final VariableElement parameter) {
    final TypeMirror type = parameter.asType();
    if (TypeMirrors.isSame(types, elements, type, Request.class)) {
      return Optional.of(RequestRead.REQUEST);
    }

    for (final ParameterBinder binder : binders) {
      if (binder.matches(parameter)) {
        return binder.emit(body, parameter);
      }
    }

    return emitRequest(body, parameter, type);
  }

  private Optional<Expr> emitRequest(
    final Body body,
    final VariableElement parameter,
    final TypeMirror type
  ) {
    final String name = parameter.getSimpleName().toString();
    final AnnotationMirror mirror = Mirrors.withMeta(parameter, RequestBinding.class);
    final RequestBinding binding = mirror == null
      ? null
      : mirror.getAnnotationType().asElement().getAnnotation(RequestBinding.class);

    final BindingSource source = binding == null ? BindingSource.BODY : binding.value();
    final Optional<TypeMirror> optionalValue = TypeMirrors.optionalValue(types, type);
    final boolean optional = optionalValue.isPresent();
    final boolean required = !optional && Mirrors.bool(mirror, "required", true);
    final String defaultValue = optional ? "" : Mirrors.string(mirror, "defaultValue", "");
    final String bindingName = Mirrors.string(mirror, "value", name);
    final TypeRef declared = Types.of(type);

    if (source == BindingSource.BODY) {
      return emitBody(body, type, declared, name, binding == null || required);
    }

    if (!defaultValue.isEmpty()) {
      body.add(Blocks.declare(declared, name, conversion.convert(type, RequestRead.raw(source, bindingName, Expr.lit(defaultValue)), false)));
      return Optional.of(Exprs.name(name));
    }

    if (required) {
      return required(body, type, declared, name, source, bindingName);
    }

    final Expr raw = RequestRead.raw(source, bindingName, Expr.nil());
    final Expr value = optional
      ? conversion.optionalWrap(optionalValue.get(), raw)
      : conversion.convert(type, raw, false);
    body.add(Blocks.declare(declared, name, value));

    return Optional.of(Exprs.name(name));
  }

  private Optional<Expr> required(
    final Body body,
    final TypeMirror type,
    final TypeRef declared,
    final String name,
    final BindingSource source,
    final String bindingName
  ) {
    final String raw = name + "Raw";
    body.add(Blocks.declare(String.class, raw, RequestRead.raw(source, bindingName, Expr.nil())));
    body.add(missing(Exprs.name(raw), "Missing required " + RequestRead.label(source) + " '" + bindingName + "'"));
    body.add(conversion(type, declared, name, raw, "Invalid " + bindingName, false));

    return Optional.of(Exprs.name(name));
  }

  private Optional<Expr> emitBody(
    final Body body,
    final TypeMirror type,
    final TypeRef declared,
    final String name,
    final boolean required
  ) {
    final boolean optional = TypeMirrors.optionalValue(types, type).isPresent();
    final Expr raw = RequestRead.raw(BindingSource.BODY, name, Expr.nil());

    if (required && !optional) {
      final String rawName = name + "Raw";
      body.add(Blocks.declare(byte[].class, rawName, raw));
      body.add(missing(Exprs.name(rawName), "Missing required request body"));
      body.add(conversion(type, declared, name, rawName, "Invalid request body", true));
      return Optional.of(Exprs.name(name));
    }

    body.add(Blocks.declare(declared, name, conversion.convert(type, raw, true)));
    return Optional.of(Exprs.name(name));
  }

  private Stmt missing(final Expr raw, final String message) {
    return Blocks.ifThen(raw.eq(Expr.nil()), Blocks.ret(RequestRead.badRequest(Expr.lit(message))));
  }

  private List<Stmt> conversion(
    final TypeMirror type,
    final TypeRef declared,
    final String name,
    final String raw,
    final String message,
    final boolean body
  ) {
    return List.of(
      Blocks.declare(declared, name),
      Blocks.tryCatch(
        List.of(Blocks.assign(name, conversion.convert(type, Exprs.name(raw), body))),
        RuntimeException.class,
        "_invalid",
        List.of(Blocks.ret(RequestRead.badRequest(Expr.lit(message))))));
  }
}
