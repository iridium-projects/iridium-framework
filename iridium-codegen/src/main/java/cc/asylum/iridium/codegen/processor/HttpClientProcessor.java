package cc.asylum.iridium.codegen.processor;

import com.io7m.jodist.ClassName;
import com.io7m.jodist.CodeBlock;
import com.io7m.jodist.MethodSpec;
import com.io7m.jodist.ParameterSpec;
import com.io7m.jodist.TypeName;
import cc.asylum.iridium.codegen.IridiumProcessor;
import cc.asylum.iridium.codegen.support.Diagnostics;
import cc.asylum.iridium.codegen.support.MirrorSupport;
import cc.asylum.iridium.codegen.support.ModelSupport;
import cc.asylum.iridium.codegen.support.SourceWriter;
import cc.asylum.iridium.codegen.support.TypeSupport;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.web.controller.parameter.RequestBinding;
import cc.asylum.iridium.web.http.HttpClient;
import cc.asylum.iridium.web.http.RequestMapping;
import cc.asylum.iridium.web.http.RequestMethod;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Internal
public final class HttpClientProcessor extends IridiumProcessor {

  private static final ClassName CLIENTS = ClassName.get(
      "cc.asylum.iridium.web.undertow.client", "UndertowClients");

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(HttpClient.class.getCanonicalName());
  }

  @Override
  protected void processRound(final RoundEnvironment roundEnv) {
    for (final TypeElement client : ModelSupport.annotatedTypes(roundEnv, HttpClient.class, ElementKind.INTERFACE)) {
      generate(client);
    }
  }

  private void generate(final TypeElement client) {
    final AnnotationMirror clientMirror = MirrorSupport.mirrorOf(client, HttpClient.class);
    final String url = MirrorSupport.stringMember(clientMirror, "url", "");

    if (url.isEmpty()) {
      Diagnostics.error(messager, client, "@HttpClient url must not be empty");
      return;
    }

    final String pkg = ModelSupport.packageOf(elements, client);
    final String name = client.getSimpleName() + "Undertow";
    final var type = SourceWriter.generatedType(name).addSuperinterface(ClassName.get(client));

    boolean mapped = false;
    for (final Element enclosed : client.getEnclosedElements()) {
      if (enclosed.getKind() != ElementKind.METHOD) {
        continue;
      }

      final ExecutableElement method = (ExecutableElement) enclosed;
      if (method.isDefault() || method.getModifiers().contains(Modifier.STATIC)) {
        continue;
      }

      final AnnotationMirror mapping = MirrorSupport.mirrorOf(method, RequestMapping.class);
      if (mapping == null) {
        Diagnostics.error(messager, method, "@HttpClient methods require @RequestMapping");
        continue;
      }

      final MethodSpec generated = methodOf(method, mapping, url);
      if (generated == null) {
        continue;
      }

      type.addMethod(generated);
      mapped = true;
    }

    if (!mapped) {
      return;
    }

    SourceWriter.writeJava(filer, pkg, type.build(), client);
  }

  private MethodSpec methodOf(
      final ExecutableElement method,
      final AnnotationMirror mapping,
      final String url) {
    final String path = MirrorSupport.stringMember(mapping, "value", "");
    if (path.isEmpty()) {
      Diagnostics.error(messager, method, "@RequestMapping value must not be empty");
      return null;
    }

    final String contentType = MirrorSupport.stringMember(mapping, "contentType", "");
    final String httpMethod = methodName(mapping);
    final MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getSimpleName().toString())
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(TypeName.get(method.getReturnType()));

    for (final TypeMirror thrown : method.getThrownTypes()) {
      builder.addException(TypeName.get(thrown));
    }

    VariableElement body = null;
    final List<String> query = new ArrayList<>();
    final List<String> pathArgs = new ArrayList<>();

    for (final VariableElement parameter : method.getParameters()) {
      final String param = parameter.getSimpleName().toString();
      builder.addParameter(ParameterSpec.builder(TypeName.get(parameter.asType()), param).build());
      final RequestBinding.Source source = sourceOf(parameter);

      if (source == RequestBinding.Source.BODY) {
        if (body != null) {
          Diagnostics.error(messager, method, "only one request body parameter is allowed");
          return null;
        }

        body = parameter;
        continue;
      }

      final String bindingName = bindingName(parameter, param);
      if (source == RequestBinding.Source.PATH) {
        pathArgs.add(bindingName);
        pathArgs.add(param);
        continue;
      }

      query.add(bindingName);
      query.add(param);
    }

    final CodeBlock pathExpr = pathExpression(path, pathArgs, query);
    if (body == null) {
      builder.addStatement(
          "final byte[] _body = $T.exchange($S, $S, $L, $S, null)",
          CLIENTS,
          url,
          httpMethod,
          pathExpr,
          contentType);

    } else {
      final String bodyName = body.getSimpleName().toString();

      builder.addStatement(
          "final String _contentType = $T.contentTypeOf($S, $L)",
          CLIENTS,
          contentType,
          bodyName);

      builder.addStatement(
          "final byte[] _encoded = $T.bytes($L)",
          CLIENTS,
          bodyName);

      builder.addStatement(
          "final byte[] _body = $T.exchange($S, $S, $L, _contentType, _encoded)",
          CLIENTS,
          url,
          httpMethod,
          pathExpr);
    }

    this.emitReturn(builder, method.getReturnType());
    return builder.build();
  }

  private void emitReturn(final MethodSpec.Builder builder, final TypeMirror type) {
    if (type.getKind() == TypeKind.VOID) {
      return;
    }

    if (type.getKind() == TypeKind.ARRAY && "byte[]".equals(type.toString())) {
      builder.addStatement("return _body");
      return;
    }

    if (TypeSupport.isString(types, type)) {
      builder.addStatement("return $T.text(_body)", CLIENTS);
      return;
    }

    builder.addStatement("return $T.json(_body, $T.class)", CLIENTS, classOf(type));
  }

  private TypeName classOf(final TypeMirror type) {
    if (type.getKind().isPrimitive()) {
      return TypeName.get(types.boxedClass(types.getPrimitiveType(type.getKind())).asType());
    }

    return TypeName.get(types.erasure(type));
  }

  private CodeBlock pathExpression(
      final String path,
      final List<String> pathArgs,
      final List<String> query) {
    final CodeBlock.Builder block = CodeBlock.builder();
    int cursor = 0;

    boolean first = true;

    while (cursor < path.length()) {
      final int open = path.indexOf('{', cursor);

      if (open < 0) {
        this.appendLiteral(block, first, path.substring(cursor));
        first = false;
        break;
      }

      final int close = path.indexOf('}', open);
      if (close < 0) {
        this.appendLiteral(block, first, path.substring(cursor));
        break;
      }

      this.appendLiteral(block, first, path.substring(cursor, open));
      first = false;
      block.add(" + $L", argumentFor(path.substring(open + 1, close), pathArgs));
      cursor = close + 1;
    }

    if (first) {
      block.add("$S", path);
    }

    boolean queryFirst = true;
    for (int i = 0; i < query.size(); i += 2) {
      block.add(
          queryFirst ? " + $S + $T.query($L)" : " + $S + $T.query($L)",
          (queryFirst ? "?" : "&") + query.get(i) + "=",
          CLIENTS,
          query.get(i + 1));

      queryFirst = false;
    }

    return block.build();
  }

  private void appendLiteral(final CodeBlock.Builder block, final boolean first, final String literal) {
    if (literal.isEmpty()) {
      if (first) {
        block.add("$S", "");
      }

      return;
    }

    block.add(first ? "$S" : " + $S", literal);
  }

  private String argumentFor(final String name, final List<String> args) {
    for (int i = 0; i < args.size(); i += 2) {
      if (args.get(i).equals(name)) {
        return args.get(i + 1);
      }
    }

    return "\"\"";
  }

  private RequestBinding.Source sourceOf(final VariableElement parameter) {
    final AnnotationMirror mirror = MirrorSupport.annotationWithMeta(parameter, RequestBinding.class);
    if (mirror == null) {
      return RequestBinding.Source.QUERY;
    }

    final RequestBinding binding = mirror.getAnnotationType().asElement().getAnnotation(RequestBinding.class);

    return binding == null ? RequestBinding.Source.QUERY : binding.value();
  }

  private String bindingName(final VariableElement parameter, final String fallback) {
    final AnnotationMirror mirror = MirrorSupport.annotationWithMeta(parameter, RequestBinding.class);
    final String value = MirrorSupport.stringMember(mirror, "value", "");

    return value.isEmpty() ? fallback : value;
  }

  private String methodName(final AnnotationMirror mapping) {
    final Object value = MirrorSupport.member(elements, mapping, "method");
    if (value == null) {
      return RequestMethod.GET.name();
    }

    final String text = value.toString();
    final int dot = text.lastIndexOf('.');

    return dot < 0 ? text : text.substring(dot + 1);
  }
}
