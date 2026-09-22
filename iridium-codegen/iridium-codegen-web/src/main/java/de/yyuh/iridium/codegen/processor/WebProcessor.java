package de.yyuh.iridium.codegen.processor;

import com.io7m.jodist.AnnotationSpec;
import com.io7m.jodist.ClassName;
import com.io7m.jodist.CodeBlock;
import com.io7m.jodist.JavaFile;
import com.io7m.jodist.MethodSpec;
import com.io7m.jodist.ParameterizedTypeName;
import com.io7m.jodist.TypeName;
import com.io7m.jodist.TypeSpec;
import com.io7m.jodist.WildcardTypeName;
import de.yyuh.iridium.codegen.annotation.Generated;
import de.yyuh.iridium.core.bean.BeanPool;
import de.yyuh.iridium.core.component.Component;
import de.yyuh.iridium.core.inject.Inject;
import dev.yyuh.iridium.web.WebApplication;
import dev.yyuh.iridium.web.router.Handler;
import dev.yyuh.iridium.web.router.Request;
import dev.yyuh.iridium.web.router.Router;
import dev.yyuh.iridium.web.webserver.WebRegistrar;
import dev.yyuh.iridium.web.webserver.WebServer;
import dev.yyuh.iridium.web.controller.Parameters;
import dev.yyuh.iridium.web.controller.RestController;
import dev.yyuh.iridium.web.controller.mapping.DELETE;
import dev.yyuh.iridium.web.controller.mapping.GET;
import dev.yyuh.iridium.web.controller.mapping.HEAD;
import dev.yyuh.iridium.web.controller.mapping.OPTIONS;
import dev.yyuh.iridium.web.controller.mapping.PATCH;
import dev.yyuh.iridium.web.controller.mapping.POST;
import dev.yyuh.iridium.web.controller.mapping.PUSH;
import dev.yyuh.iridium.web.controller.mapping.PUT;
import dev.yyuh.iridium.web.controller.parameter.CookieValue;
import dev.yyuh.iridium.web.controller.parameter.PathVariable;
import dev.yyuh.iridium.web.controller.parameter.RequestAttribute;
import dev.yyuh.iridium.web.controller.parameter.RequestBody;
import dev.yyuh.iridium.web.controller.parameter.RequestHeader;
import dev.yyuh.iridium.web.controller.parameter.RequestParam;
import dev.yyuh.iridium.web.middleware.Middleware;
import dev.yyuh.iridium.web.response.Response;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SupportedAnnotationTypes("*")
public final class WebProcessor extends AbstractProcessor {

    private static final String GENERATED_CLASS = "WebRegistrarGenerated";
    private static final String DEFAULT_PACKAGE = "de.yyuh.iridium.gen";
    private static final String AUTHOR = "Iridium";

    private static final Map<Class<? extends Annotation>, String> MAPPINGS = Map.ofEntries(
            Map.entry(GET.class, "GET"),
            Map.entry(POST.class, "POST"),
            Map.entry(PUT.class, "PUT"),
            Map.entry(DELETE.class, "DELETE"),
            Map.entry(PATCH.class, "PATCH"),
            Map.entry(HEAD.class, "HEAD"),
            Map.entry(OPTIONS.class, "OPTIONS"),
            Map.entry(PUSH.class, "PUSH")
    );

    private static final ClassName ROUTER = ClassName.get(Router.class);
    private static final ClassName WEB_REGISTRAR = ClassName.get(WebRegistrar.class);
    private static final ClassName WEB_SERVER = ClassName.get(WebServer.class);
    private static final ClassName HANDLER = ClassName.get(Handler.class);
    private static final ClassName REQUEST_CLASS = ClassName.get(Request.class);
    private static final ClassName RESPONSE = ClassName.get(Response.class);
    private static final ClassName PARAMETERS = ClassName.get(Parameters.class);
    private static final ClassName BEAN_POOL = ClassName.get(BeanPool.class);

    private enum Binding {
        PATH_VARIABLE(PathVariable.class),
        REQUEST_PARAM(RequestParam.class),
        REQUEST_HEADER(RequestHeader.class),
        REQUEST_BODY(RequestBody.class),
        REQUEST_ATTRIBUTE(RequestAttribute.class),
        COOKIE_VALUE(CookieValue.class);

        private final Class<? extends Annotation> annotation;

        Binding(final Class<? extends Annotation> annotation) {
            this.annotation = annotation;
        }
    }

    private Elements elements;
    private Types types;

