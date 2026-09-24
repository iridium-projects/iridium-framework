package cc.asylum.iridium.codegen.writer;

import com.io7m.jodist.ClassName;
import com.io7m.jodist.CodeBlock;
import com.io7m.jodist.MethodSpec;
import com.io7m.jodist.ParameterizedTypeName;
import com.io7m.jodist.TypeName;
import com.io7m.jodist.TypeSpec;
import com.io7m.jodist.WildcardTypeName;
import cc.asylum.iridium.codegen.support.Diagnostics;
import cc.asylum.iridium.codegen.support.MirrorSupport;
import cc.asylum.iridium.codegen.support.ModelSupport;
import cc.asylum.iridium.codegen.support.TypeSupport;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.bean.BeanPool;
import cc.asylum.iridium.web.controller.Parameters;
import cc.asylum.iridium.web.controller.RestController;
import cc.asylum.iridium.web.controller.mapping.HttpMapping;
import cc.asylum.iridium.web.controller.parameter.RequestBinding;
import cc.asylum.iridium.web.response.Response;
import cc.asylum.iridium.web.router.Handler;
import cc.asylum.iridium.web.router.Request;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Internal
public final class RouteWriter {

  private static final ClassName RESPONSE = ClassName.get(Response.class);
  private static final ClassName PARAMETERS = ClassName.get(Parameters.class);
  private static final ClassName REQUEST_CLASS = ClassName.get(Request.class);
  private static final ClassName HANDLER = ClassName.get(Handler.class);
  private static final ClassName BEAN_POOL = ClassName.get(BeanPool.class);

  private final Types types;
  private final Elements elements;
  private final Messager messager;

  public RouteWriter(
      final Types types,
      final Elements elements,
      final Messager messager
  ) {
    this.types = types;
    this.elements = elements;
    this.messager = messager;
  }

  public record MethodMapping(String httpMethod, String path) {
  }

  public Optional<MethodMapping> mappingOf(final ExecutableElement method) {
    final HttpMapping mapping = MirrorSupport.metaAnnotation(method, HttpMapping.class);
    if (mapping == null) {
      return Optional.empty();
    }
    final AnnotationMirror mirror = MirrorSupport.annotationWithMeta(method, HttpMapping.class);
    return Optional.of(new MethodMapping(mapping.method(), MirrorSupport.stringMember(mirror, "value", "")));
  }

  public String prefixOf(final TypeElement controller) {
    return MirrorSupport.stringValue(controller, RestController.class, "value", "");
  }

  public static String resolvePath(final String prefix, final String path) {
    final String head = trimSlashes(prefix);
    final String tail = trimSlashes(path);
    if (head.isEmpty()) {
      return tail.isEmpty() ? "/" : "/" + tail;
    }
    return tail.isEmpty() ? "/" + head : "/" + head + "/" + tail;
  }

  private static String trimSlashes(final String value) {
    if (value == null) {
      return "";
    }
    int start = 0;
    int end = value.length();
    while (start < end && value.charAt(start) == '/') {
      start++;
    }
    while (end > start && value.charAt(end - 1) == '/') {
      end--;
    }
    return value.substring(start, end);
  }

  public CodeBlock beanLookupPool() {
    return CodeBlock.of("$T.instance()", BEAN_POOL);
  }

  public Optional<TypeSpec> routeHandler(
      final String controllerVar,
      final ExecutableElement method
  ) {
    final TypeName responseWildcard = ParameterizedTypeName.get(
        RESPONSE,
        WildcardTypeName.subtypeOf(TypeName.OBJECT)
    );
    final MethodSpec.Builder handle = MethodSpec.methodBuilder("handle")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(REQUEST_CLASS, "_request")
        .addException(Exception.class)
        .returns(responseWildcard);

    final List<String> arguments = new ArrayList<>();
    for (final VariableElement parameter : method.getParameters()) {
      final Optional<String> argument = emitBinding(handle, parameter);
      if (argument.isEmpty()) {
        return Optional.empty();
      }
      arguments.add(argument.get());
    }

    handle.addStatement(
        "return $L.$N($L)",
        controllerVar,
        method.getSimpleName().toString(),
        String.join(", ", arguments)
    );
    return Optional.of(TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(HANDLER)
        .addMethod(handle.build())
        .build());
  }

