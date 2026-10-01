package cc.asylum.iridium.web.processor.route;

import cc.asylum.forgery.model.TypeRef;
import cc.asylum.forgery.type.ClassBuilder;
import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.codegen.write.NestedTypes;
import cc.asylum.iridium.core.validation.Valid;
import cc.asylum.iridium.web.controller.RestController;
import cc.asylum.iridium.web.controller.mapping.HttpMapping;
import cc.asylum.iridium.web.controller.parameter.BindingSource;
import cc.asylum.iridium.web.controller.parameter.RequestBinding;
import cc.asylum.iridium.web.controller.parameter.RequestBody;
import cc.asylum.iridium.web.processor.MirrorSupport;
import cc.asylum.iridium.web.router.Request;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class RouteWriterGenerateTest {

  @Test
  void writesRoutesAndSkips() {
    final Types types = mock(Types.class);
    final Elements elements = mock(Elements.class);
    final Processing processing = new Processing(types, elements, mock(Messager.class), mock(Filer.class), mock(RoundEnvironment.class));
    final RouteWriter writer = new RouteWriter(processing);

    assertNotNull(writer.beanLookup());
    assertTrue(writer.prefixOf(controller("")).isEmpty());
    assertTrue(RouteWriter.handlerMethods(controllerWith(field(), method("get", List.of()))).size() == 1);

    final ExecutableElement unmapped = method("plain", List.of());
    assertTrue(writer.mappingOf(unmapped).isEmpty());
    final ExecutableElement mapped = method("list", List.of());
    mapped(mapped, "GET", "/users");
    assertTrue(writer.mappingOf(mapped).isPresent());

    final TypeElement controller = controller("/api");
    when(controller.asType()).thenAnswer(invocation -> declared(types, "cc.asylum.Users"));
    final NestedTypes nested = (name, configure) -> {
      configure.accept(mock(ClassBuilder.class, Answers.RETURNS_DEEP_STUBS));
      return TypeRef.of(name);
    };

    final VariableElement request = parameter("request", requestType(types, elements), List.of(), null);
    assertTrue(writer.routeHandler(nested, controller, method("show", List.of(request))).isPresent());

    final VariableElement body = parameter(
      "payload",
      declared(types, "java.lang.String"),
      List.of(binding(BindingSource.BODY, "", true, "")),
      RequestBody.class);
    assertTrue(writer.routeHandler(nested, controller, method("save", List.of(body))).isPresent());

    final VariableElement valid = parameter("name", declared(types, "java.lang.String"), List.of(binding(BindingSource.QUERY, "name", true, "")), Valid.class);
    assertTrue(writer.routeHandler(nested, controller, method("check", List.of(valid))).isPresent());

    assertTrue(writer.routeHandler(nested, controller, method("index", List.of(request))).isPresent());

    assertNotNull(writer.args(constructor(List.of()), processing));
    assertNotNull(writer.args(null, processing));
    assertFalse(writer.mappingOf(method("none", List.of())).isPresent());
  }

  private static TypeElement controller(final String prefix) {
    final TypeElement controller = mock(TypeElement.class);
    when(controller.getSimpleName()).thenAnswer(invocation -> MirrorSupport.name("Users"));
    when(controller.getKind()).thenAnswer(invocation -> ElementKind.CLASS);
    when(controller.getEnclosedElements()).thenAnswer(invocation -> List.of());
    when(controller.getAnnotationMirrors()).thenAnswer(invocation -> List.of());
    if (!prefix.isEmpty()) {
      final AnnotationMirror rest = MirrorSupport.annotation(null, RestController.class.getCanonicalName(), Map.of("value", prefix));
      when(controller.getAnnotationMirrors()).thenAnswer(invocation -> List.of(rest));
    }
    return controller;
  }

  private static TypeElement controllerWith(final Element... enclosed) {
    final TypeElement controller = controller("");
    when(controller.getEnclosedElements()).thenAnswer(invocation -> List.of(enclosed));
    return controller;
  }

  private static Element field() {
    final Element field = mock(Element.class);
    when(field.getKind()).thenAnswer(invocation -> ElementKind.FIELD);
    return field;
  }

  private static ExecutableElement method(final String simple, final List<VariableElement> parameters) {
    final ExecutableElement method = mock(ExecutableElement.class);
    when(method.getKind()).thenAnswer(invocation -> ElementKind.METHOD);
    when(method.getSimpleName()).thenAnswer(invocation -> MirrorSupport.name(simple));
    when(method.getParameters()).thenAnswer(invocation -> parameters);
    when(method.getAnnotationMirrors()).thenAnswer(invocation -> List.of());
    when(method.getAnnotation(Valid.class)).thenAnswer(invocation -> null);
    return method;
  }

  private static ExecutableElement constructor(final List<VariableElement> parameters) {
    final ExecutableElement constructor = mock(ExecutableElement.class);
    when(constructor.getKind()).thenAnswer(invocation -> ElementKind.CONSTRUCTOR);
    when(constructor.getParameters()).thenAnswer(invocation -> parameters);
    return constructor;
  }

  private static void mapped(final ExecutableElement method, final String http, final String path) {
    final AnnotationMirror mirror = MirrorSupport.annotation(HttpMapping.class, "cc.asylum.Get", Map.of("value", path));
    when(mirror.getAnnotationType().asElement().getAnnotation(HttpMapping.class)).thenAnswer(invocation -> new HttpMapping() {
      @Override
      public String method() {
        return http;
      }

      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return HttpMapping.class;
      }
    });
    when(method.getAnnotationMirrors()).thenAnswer(invocation -> List.of(mirror));
  }

  private static VariableElement parameter(
      final String simple,
      final TypeMirror type,
      final List<AnnotationMirror> mirrors,
      final Class<? extends java.lang.annotation.Annotation> present
  ) {
    final VariableElement parameter = mock(VariableElement.class);
    when(parameter.getSimpleName()).thenAnswer(invocation -> MirrorSupport.name(simple));
    when(parameter.asType()).thenAnswer(invocation -> type);
    when(parameter.getAnnotationMirrors()).thenAnswer(invocation -> mirrors);
    when(parameter.getAnnotation(RequestBody.class)).thenAnswer(invocation -> present == RequestBody.class ? requestBody() : null);
    when(parameter.getAnnotation(Valid.class)).thenAnswer(invocation -> present == Valid.class ? valid() : null);
    return parameter;
  }

  private static AnnotationMirror binding(final BindingSource source, final String value, final boolean required, final String fallback) {
    final AnnotationMirror mirror = MirrorSupport.annotation(RequestBinding.class, "cc.asylum.Bind", Map.of(
      "value", value,
      "required", required,
      "defaultValue", fallback));
    when(mirror.getAnnotationType().asElement().getAnnotation(RequestBinding.class)).thenAnswer(invocation -> new RequestBinding() {
      @Override
      public BindingSource value() {
        return source;
      }

      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return RequestBinding.class;
      }
    });
    return mirror;
  }

  private static TypeMirror requestType(final Types types, final Elements elements) {
    final TypeMirror type = MirrorSupport.type(TypeKind.DECLARED, Request.class.getCanonicalName());
    final TypeElement element = mock(TypeElement.class);
    when(element.asType()).thenAnswer(invocation -> type);
    when(elements.getTypeElement(Request.class.getCanonicalName())).thenAnswer(invocation -> element);
    when(types.isSameType(type, type)).thenAnswer(invocation -> true);
    return type;
  }

  private static TypeMirror declared(final Types types, final String qualified) {
    final DeclaredType type = MirrorSupport.declared(qualified);
    when(types.asElement(type)).thenAnswer(invocation -> type.asElement());
    return type;
  }

  private static RequestBody requestBody() {
    return new RequestBody() {
      @Override
      public boolean required() {
        return true;
      }

      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return RequestBody.class;
      }
    };
  }

  private static Valid valid() {
    return new Valid() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Valid.class;
      }
    };
  }
}
