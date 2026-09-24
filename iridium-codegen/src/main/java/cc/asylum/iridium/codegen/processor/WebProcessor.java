package cc.asylum.iridium.codegen.processor;

import com.io7m.jodist.ClassName;
import com.io7m.jodist.MethodSpec;
import cc.asylum.iridium.codegen.IridiumProcessor;
import cc.asylum.iridium.codegen.support.BindingContext;
import cc.asylum.iridium.codegen.support.ModelSupport;
import cc.asylum.iridium.codegen.support.SourceWriter;
import cc.asylum.iridium.codegen.support.TypeSupport;
import cc.asylum.iridium.codegen.writer.RouteWriter;
import cc.asylum.iridium.core.component.Component;
import cc.asylum.iridium.web.controller.RestController;
import cc.asylum.iridium.web.middleware.Middleware;
import cc.asylum.iridium.web.router.Router;
import cc.asylum.iridium.web.webserver.WebRegistrar;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class WebProcessor extends IridiumProcessor {

  private static final String GENERATED_CLASS = "WebRegistrarGenerated";
  private static final ClassName ROUTER = ClassName.get(Router.class);

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(
        RestController.class.getCanonicalName(),
        Component.class.getCanonicalName());
  }

  @Override
  protected void processRound(final RoundEnvironment roundEnv) {
    final Set<TypeElement> controllers = new LinkedHashSet<>();
    final Set<TypeElement> middlewares = new LinkedHashSet<>();
    for (final TypeElement type : ModelSupport.rootTypes(roundEnv, ElementKind.CLASS)) {
      if (type.getAnnotation(RestController.class) != null) {
        controllers.add(type);
      }
      if (type.getAnnotation(Component.class) != null
          && TypeSupport.isAssignable(types, elements, type.asType(), Middleware.class)) {
        middlewares.add(type);
      }
    }
    if (controllers.isEmpty() && middlewares.isEmpty()) {
      return;
    }
    generate(roundEnv, controllers, middlewares);
  }

  private void generate(
      final RoundEnvironment roundEnv,
      final Set<TypeElement> controllers,
      final Set<TypeElement> middlewares
  ) {
    final List<TypeElement> roots = ModelSupport.rootTypes(roundEnv, ElementKind.CLASS, ElementKind.RECORD);
    final String pkg = ModelSupport.generatedPackage(elements, roots);
    final var routes = new RouteWriter(types, elements, messager);
    final var binding = new BindingContext(types, elements, messager);

    final MethodSpec.Builder register = MethodSpec.methodBuilder("register")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ROUTER, "router");

    for (final TypeElement middleware : middlewares) {
      register.addStatement("router.use(new $T($L))",
          ClassName.get(middleware),
          ModelSupport.dependencyArgs(
              ModelSupport.resolveConstructor(middleware),
              routes.beanLookupPool(),
              binding
          )
      );
    }

    int controllerIndex = 0;
    for (final TypeElement controller : controllers) {
      final ClassName type = ClassName.get(controller);
      final String varName = "controller" + controllerIndex++;
      final String prefix = routes.prefixOf(controller);
      final ExecutableElement constructor = ModelSupport.resolveConstructor(controller);
      if (constructor == null) {
        register.addStatement("final $T $L = new $T()", type, varName, type);
      } else {
        register.addStatement("final $T $L = new $T($L)",
            type,
            varName,
            type,
            ModelSupport.dependencyArgs(constructor, routes.beanLookupPool(), binding)
        );
      }
      for (final ExecutableElement method : RouteWriter.handlerMethods(controller)) {
        final var mapping = routes.mappingOf(method);
        if (mapping.isEmpty()) {
          continue;
        }
        final var handler = routes.routeHandler(varName, method);
        if (handler.isEmpty()) {
          continue;
        }
        register.addStatement("router.register($S, $S, $L)",
            mapping.get().httpMethod(), RouteWriter.resolvePath(prefix, mapping.get().path()), handler.get());
      }
    }

    SourceWriter.writeJava(filer, pkg, SourceWriter.generatedType(GENERATED_CLASS)
        .addSuperinterface(ClassName.get(WebRegistrar.class))
        .addMethod(register.build())
        .build(), roots.toArray(new TypeElement[0]));
    SourceWriter.writeService(filer, WebRegistrar.class, pkg + "." + GENERATED_CLASS,
        roots.toArray(new TypeElement[0]));
  }
}