  private Optional<String> emitBinding(
      final MethodSpec.Builder handle,
      final VariableElement parameter
  ) {
    final TypeMirror type = parameter.asType();
    final String typeName = type.toString();
    final String name = parameter.getSimpleName().toString();

    if (TypeSupport.isSameType(types, elements, type, Request.class)) {
      return Optional.of("_request");
    }

    final AnnotationMirror bindingMirror = MirrorSupport.annotationWithMeta(parameter, RequestBinding.class);
    final RequestBinding binding = bindingMirror == null
        ? null
        : bindingMirror.getAnnotationType().asElement().getAnnotation(RequestBinding.class);
    final RequestBinding.Source source = binding == null
        ? RequestBinding.Source.BODY
        : binding.value();

    if (source == RequestBinding.Source.ATTRIBUTE) {
      Diagnostics.error(messager, parameter, "@RequestAttribute is not supported yet");
      return Optional.empty();
    }

    final Optional<TypeMirror> optionalValue = TypeSupport.optionalValueType(types, type);
    final boolean optional = optionalValue.isPresent();
    final boolean required = optional
        ? false
        : MirrorSupport.booleanMember(bindingMirror, "required", true);
    final String defaultValue = optional
        ? ""
        : MirrorSupport.stringMember(bindingMirror, "defaultValue", "");
    final String bindingName = MirrorSupport.stringMember(bindingMirror, "value", name);

    if (source == RequestBinding.Source.BODY) {
      return Optional.of(emitBody(handle, type, name, binding == null || required));
    }

    if (!defaultValue.isEmpty()) {
      handle.addStatement(
          "$L $L = $L",
          typeName,
          name,
          convertExpression(type, rawExpr(source, bindingName, defaultValue))
      );
      return Optional.of(name);
    }

    if (required) {
      final String rawVar = name + "Raw";
      handle.addStatement("java.lang.String $L = $L", rawVar, rawExpr(source, bindingName, null));
      handle.addStatement(
          "if ($L == null) { return $T.badRequest().body($S); }",
          rawVar,
          RESPONSE,
          "Missing required " + label(source) + " '" + bindingName + "'"
      );
      handle.addStatement("$L $L = $L", typeName, name, convertExpression(type, rawVar));
      return Optional.of(name);
    }

    final String value = optional
        ? optionalWrapExpression(optionalValue.get(), rawExpr(source, bindingName, null))
        : convertExpression(type, rawExpr(source, bindingName, null));
    handle.addStatement("$L $L = $L", typeName, name, value);
    return Optional.of(name);
  }

  private String emitBody(
      final MethodSpec.Builder handle,
      final TypeMirror type,
      final String name,
      final boolean required
  ) {
    final boolean optional = TypeSupport.optionalValueType(types, type).isPresent();
    if (required && !optional) {
      final String rawVar = name + "Raw";
      handle.addStatement(
          "java.lang.String $L = $L",
          rawVar,
          rawExpr(RequestBinding.Source.BODY, name, null)
      );
      handle.addStatement(
          "if ($L == null) { return $T.badRequest().body($S); }",
          rawVar,
          RESPONSE,
          "Missing required request body"
      );
      handle.addStatement("$L $L = $L", type.toString(), name, convertExpression(type, rawVar));
      return name;
    }

    handle.addStatement(
        "$L $L = $L",
        type.toString(),
        name,
        convertExpression(type, rawExpr(RequestBinding.Source.BODY, name, null))
    );
    return name;
  }

