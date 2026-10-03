package cc.asylum.iridium.web.processor.client;

import cc.asylum.iridium.web.controller.parameter.BindingSource;
import cc.asylum.iridium.web.controller.parameter.RequestBinding;
import cc.asylum.iridium.web.http.RequestMapping;
import cc.asylum.iridium.web.processor.MirrorSupport;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class ClientMethodTest {

  @Test
  void emitsAndRejects() {
    final Types types = mock(Types.class);
    final Elements elements = mock(Elements.class);
    final Messager messager = mock(Messager.class);
    final ClientMethod writer = new ClientMethod(types, elements, messager);

    final AnnotationMirror empty = mapping(elements, "", "text/plain", null);
    assertNull(writer.emit(method("missing", type(TypeKind.VOID, "void"), List.of()), empty, "http://host"));

    final VariableElement first = parameter("left", requestBinding(BindingSource.BODY, ""));
    final VariableElement second = parameter("right", requestBinding(BindingSource.BODY, ""));
    final AnnotationMirror post = mapping(elements, "/items", "application/json", "RequestMethod.POST");
    assertNull(writer.emit(method("create", type(TypeKind.VOID, "void"), List.of(first, second)), post, "http://host"));

    final VariableElement id = parameter("id", requestBinding(BindingSource.PATH, "id"));
    final VariableElement query = parameter("q", requestBinding(BindingSource.QUERY, ""));
    final VariableElement header = parameter("auth", requestBinding(BindingSource.HEADER, "Authorization"));
    final VariableElement bare = parameter("page", null);
    final TypeMirror stringType = declared(types, "java.lang.String");
    assertNotNull(writer.emit(
      method("get", stringType, List.of(id, query, header, bare)),
      mapping(elements, "/users/{id}", "", "GET"),
      "http://host").configure());

    final TypeMirror bytes = type(TypeKind.ARRAY, "byte[]");
    final VariableElement body = parameter("payload", requestBinding(BindingSource.BODY, "payload"));
    assertNotNull(writer.emit(
      method("send", bytes, List.of(body)),
      mapping(elements, "/send", "application/octet-stream", "cc.asylum.RequestMethod.PUT"),
      "http://host"));

    final TypeMirror custom = declared(types, "cc.asylum.Item");
    when(types.erasure(custom)).thenAnswer(invocation -> custom);
    assertNotNull(writer.emit(method("load", custom, List.of()), mapping(elements, "/open{", "", null), "http://host"));

    final PrimitiveType primitive = mock(PrimitiveType.class);
    when(primitive.getKind()).thenAnswer(invocation -> TypeKind.INT);
    when(primitive.toString()).thenAnswer(invocation -> "int");
    final TypeMirror boxed = declared(types, "java.lang.Integer");
    final TypeElement boxedElement = mock(TypeElement.class);
    when(types.getPrimitiveType(TypeKind.INT)).thenAnswer(invocation -> primitive);
    when(types.boxedClass(primitive)).thenAnswer(invocation -> boxedElement);
    when(boxedElement.asType()).thenAnswer(invocation -> boxed);
    assertNotNull(writer.emit(method("count", primitive, List.of()), mapping(elements, "/count", "", "POST"), "http://host"));

    final AnnotationMirror unbound = MirrorSupport.annotation(null, "other.Ann", Map.of());
    assertNotNull(writer.emit(method("raw", type(TypeKind.VOID, "void"), List.of(parameter("x", unbound))), mapping(elements, "/raw", "", ""), "http://host"));
  }

  private static ExecutableElement method(final String simple, final TypeMirror returned, final List<VariableElement> parameters) {
    final ExecutableElement method = mock(ExecutableElement.class);
    when(method.getSimpleName()).thenAnswer(invocation -> MirrorSupport.name(simple));
    when(method.getReturnType()).thenAnswer(invocation -> returned);
    when(method.getParameters()).thenAnswer(invocation -> parameters);
    when(method.getThrownTypes()).thenAnswer(invocation -> List.of(type(TypeKind.DECLARED, "java.io.IOException")));
    return method;
  }

  private static VariableElement parameter(final String simple, final AnnotationMirror mirror) {
    final VariableElement parameter = mock(VariableElement.class);
    final Name name = MirrorSupport.name(simple);
    when(parameter.getSimpleName()).thenAnswer(invocation -> name);
    when(parameter.asType()).thenAnswer(invocation -> type(TypeKind.DECLARED, "java.lang.String"));
    when(parameter.getAnnotationMirrors()).thenAnswer(invocation -> mirror == null ? List.of() : List.of(mirror));
    return parameter;
  }

  private static AnnotationMirror requestBinding(final BindingSource source, final String value) {
    final AnnotationMirror mirror = MirrorSupport.annotation(RequestBinding.class, "cc.asylum.Path", Map.of("value", value));
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

  private static AnnotationMirror mapping(final Elements elements, final String path, final String contentType, final String method) {
    final AnnotationMirror mirror = MirrorSupport.annotation(null, RequestMapping.class.getCanonicalName(), Map.of("value", path, "contentType", contentType));
    final ExecutableElement member = mock(ExecutableElement.class);
    when(member.getSimpleName()).thenAnswer(invocation -> MirrorSupport.name("method"));
    final AnnotationValue value = mock(AnnotationValue.class);
    when(value.getValue()).thenAnswer(invocation -> method);
    when(elements.getElementValuesWithDefaults(mirror)).thenAnswer(invocation -> method == null ? Map.of() : Map.of(member, value));
    return mirror;
  }

  private static TypeMirror declared(final Types types, final String qualified) {
    final TypeMirror type = MirrorSupport.declared(qualified);
    when(types.asElement(type)).thenAnswer(invocation -> ((DeclaredType) type).asElement());
    return type;
  }

  private static TypeMirror type(final TypeKind kind, final String text) {
    return MirrorSupport.type(kind, text);
  }
}
