package cc.asylum.iridium.web.processor.binding;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.write.Body;
import cc.asylum.iridium.web.controller.parameter.RequestBinding;
import cc.asylum.iridium.web.processor.MirrorSupport;
import cc.asylum.iridium.web.processor.binding.convert.RequestConversion;
import cc.asylum.iridium.web.router.Request;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class ParameterBindingTest {

  @Test
  void emitsRequestBinderAndRequestSources() {
    final Types types = mock(Types.class);
    final Elements elements = mock(Elements.class);
    final RequestConversion conversion = new RequestConversion(types);
    final ParameterBinder binder = new ParameterBinder() {
      @Override
      public boolean matches(final VariableElement parameter) {
        return "bound".equals(parameter.getSimpleName().toString());
      }

      @Override
      public Optional<Expr> emit(final Body body, final VariableElement parameter) {
        return Optional.empty();
      }
    };
    final ParameterBinding binding = new ParameterBinding(types, elements, conversion, List.of(binder));

    final TypeElement requestType = mock(TypeElement.class);
    when(elements.getTypeElement(Request.class.getCanonicalName())).thenAnswer(invocation -> requestType);
    final TypeMirror request = MirrorSupport.type(TypeKind.DECLARED, Request.class.getCanonicalName());
    when(requestType.asType()).thenAnswer(invocation -> request);
    when(types.isSameType(request, request)).thenAnswer(invocation -> true);
    assertTrue(binding.emit(new Body(), parameter("request", request, List.of())).isPresent());

    assertFalse(binding.emit(new Body(), parameter("bound", MirrorSupport.type(TypeKind.DECLARED, "java.lang.String"), List.of())).isPresent());

    final TypeMirror text = declared(types, "java.lang.String");
    assertTrue(binding.emit(new Body(), parameter("plain", text, List.of())).isPresent());

    final AnnotationMirror path = requestBinding(Map.of("value", "id", "required", Boolean.TRUE, "defaultValue", ""));
    assertTrue(binding.emit(new Body(), parameter("id", text, List.of(path))).isPresent());

    final AnnotationMirror fallback = requestBinding(Map.of("value", "q", "required", Boolean.TRUE, "defaultValue", "none"));
    assertTrue(binding.emit(new Body(), parameter("q", text, List.of(fallback))).isPresent());

    final AnnotationMirror optional = requestBinding(Map.of("value", "page", "required", Boolean.FALSE, "defaultValue", ""));
    assertTrue(binding.emit(new Body(), parameter("page", text, List.of(optional))).isPresent());

    final TypeMirror wrapped = optionalOf(types, text);
    when(types.erasure(wrapped)).thenAnswer(invocation -> wrapped);
    assertTrue(binding.emit(new Body(), parameter("maybe", wrapped, List.of())).isPresent());

    final TypeMirror custom = declared(types, "cc.asylum.Item");
    when(types.erasure(custom)).thenAnswer(invocation -> custom);
    final AnnotationMirror body = requestBinding(Map.of("required", Boolean.FALSE));
    assertTrue(binding.emit(new Body(), parameter("item", custom, List.of(body))).isPresent());
    assertTrue(binding.emit(new Body(), parameter("payload", text, List.of())).isPresent());
  }

  private static VariableElement parameter(final String simple, final TypeMirror type, final List<AnnotationMirror> mirrors) {
    final VariableElement parameter = mock(VariableElement.class);
    final Name name = MirrorSupport.name(simple);
    when(parameter.getSimpleName()).thenAnswer(invocation -> name);
    when(parameter.asType()).thenAnswer(invocation -> type);
    when(parameter.getAnnotationMirrors()).thenAnswer(invocation -> mirrors);
    return parameter;
  }

  private static AnnotationMirror requestBinding(final Map<String, Object> members) {
    return MirrorSupport.annotation(RequestBinding.class, "cc.asylum.iridium.web.controller.parameter.PathVariable", members);
  }

  private static TypeMirror declared(final Types types, final String qualified) {
    final DeclaredType type = MirrorSupport.declared(qualified);
    when(types.asElement(type)).thenAnswer(invocation -> type.asElement());
    return type;
  }

  private static TypeMirror optionalOf(final Types types, final TypeMirror inner) {
    final DeclaredType type = mock(DeclaredType.class);
    final TypeElement element = mock(TypeElement.class);
    when(type.getKind()).thenAnswer(invocation -> TypeKind.DECLARED);
    when(type.toString()).thenAnswer(invocation -> "java.util.Optional");
    when(type.asElement()).thenAnswer(invocation -> element);
    when(element.getQualifiedName()).thenAnswer(invocation -> MirrorSupport.name("java.util.Optional"));
    when(type.getTypeArguments()).thenAnswer(invocation -> List.of(inner));
    when(types.asElement(type)).thenAnswer(invocation -> element);
    return type;
  }
}