    @Override
    public synchronized void init(final javax.annotation.processing.ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        final Set<TypeElement> controllers = new LinkedHashSet<>();
        final Set<TypeElement> middlewares = new LinkedHashSet<>();
        final Set<TypeElement> applications = new LinkedHashSet<>();

        for (final Element root : roundEnv.getRootElements()) {
            if (root.getKind() != ElementKind.CLASS) {
                continue;
            }
            final TypeElement type = (TypeElement) root;
            if (type.getAnnotation(RestController.class) != null) {
                controllers.add(type);
            }
            if (type.getAnnotation(Component.class) != null && implementsMiddleware(type)) {
                middlewares.add(type);
            }
            if (type.getAnnotation(WebApplication.class) != null) {
                applications.add(type);
            }
        }

        if (controllers.isEmpty() && middlewares.isEmpty() && applications.isEmpty()) {
            return false;
        }

        if (!controllers.isEmpty() || !middlewares.isEmpty()) {
            generate(controllers, middlewares);
        }

        for (final TypeElement application : applications) {
            generateBootstrap(application);
        }
        return false;
    }

    private void generateBootstrap(final TypeElement application) {
        final WebApplication config = application.getAnnotation(WebApplication.class);
        final int port = config.port();
        final String host = config.host();

        final MethodSpec main = MethodSpec.methodBuilder("main")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(String[].class, "args")
                .addStatement("$T.initialize()", BEAN_POOL)
                .addStatement("$T server = $T.instance().get($T.class)", WEB_SERVER, BEAN_POOL, WEB_SERVER)
                .addStatement("if (server == null) { throw new $T($S); }",
                        IllegalStateException.class, "No WebServer bean registered")
                .addStatement("server.start($L, $S)", port, host)
                .addStatement("server.registerRoutes()")
                .addStatement("$T.out.println($S)", System.class,
                        "Iridium application listening on http://localhost:" + port)
                .build();

        final AnnotationSpec generated = AnnotationSpec.builder(ClassName.get(Generated.class))
                .addMember("author", "$S", AUTHOR)
                .addMember("date", "$S", Instant.now().toString())
                .build();

        final TypeSpec typeSpec = TypeSpec.classBuilder(application.getSimpleName() + "Bootstrap")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(generated)
                .addMethod(main)
                .build();

        final JavaFile javaFile = JavaFile.builder(packageOf(application), typeSpec).build();
        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (final IOException e) {
            throw new RuntimeException("Failed to generate bootstrap for " + application.getQualifiedName(), e);
        }
    }

    private void generate(final Set<TypeElement> controllers, final Set<TypeElement> middlewares) {
        final Set<String> packages = new LinkedHashSet<>();
        for (final TypeElement controller : controllers) {
            packages.add(packageOf(controller));
        }
        for (final TypeElement middleware : middlewares) {
            packages.add(packageOf(middleware));
        }
        final String basePackage = commonPrefix(packages);
        final String generatedPackage = basePackage.isEmpty() ? DEFAULT_PACKAGE : basePackage + ".gen";

        final MethodSpec.Builder register = MethodSpec.methodBuilder("register")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ROUTER, "router");

        for (final TypeElement middleware : middlewares) {
            final ClassName type = ClassName.get(middleware);
            register.addStatement("router.use(new $T($L))", type, dependencyArgs(resolveConstructor(middleware)));
        }

        int controllerIndex = 0;
        for (final TypeElement controller : controllers) {
            final ClassName type = ClassName.get(controller);
            final String varName = "controller" + controllerIndex++;

            final ExecutableElement constructor = resolveConstructor(controller);
            if (constructor == null) {
                register.addStatement("final $T $L = new $T()", type, varName, type);
            } else {
                register.addStatement("final $T $L = new $T($L)", type, varName, type, dependencyArgs(constructor));
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

        final AnnotationSpec generated = AnnotationSpec.builder(ClassName.get(Generated.class))
                .addMember("author", "$S", AUTHOR)
                .addMember("date", "$S", Instant.now().toString())
                .build();

        final TypeSpec typeSpec = TypeSpec.classBuilder(GENERATED_CLASS)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(WEB_REGISTRAR)
                .addAnnotation(generated)
                .addMethod(register.build())
                .build();

        final JavaFile javaFile = JavaFile.builder(generatedPackage, typeSpec).build();
        try {
            javaFile.writeTo(processingEnv.getFiler());
            writeServiceFile(generatedPackage);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to generate " + generatedPackage + "." + GENERATED_CLASS, e);
        }
    }

    private TypeSpec routeHandler(final String controllerVar, final ExecutableElement method) {
        final TypeName responseWildcard = ParameterizedTypeName.get(RESPONSE,
                WildcardTypeName.subtypeOf(TypeName.OBJECT));

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

        final Binding binding = bindingOf(parameter);
        if (binding == null) {
            return emitBody(handle, parameter, typeName, name, true);
        }

        if (binding == Binding.REQUEST_ATTRIBUTE) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@RequestAttribute is not supported yet", parameter);
            return null;
        }

        final boolean optional = typeName.startsWith("java.util.Optional<");
        final boolean required = optional ? false : requiredOf(binding, parameter);
        final String defaultValue = optional ? "" : defaultValueOf(binding, parameter);
        final String bindingName = bindingNameOf(binding, parameter, name);

        if (!defaultValue.isEmpty()) {
            handle.addStatement("$L $L = $L",
                    typeName, name, convert(type, rawExpr(binding, bindingName, defaultValue)));
            return name;
        }

        if (required) {
            final String rawVar = name + "Raw";
            handle.addStatement("java.lang.String $L = $L", rawVar, rawExpr(binding, bindingName, null));
            handle.addStatement("if ($L == null) { return $T.badRequest().body($S); }",
                    rawVar, RESPONSE, "Missing required " + label(binding) + " '" + bindingName + "'");
            handle.addStatement("$L $L = $L", typeName, name, convert(type, rawVar));
            return name;
        }

        handle.addStatement("$L $L = $L", typeName, name,
                optional
                        ? optionalWrap(type, rawExpr(binding, bindingName, null))
                        : convert(type, rawExpr(binding, bindingName, null)));
        return name;
    }

