package cc.asylum.iridium.codegen.processor;

import com.io7m.jodist.ClassName;
import com.io7m.jodist.CodeBlock;
import com.io7m.jodist.MethodSpec;
import com.io7m.jodist.ParameterizedTypeName;
import com.io7m.jodist.TypeName;
import com.io7m.jodist.TypeSpec;
import com.io7m.jodist.WildcardTypeName;
import cc.asylum.iridium.codegen.IridiumProcessor;
import cc.asylum.iridium.core.bean.BeanPool;
import cc.asylum.iridium.core.component.Component;
import cc.asylum.iridium.web.controller.Parameters;
import cc.asylum.iridium.web.controller.RestController;
import cc.asylum.iridium.web.controller.mapping.HttpMapping;
import cc.asylum.iridium.web.controller.parameter.RequestBinding;
import cc.asylum.iridium.web.middleware.Middleware;
import cc.asylum.iridium.web.response.Response;
import cc.asylum.iridium.web.router.Handler;
import cc.asylum.iridium.web.router.Request;
import cc.asylum.iridium.web.router.Router;
import cc.asylum.iridium.web.webserver.WebRegistrar;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("*")
public final class WebProcessor extends IridiumProcessor {

  private static final String GENERATED_CLASS = "WebRegistrarGenerated";
  private static final ClassName ROUTER = ClassName.get(Router.class);
  private static final ClassName HANDLER = ClassName.get(Handler.class);
  private static final ClassName REQUEST_CLASS = ClassName.get(Request.class);
  private static final ClassName RESPONSE = ClassName.get(Response.class);
  private static final ClassName PARAMETERS = ClassName.get(Parameters.class);
  private static final ClassName BEAN_POOL = ClassName.get(BeanPool.class);

  @Override
  protected void processRound(final RoundEnvironment roundEnv) {
    final Set<TypeElement> controllers = new LinkedHashSet<>();
    final Set<TypeElement> middlewares = new LinkedHashSet<>();
    for (final TypeElement type : rootTypes(roundEnv, ElementKind.CLASS)) {
      if (type.getAnnotation(RestController.class) != null) {
        controllers.add(type);
      }
      if (type.getAnnotation(Component.class) != null && isAssignable(type.asType(), Middleware.class)) {
        middlewares.add(type);
      }
    }
    if (controllers.isEmpty() && middlewares.isEmpty()) {
      return;
    }
    generate(roundEnv, controllers, middlewares);
  }

  private void generate(final RoundEnvironment roundEnv, final Set<TypeElement> controllers,
      final Set<TypeElement> middlewares) {
    final String pkg = generatedPackage(rootTypes(roundEnv, ElementKind.CLASS, ElementKind.RECORD));

    final MethodSpec.Builder register = MethodSpec.methodBuilder("register")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ROUTER, "router");

    final CodeBlock pool = CodeBlock.of("$T.instance()", BEAN_POOL);
    for (final TypeElement middleware : middlewares) {
      register.addStatement("router.use(new $T($L))",
          ClassName.get(middleware), dependencyArgs(resolveConstructor(middleware), pool));
    }

    int controllerIndex = 0;
    for (final TypeElement controller : controllers) {
      final ClassName type = ClassName.get(controller);
      final String varName = "controller" + controllerIndex++;
      final ExecutableElement constructor = resolveConstructor(controller);
      if (constructor == null) {
        register.addStatement("final $T $L = new $T()", type, varName, type);
      } else {
        register.addStatement("final $T $L = new $T($L)",
            type, varName, type, dependencyArgs(constructor, pool));
      }
      for (final Element enclosed : controller.getEnclosedElements()) {
        if (enclosed.getKind() != ElementKind.METHOD) {
          continue;
        }
        final ExecutableElement method = (ExecutableElement) enclosed;
        final MethodMapping mapping = mappingOf(method);
        if (mapping == null) {
          continue;
        }
        register.addStatement("router.register($S, $S, $L)",
            mapping.httpMethod(), mapping.path(), routeHandler(varName, method));
      }
    }

