package cc.asylum.iridium.web.processor.route;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.model.TypeRef;
import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.codegen.bean.BeanRegistration;
import cc.asylum.iridium.codegen.code.Blocks;
import cc.asylum.iridium.codegen.write.Body;
import cc.asylum.iridium.codegen.write.NestedTypes;
import cc.asylum.iridium.codegen.write.Registrar;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.model.Mirrors;
import cc.asylum.iridium.codegen.code.Names;
import cc.asylum.iridium.codegen.code.Types;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.bean.BeanPool;
import cc.asylum.iridium.core.util.Paths;
import cc.asylum.iridium.core.validation.Valid;
import cc.asylum.iridium.core.validation.Validation;
import cc.asylum.iridium.web.controller.RestController;
import cc.asylum.iridium.web.controller.mapping.HttpMapping;
import cc.asylum.iridium.web.controller.parameter.BindingSource;
import cc.asylum.iridium.web.controller.parameter.RequestBinding;
import cc.asylum.iridium.web.controller.parameter.RequestBody;
import cc.asylum.iridium.web.processor.binding.ParameterBinder;
import cc.asylum.iridium.web.processor.binding.ParameterBinderFactory;
import cc.asylum.iridium.web.processor.binding.ParameterBinding;
import cc.asylum.iridium.web.processor.binding.convert.RequestConversion;
import cc.asylum.iridium.web.processor.binding.convert.RequestRead;
import cc.asylum.iridium.web.processor.binding.convert.WebRequestValues;
import cc.asylum.iridium.web.response.Response;
import cc.asylum.iridium.web.router.Handler;
import cc.asylum.iridium.web.router.Request;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

@Internal
public final class RouteWriter {

  private static final TypeRef RESPONSE = Types.parameterized(Response.class, Types.wildcardExtends(TypeRef.OBJECT));

  private final ParameterBinding binding;
  private final Names names = new Names();

  public RouteWriter(final Processing processing) {
    final List<ParameterBinder> binders = ServiceLoader.load(
        ParameterBinderFactory.class,
        ParameterBinderFactory.class.getClassLoader()).stream()
      .map(provider -> provider.get().create(
        processing.types(), processing.elements(), processing.messager(), new WebRequestValues()))
      .toList();

    this.binding = new ParameterBinding(
      processing.types(), processing.elements(), new RequestConversion(processing.types()), binders);
  }

  public List<Expr> args(final ExecutableElement constructor, final Processing processing) {
    return new BeanRegistration(
      processing,
      Registrar.of("_", Object.class, Object.class, "pool")
    ).args(constructor, beanLookup());
  }

  public Optional<MethodMapping> mappingOf(final ExecutableElement method) {
    final HttpMapping mapping = Mirrors.meta(method, HttpMapping.class);
    if (mapping == null) {
      return Optional.empty();
    }

    final AnnotationMirror mirror = Mirrors.withMeta(method, HttpMapping.class);
    return Optional.of(new MethodMapping(mapping.method(), Mirrors.string(mirror, "value", "")));
  }

  public String prefixOf(final TypeElement controller) {
    return Mirrors.string(controller, RestController.class, "value", "");
  }

  public static String resolvePath(final String prefix, final String path) {
    return Paths.join(prefix, path);
  }

  public Expr beanLookup() {
    return Exprs.invokeStatic(BeanPool.class, "instance");
  }

  public Optional<Expr> routeHandler(
    final NestedTypes nested,
    final TypeElement controller,
    final ExecutableElement method
  ) {
    final TypeRef controllerType = Types.of(controller.asType());
    final Body body = new Body();
    body.add(Blocks.declare(controllerType, "_controller", Expr.this_().field("controller")));

    final List<Expr> arguments = new ArrayList<>();
    boolean readsBody = false;
    for (final VariableElement parameter : method.getParameters()) {
      readsBody |= readsBody(parameter);

      final Optional<Expr> argument = binding.emit(body, parameter);
      if (argument.isEmpty()) {
        return Optional.empty();
      }

      Expr bound = argument.get();
      if (parameter.getAnnotation(Valid.class) != null) {
        bound = escapeHtml(body, bound);
        this.validate(body, bound);
      }

      arguments.add(bound);
    }

    body.add(Blocks.ret(Exprs.invoke(Exprs.name("_controller"), method.getSimpleName().toString(), arguments)));

    final boolean bodyReader = readsBody;
    final String name = controller.getSimpleName() + capitalize(method.getSimpleName().toString()) + "Handler";
    nested.nest(name, type -> {
      type.implements_(Handler.class);

      type.field(controllerType, "controller", field -> field.private_().final_());
      type.constructor(constructor -> constructor.body(init -> init.assign(
        Exprs.name("controller"),
        Exprs.invokeStatic(BeanPool.class, "instance").invoke("get", Exprs.classLit(controllerType)))));

      type.method("handle", handle -> {
        handle.public_().overrides();
        handle.returns(RESPONSE);
        handle.parameter(Request.class, "_request");
        handle.throws_(Exception.class);
        handle.body(block -> Blocks.addAll(block, body.statements()));
      });

      if (bodyReader) {
        type.method("readsBody", reads -> {
          reads.public_().overrides();
          reads.returns(boolean.class);
          reads.body(block -> block.return_(Expr.lit(true)));
        });
      }

    });

    for (final var configure : body.nested()) {
      configure.accept(nested);
    }
    return Optional.of(Exprs.new_(TypeRef.of(name)));
  }

  private void validate(final Body body, final Expr value) {
    final String checked = names.next("checked");

    body.add(Blocks.declareVar(checked, Exprs.invokeStatic(Validation.class, "validate", value)));
    body.add(Blocks.ifThen(
      Exprs.name(checked).invoke("isErr"),
      Blocks.ret(RequestRead.badRequest(Exprs.name(checked).invoke("unwrapErr")))));
  }

  private Expr escapeHtml(final Body body, final Expr value) {
    final String escaped = names.next("escaped");
    body.add(Blocks.declareVar(escaped, Exprs.invokeStatic(Validation.class, "escape", value)));
    return Exprs.name(escaped);
  }


  private boolean readsBody(final VariableElement parameter) {
    if (parameter.getAnnotation(RequestBody.class) != null) {
      return true;
    }

    final AnnotationMirror mirror = Mirrors.withMeta(parameter, RequestBinding.class);
    if (mirror == null) {
      return false;
    }

    final RequestBinding binding = mirror.getAnnotationType().asElement().getAnnotation(RequestBinding.class);
    return binding != null && binding.value() == BindingSource.BODY;
  }

  public static List<ExecutableElement> handlerMethods(final TypeElement controller) {
    final List<ExecutableElement> result = new ArrayList<>();

    for (final Element enclosed : controller.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.METHOD) {
        result.add((ExecutableElement) enclosed);
      }
    }

    return result;
  }

  private static String capitalize(final String value) {
    if (value.isEmpty()) {
      return value;
    }
    return Character.toUpperCase(value.charAt(0)) + value.substring(1);
  }
}