    private String emitBody(final MethodSpec.Builder handle, final VariableElement parameter,
                            final String typeName, final String name, final boolean required) {
        final boolean optional = typeName.startsWith("java.util.Optional<");
        if (required && !optional) {
            final String rawVar = name + "Raw";
            handle.addStatement("java.lang.String $L = $L", rawVar, rawExpr(Binding.REQUEST_BODY, name, null));
            handle.addStatement("if ($L == null) { return $T.badRequest().body($S); }",
                    rawVar, RESPONSE, "Missing required request body");
            handle.addStatement("$L $L = $L", typeName, name, convert(parameter.asType(), rawVar));
            return name;
        }
        handle.addStatement("$L $L = $L", typeName, name,
                convert(parameter.asType(), rawExpr(Binding.REQUEST_BODY, name, null)));
        return name;
    }

    private String rawExpr(final Binding binding, final String name, final String defaultValue) {
        final String value = defaultValue == null ? "null" : "\"" + defaultValue + "\"";
        return switch (binding) {
            case PATH_VARIABLE -> PARAMETERS.toString() + ".pathVariable(_request, \"" + name + "\", " + value + ")";
            case REQUEST_HEADER -> PARAMETERS.toString() + ".header(_request, \"" + name + "\", " + value + ")";
            case COOKIE_VALUE -> PARAMETERS.toString() + ".cookie(_request, \"" + name + "\", " + value + ")";
            case REQUEST_BODY -> PARAMETERS.toString() + ".body(_request)";
            default -> PARAMETERS.toString() + ".query(_request, \"" + name + "\", " + value + ")";
        };
    }