    writeJava(pkg, generatedType(GENERATED_CLASS)
        .addSuperinterface(ClassName.get(WebRegistrar.class))
        .addMethod(register.build())
        .build());
    writeService(WebRegistrar.class, pkg + "." + GENERATED_CLASS);
  }

  private TypeSpec routeHandler(final String controllerVar, final ExecutableElement method) {
    final TypeName responseWildcard = ParameterizedTypeName.get(RESPONSE, WildcardTypeName.subtypeOf(TypeName.OBJECT));
    final MethodSpec.Builder handle = MethodSpec.methodBuilder("handle")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(REQUEST_CLASS, "_request")
        .addException(Exception.class)
        .returns(responseWildcard);

    final List<String> arguments = new ArrayList<>();
    for (final VariableElement parameter : method.getParameters()) {
      final String argument = emitBinding(handle, parameter);
      if (argument == null) {
        return null;
      }
      arguments.add(argument);
    }

    handle.addStatement("return $L.$N($L)",
        controllerVar, method.getSimpleName().toString(), String.join(", ", arguments));
    return TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(HANDLER)
        .addMethod(handle.build())
        .build();
  }

  private String emitBinding(final MethodSpec.Builder handle, final VariableElement parameter) {
    final TypeMirror type = parameter.asType();
    final String typeName = type.toString();
    final String name = parameter.getSimpleName().toString();

    if (Request.class.getName().equals(typeName)) {
      return "_request";
    }

    final AnnotationMirror bindingMirror = annotationWithMeta(parameter, RequestBinding.class);
    final RequestBinding binding = bindingMirror == null
        ? null
        : bindingMirror.getAnnotationType().asElement().getAnnotation(RequestBinding.class);
    final RequestBinding.Source source = binding == null ? RequestBinding.Source.BODY : binding.value();

    if (source == RequestBinding.Source.ATTRIBUTE) {
      error(parameter, "@RequestAttribute is not supported yet");
      return null;
    }

    final boolean optional = typeName.startsWith("java.util.Optional<");
    final boolean required = optional ? false : booleanMember(bindingMirror, "required", true);
    final String defaultValue = optional ? "" : stringMember(bindingMirror, "defaultValue", "");
    final String bindingName = stringMember(bindingMirror, "value", name);

    if (source == RequestBinding.Source.BODY) {
      return emitBody(handle, parameter, typeName, name, binding == null || required);
    }

    if (!defaultValue.isEmpty()) {
      handle.addStatement("$L $L = $L", typeName, name, convert(type, rawExpr(source, bindingName, defaultValue)));
      return name;
    }

    if (required) {
      final String rawVar = name + "Raw";
      handle.addStatement("java.lang.String $L = $L", rawVar, rawExpr(source, bindingName, null));
      handle.addStatement("if ($L == null) { return $T.badRequest().body($S); }",
          rawVar, RESPONSE, "Missing required " + label(source) + " '" + bindingName + "'");
      handle.addStatement("$L $L = $L", typeName, name, convert(type, rawVar));
      return name;
    }

    handle.addStatement("$L $L = $L", typeName, name,
        optional
            ? optionalWrap(type, rawExpr(source, bindingName, null))
            : convert(type, rawExpr(source, bindingName, null)));
    return name;
  }

  private String emitBody(final MethodSpec.Builder handle, final VariableElement parameter,
      final String typeName, final String name, final boolean required) {
    final boolean optional = typeName.startsWith("java.util.Optional<");
    if (required && !optional) {
      final String rawVar = name + "Raw";
      handle.addStatement("java.lang.String $L = $L", rawVar, rawExpr(RequestBinding.Source.BODY, name, null));
      handle.addStatement("if ($L == null) { return $T.badRequest().body($S); }",
          rawVar, RESPONSE, "Missing required request body");
      handle.addStatement("$L $L = $L", typeName, name, convert(parameter.asType(), rawVar));
      return name;
    }
    handle.addStatement("$L $L = $L", typeName, name,
        convert(parameter.asType(), rawExpr(RequestBinding.Source.BODY, name, null)));
    return name;
  }

  private String rawExpr(final RequestBinding.Source source, final String name, final String defaultValue) {
    final String value = defaultValue == null ? "null" : "\"" + defaultValue + "\"";
    return switch (source) {
      case PATH -> PARAMETERS + ".pathVariable(_request, \"" + name + "\", " + value + ")";
      case HEADER -> PARAMETERS + ".header(_request, \"" + name + "\", " + value + ")";
      case COOKIE -> PARAMETERS + ".cookie(_request, \"" + name + "\", " + value + ")";
      case BODY -> PARAMETERS + ".body(_request)";
      default -> PARAMETERS + ".query(_request, \"" + name + "\", " + value + ")";
    };
  }

  private String convert(final TypeMirror type, final String raw) {
    return switch (type.toString()) {
      case "java.lang.String" -> raw;
      case "int" -> "Integer.parseInt(" + raw + ")";
      case "long" -> "Long.parseLong(" + raw + ")";
      case "double" -> "Double.parseDouble(" + raw + ")";
      case "float" -> "Float.parseFloat(" + raw + ")";
      case "boolean" -> "Boolean.parseBoolean(" + raw + ")";
      case "short" -> "Short.parseShort(" + raw + ")";
      case "byte" -> "Byte.parseByte(" + raw + ")";
      case "char" -> raw + ".charAt(0)";
      case "java.lang.Integer" -> "Integer.valueOf(" + raw + ")";
      case "java.lang.Long" -> "Long.valueOf(" + raw + ")";
      case "java.lang.Double" -> "Double.valueOf(" + raw + ")";
      case "java.lang.Float" -> "Float.valueOf(" + raw + ")";
      case "java.lang.Boolean" -> "Boolean.valueOf(" + raw + ")";
      case "java.lang.Short" -> "Short.valueOf(" + raw + ")";
      case "java.lang.Byte" -> "Byte.valueOf(" + raw + ")";
      case "java.lang.Character" -> raw + ".charAt(0)";
      default -> rootPackage() + ".json.Json.load()"
          + ".deserialize(" + raw + ", " + types.erasure(type) + ".class)";
    };
  }

  private String optionalWrap(final TypeMirror type, final String raw) {
    final String fqcn = type.toString();
    final String inner = fqcn.substring("java.util.Optional<".length(), fqcn.length() - 1);
    return switch (inner) {
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

  private MethodMapping mappingOf(final ExecutableElement method) {
    final HttpMapping mapping = metaAnnotation(method, HttpMapping.class);
    if (mapping == null) {
      return null;
    }
    final AnnotationMirror mirror = annotationWithMeta(method, HttpMapping.class);
    return new MethodMapping(mapping.method(), stringMember(mirror, "value", ""));
  }

  private record MethodMapping(String httpMethod, String path) {
  }
}
