package cc.asylum.iridium.web.processor.route;

import cc.asylum.iridium.codegen.model.Diagnostics;
import cc.asylum.iridium.codegen.model.Elements;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.codegen.write.Registrar;
import cc.asylum.iridium.codegen.model.TypeMirrors;
import cc.asylum.iridium.codegen.code.Types;
import cc.asylum.iridium.core.component.Component;
import cc.asylum.iridium.web.controller.RestController;
import cc.asylum.iridium.web.middleware.Middleware;
import cc.asylum.iridium.web.router.Router;
import cc.asylum.iridium.web.webserver.WebRegistrar;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashSet;
import java.util.Set;

public final class WebModel {

  private WebModel() {
  }

  public static void generate(final Processing processing) {
    final Set<TypeElement> controllers = new LinkedHashSet<>();
    final Set<TypeElement> middlewares = new LinkedHashSet<>();

    for (final TypeElement type : Elements.rootTypes(processing.round(), ElementKind.CLASS)) {
      if (type.getAnnotation(RestController.class) != null) {
        controllers.add(type);
      }

      if (type.getAnnotation(Component.class) != null && TypeMirrors.isAssignable(processing.types(), processing.elements(), type.asType(), Middleware.class)) {
        middlewares.add(type);
      }
    }

    if (controllers.isEmpty() && middlewares.isEmpty()) {
      return;
    }

    final var roots = Elements.rootTypes(processing.round(), ElementKind.CLASS, ElementKind.RECORD);
    final var routes = new RouteWriter(processing);
    final Registrar registrar = Registrar.of("WebRegistrarGenerated", WebRegistrar.class, Router.class, "router");

    use(middlewares, routes, processing, registrar);
    register(controllers, routes, registrar);
    registrar.write(processing.filer(), Elements.generatedPackage(processing.elements(), roots), roots.toArray(new TypeElement[0]));
  }

  private static void use(
    final Set<TypeElement> middlewares,
    final RouteWriter routes,
    final Processing processing,
    final Registrar registrar) {
    for (final TypeElement middleware : middlewares) {
      final ExecutableElement constructor = Elements.constructor(middleware);
      if (constructor == null) {
        Diagnostics.error(processing.messager(), middleware, "type must have exactly one constructor");
        continue;
      }

      registrar.line(registrar.pool().invoke("use", Exprs.new_(Types.of(middleware.asType()), routes.args(constructor, processing))));
    }
  }

  private static void register(final Set<TypeElement> controllers, final RouteWriter routes, final Registrar registrar) {
    for (final TypeElement controller : controllers) {
      final String prefix = routes.prefixOf(controller);
      for (final ExecutableElement method : RouteWriter.handlerMethods(controller)) {
        routes.mappingOf(method).ifPresent(mapping -> registrar.nest(nested -> routes.routeHandler(nested, controller, method).ifPresent(handler -> registrar.line(registrar.pool().invoke("register", Exprs.lit(mapping.httpMethod()), Exprs.lit(RouteWriter.resolvePath(prefix, mapping.path())), handler)))));
      }
    }
  }
}