    private String convert(final TypeMirror type, final String raw) {
        final String fqcn = type.toString();
        return switch (fqcn) {
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
            default -> BeanPool.class.getName() + ".instance()"
                    + ".get(dev.yyuh.iridium.json.Json.class)"
                    + ".deserialize(" + raw + ", " + types.erasure(type).toString() + ".class)";
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

    private String label(final Binding binding) {
        return switch (binding) {
            case PATH_VARIABLE -> "path variable";
            case REQUEST_HEADER -> "header";
            case COOKIE_VALUE -> "cookie";
            case REQUEST_BODY -> "request body";
            default -> "query parameter";
        };
    }

    private Binding bindingOf(final VariableElement parameter) {
        if (parameter.getAnnotation(PathVariable.class) != null) {
            return Binding.PATH_VARIABLE;
        }
        if (parameter.getAnnotation(RequestParam.class) != null) {
            return Binding.REQUEST_PARAM;
        }
        if (parameter.getAnnotation(RequestHeader.class) != null) {
            return Binding.REQUEST_HEADER;
        }
        if (parameter.getAnnotation(RequestBody.class) != null) {
            return Binding.REQUEST_BODY;
        }
        if (parameter.getAnnotation(RequestAttribute.class) != null) {
            return Binding.REQUEST_ATTRIBUTE;
        }
        if (parameter.getAnnotation(CookieValue.class) != null) {
            return Binding.COOKIE_VALUE;
        }
        return null;
    }

    private String bindingNameOf(final Binding binding, final VariableElement parameter, final String fallback) {
        return switch (binding) {
            case PATH_VARIABLE, REQUEST_PARAM, REQUEST_HEADER, COOKIE_VALUE ->
                    stringValue(parameter, binding.annotation, "value", fallback);
            default -> fallback;
        };
    }

    private boolean requiredOf(final Binding binding, final VariableElement parameter) {
        return switch (binding) {
            case REQUEST_PARAM, REQUEST_HEADER, REQUEST_BODY, COOKIE_VALUE ->
                    booleanValue(parameter, binding.annotation, "required", true);
            default -> true;
        };
    }

    private String defaultValueOf(final Binding binding, final VariableElement parameter) {
        return switch (binding) {
            case REQUEST_PARAM, REQUEST_HEADER, COOKIE_VALUE ->
                    stringValue(parameter, binding.annotation, "defaultValue", "");
            default -> "";
        };
    }

    private MethodMapping mappingOf(final ExecutableElement method) {
        for (final Map.Entry<Class<? extends Annotation>, String> entry : MAPPINGS.entrySet()) {
            if (method.getAnnotation(entry.getKey()) != null) {
                return new MethodMapping(entry.getValue(), stringValue(method, entry.getKey(), "value", ""));
            }
        }
        return null;
    }

    private record MethodMapping(String httpMethod, String path) {
    }

    private boolean implementsMiddleware(final TypeElement type) {
        final TypeElement middleware = elements.getTypeElement(Middleware.class.getName());
        return middleware != null && types.isAssignable(type.asType(), middleware.asType());
    }

    private ExecutableElement resolveConstructor(final TypeElement type) {
        final List<ExecutableElement> constructors = new ArrayList<>();
        ExecutableElement annotated = null;
        for (final Element enclosed : type.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
                final ExecutableElement constructor = (ExecutableElement) enclosed;
                constructors.add(constructor);
                if (constructor.getAnnotation(Inject.class) != null) {
                    annotated = constructor;
                }
            }
        }
        if (annotated != null) {
            return annotated;
        }
        return constructors.size() == 1 ? constructors.get(0) : null;
    }

    private CodeBlock dependencyArgs(final ExecutableElement executable) {
        final CodeBlock.Builder args = CodeBlock.builder();
        boolean first = true;
        if (executable != null) {
            for (final VariableElement parameter : executable.getParameters()) {
                if (!first) {
                    args.add(", ");
                }
                first = false;
                args.add("$T.instance().get($T.class)", BEAN_POOL, TypeName.get(parameter.asType()));
            }
        }
        return args.build();
    }

    private AnnotationMirror mirrorOf(final Element element, final Class<? extends Annotation> type) {
        final String name = type.getCanonicalName();
        for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {
            final Element annotationElement = mirror.getAnnotationType().asElement();
            if (annotationElement instanceof final TypeElement typeElement
                    && typeElement.getQualifiedName().contentEquals(name)) {
                return mirror;
            }
        }
        return null;
    }

    private String stringValue(final Element element, final Class<? extends Annotation> type,
                               final String member, final String fallback) {
        final AnnotationMirror mirror = mirrorOf(element, type);
        if (mirror == null) {
            return fallback;
        }
        for (final Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
                : mirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().contentEquals(member)) {
                final Object value = entry.getValue().getValue();
                return value == null ? fallback : value.toString();
            }
        }
        return fallback;
    }

    private boolean booleanValue(final Element element, final Class<? extends Annotation> type,
                                 final String member, final boolean fallback) {
        final AnnotationMirror mirror = mirrorOf(element, type);
        if (mirror == null) {
            return fallback;
        }
        for (final Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
                : mirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().contentEquals(member)) {
                return (Boolean) entry.getValue().getValue();
            }
        }
        return fallback;
    }

    private String packageOf(final Element element) {
        return elements.getPackageOf(element).getQualifiedName().toString();
    }

    private static String commonPrefix(final Set<String> packages) {
        String prefix = null;
        for (final String pkg : packages) {
            prefix = prefix == null ? pkg : commonPrefix(prefix, pkg);
        }
        return prefix == null ? "" : prefix;
    }

    private static String commonPrefix(final String a, final String b) {
        final String[] left = a.split("\\.");
        final String[] right = b.split("\\.");
        final int length = Math.min(left.length, right.length);
        int i = 0;
        while (i < length && left[i].equals(right[i])) {
            i++;
        }
        return String.join(".", Arrays.copyOf(left, i));
    }

    private void writeServiceFile(final String generatedPackage) throws IOException {
        final var serviceFile = processingEnv.getFiler().createResource(
                javax.tools.StandardLocation.CLASS_OUTPUT,
                "",
                "META-INF/services/" + WEB_REGISTRAR.canonicalName()
        );

        try (final Writer writer = serviceFile.openWriter()) {
            writer.write(generatedPackage + "." + GENERATED_CLASS);
            writer.write(System.lineSeparator());
        }
    }
}