  private String rawExpr(
      final RequestBinding.Source source,
      final String name,
      final String defaultValue
  ) {
    final String value = defaultValue == null ? "null" : "\"" + defaultValue + "\"";
    return switch (source) {
      case PATH -> PARAMETERS + ".pathVariable(_request, \"" + name + "\", " + value + ")";
      case HEADER -> PARAMETERS + ".header(_request, \"" + name + "\", " + value + ")";
      case COOKIE -> PARAMETERS + ".cookie(_request, \"" + name + "\", " + value + ")";
      case BODY -> PARAMETERS + ".body(_request)";
      default -> PARAMETERS + ".query(_request, \"" + name + "\", " + value + ")";
    };
  }

  String convertExpression(
      final TypeMirror type,
      final String raw
  ) {
    if (type.getKind().isPrimitive()) {
      return switch (type.getKind()) {
        case BOOLEAN -> "Boolean.parseBoolean(" + raw + ")";
        case BYTE -> "Byte.parseByte(" + raw + ")";
        case SHORT -> "Short.parseShort(" + raw + ")";
        case INT -> "Integer.parseInt(" + raw + ")";
        case LONG -> "Long.parseLong(" + raw + ")";
        case CHAR -> raw + ".charAt(0)";
        case FLOAT -> "Float.parseFloat(" + raw + ")";
        case DOUBLE -> "Double.parseDouble(" + raw + ")";
        default -> raw;
      };
    }
    return switch (TypeSupport.qualifiedName(types, type)) {
      case "java.lang.String" -> raw;
      case "java.lang.Boolean" -> "Boolean.valueOf(" + raw + ")";
      case "java.lang.Byte" -> "Byte.valueOf(" + raw + ")";
      case "java.lang.Short" -> "Short.valueOf(" + raw + ")";
      case "java.lang.Integer" -> "Integer.valueOf(" + raw + ")";
      case "java.lang.Long" -> "Long.valueOf(" + raw + ")";
      case "java.lang.Character" -> raw + ".charAt(0)";
      case "java.lang.Float" -> "Float.valueOf(" + raw + ")";
      case "java.lang.Double" -> "Double.valueOf(" + raw + ")";
      default -> ModelSupport.rootPackage() + ".json.Json.load()"
          + ".deserialize(" + raw + ", " + types.erasure(type) + ".class)";
    };
  }

  private String optionalWrapExpression(
      final TypeMirror inner,
      final String raw
  ) {
    return switch (TypeSupport.qualifiedName(types, inner)) {
      case "java.lang.String" -> "java.util.Optional.ofNullable(" + raw + ")";
      case "java.lang.Long" -> "java.util.Optional.ofNullable(" + raw + ").map(java.lang.Long::parseLong)";
      case "java.lang.Integer" -> "java.util.Optional.ofNullable(" + raw + ").map(java.lang.Integer::parseInt)";
      case "java.lang.Double" -> "java.util.Optional.ofNullable(" + raw + ").map(java.lang.Double::parseDouble)";
      case "java.lang.Float" -> "java.util.Optional.ofNullable(" + raw + ").map(java.lang.Float::parseFloat)";
      case "java.lang.Boolean" -> "java.util.Optional.ofNullable(" + raw + ").map(java.lang.Boolean::parseBoolean)";
      case "java.lang.Short" -> "java.util.Optional.ofNullable(" + raw + ").map(java.lang.Short::parseShort)";
      case "java.lang.Byte" -> "java.util.Optional.ofNullable(" + raw + ").map(java.lang.Byte::parseByte)";
      default -> "java.util.Optional.ofNullable(" + raw + ")";
    };
  }

  private String label(final RequestBinding.Source source) {
    return switch (source) {
      case PATH -> "path variable";
      case HEADER -> "header";
      case COOKIE -> "cookie";
      case BODY -> "request body";
      default -> "query parameter";
    };
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
}
