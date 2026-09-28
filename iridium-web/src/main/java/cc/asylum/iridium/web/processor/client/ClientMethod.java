package cc.asylum.iridium.web.processor.client;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.member.MethodBuilder;
import cc.asylum.forgery.stmt.Stmt;
import cc.asylum.iridium.codegen.code.Blocks;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.code.Types;
import cc.asylum.iridium.codegen.model.Diagnostics;
import cc.asylum.iridium.codegen.model.Mirrors;
import cc.asylum.iridium.codegen.model.TypeMirrors;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.web.controller.parameter.BindingSource;
import cc.asylum.iridium.web.controller.parameter.RequestBinding;
import cc.asylum.iridium.web.http.RequestMethod;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Internal
public final class ClientMethod {

  private final javax.lang.model.util.Types types;
  private final Elements elements;
  private final Messager messager;

  public ClientMethod(final javax.lang.model.util.Types types, final Elements elements, final Messager messager) {
    this.types = types;
    this.elements = elements;
    this.messager = messager;
  }

  public GeneratedMethod emit(final ExecutableElement method, final AnnotationMirror mapping, final String url) {
    final String path = Mirrors.string(mapping, "value", "");
    if (path.isEmpty()) {
      Diagnostics.error(messager, method, "@RequestMapping value must not be empty");
      return null;
    }

    VariableElement body = null;
    final List<String> query = new ArrayList<>();
    final List<String> pathArgs = new ArrayList<>();
    final List<VariableElement> parameters = new ArrayList<>(method.getParameters());
    for (final VariableElement parameter : parameters) {
      final String param = parameter.getSimpleName().toString();
      final BindingSource source = source(parameter);
      if (source == BindingSource.BODY) {
        if (body != null) {
          Diagnostics.error(messager, method, "only one request body parameter is allowed");
          return null;
        }
        body = parameter;
        continue;
      }

      final String binding = bindingName(parameter, param);
      if (source == BindingSource.PATH) {
        pathArgs.add(binding);
        pathArgs.add(param);
      } else {
        query.add(binding);
        query.add(param);
      }
    }

    final List<Stmt> statements = exchange(mapping, url, ClientPath.expression(path, pathArgs, query), body);
    final Stmt returned = returning(method.getReturnType());
    if (returned != null) {
      statements.add(returned);
    }

    return new GeneratedMethod(method.getSimpleName().toString(), builder -> {
      builder.public_().overrides();
      builder.returns(Types.of(method.getReturnType()));
      for (final TypeMirror thrown : method.getThrownTypes()) {
        builder.throws_(Types.of(thrown));
      }

      for (final VariableElement parameter : parameters) {
        builder.parameter(Types.of(parameter.asType()), parameter.getSimpleName().toString());
      }

      builder.body(block -> Blocks.addAll(block, statements));
    });
  }

  private List<Stmt> exchange(
      final AnnotationMirror mapping,
      final String url,
      final Expr path,
      final VariableElement body
  ) {
    final List<Stmt> statements = new ArrayList<>();
    final Expr method = Exprs.lit(methodName(mapping));
    if (body == null) {
      statements.add(Blocks.declare(byte[].class, "_body", Exprs.invokeStatic(
          ClientPath.CLIENTS, "exchange", Expr.lit(url), method, path, Exprs.lit(contentType(mapping)), Expr.nil())));
      return statements;
    }

    final Expr bodyName = Exprs.name(body.getSimpleName().toString());
    statements.add(Blocks.declare(String.class, "_contentType", Exprs.invokeStatic(ClientPath.CLIENTS, "contentTypeOf", Exprs.lit(contentType(mapping)), bodyName)));
    statements.add(Blocks.declare(byte[].class, "_encoded", Exprs.invokeStatic(ClientPath.CLIENTS, "bytes", bodyName)));
    statements.add(Blocks.declare(byte[].class, "_body", Exprs.invokeStatic(
        ClientPath.CLIENTS, "exchange", Expr.lit(url), method, path, Exprs.name("_contentType"), Exprs.name("_encoded"))));

    return statements;
  }

  private Stmt returning(final TypeMirror type) {
    if (type.getKind() == TypeKind.VOID) {
      return null;
    }

    if (type.getKind() == TypeKind.ARRAY && "byte[]".equals(type.toString())) {
      return Blocks.ret(Exprs.name("_body"));
    }

    if (TypeMirrors.isString(types, type)) {
      return Blocks.ret(Exprs.invokeStatic(ClientPath.CLIENTS, "text", Exprs.name("_body")));
    }

    final TypeMirror erased = type.getKind().isPrimitive()
        ? types.boxedClass(types.getPrimitiveType(type.getKind())).asType()
        : types.erasure(type);

    return Blocks.ret(Exprs.invokeStatic(ClientPath.CLIENTS, "json", Exprs.name("_body"), Exprs.classLit(Types.of(erased))));
  }

  private BindingSource source(final VariableElement parameter) {
    final AnnotationMirror mirror = Mirrors.withMeta(parameter, RequestBinding.class);
    if (mirror == null) {
      return BindingSource.QUERY;
    }

    final RequestBinding binding = mirror.getAnnotationType().asElement().getAnnotation(RequestBinding.class);
    return binding == null ? BindingSource.QUERY : binding.value();
  }

  private String bindingName(final VariableElement parameter, final String fallback) {
    final AnnotationMirror mirror = Mirrors.withMeta(parameter, RequestBinding.class);
    final String value = Mirrors.string(mirror, "value", "");
    return value.isEmpty() ? fallback : value;
  }

  private String methodName(final AnnotationMirror mapping) {
    final Object value = Mirrors.member(elements, mapping, "method");
    if (value == null) {
      return RequestMethod.GET.name();
    }

    final String text = value.toString();
    final int dot = text.lastIndexOf('.');
    return dot < 0 ? text : text.substring(dot + 1);
  }

  private String contentType(final AnnotationMirror mapping) {
    return Mirrors.string(mapping, "contentType", "");
  }

}
