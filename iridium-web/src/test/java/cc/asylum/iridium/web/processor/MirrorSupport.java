package cc.asylum.iridium.web.processor;

import cc.asylum.iridium.web.controller.parameter.BindingSource;
import cc.asylum.iridium.web.controller.parameter.RequestBinding;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class MirrorSupport {

  private MirrorSupport() {
  }

  public static Name name(final String text) {
    final Name name = mock(Name.class);
    when(name.toString()).thenAnswer(invocation -> text);
    when(name.contentEquals(text)).thenAnswer(invocation -> true);
    return name;
  }

  public static TypeMirror type(final TypeKind kind, final String text) {
    final TypeMirror type = mock(TypeMirror.class);
    when(type.getKind()).thenAnswer(invocation -> kind);
    when(type.toString()).thenAnswer(invocation -> text);
    return type;
  }

  public static DeclaredType declared(final String qualified) {
    final DeclaredType type = mock(DeclaredType.class);
    final TypeElement element = mock(TypeElement.class);
    when(type.getKind()).thenAnswer(invocation -> TypeKind.DECLARED);
    when(type.toString()).thenAnswer(invocation -> qualified);
    when(type.asElement()).thenAnswer(invocation -> element);
    when(element.getQualifiedName()).thenAnswer(invocation -> name(qualified));
    when(element.asType()).thenAnswer(invocation -> type);
    when(type.getTypeArguments()).thenAnswer(invocation -> List.of());
    return type;
  }

  public static AnnotationMirror annotation(final Class<? extends Annotation> meta, final String qualified, final Map<String, Object> members) {
    final AnnotationMirror mirror = mock(AnnotationMirror.class);
    final DeclaredType type = mock(DeclaredType.class);
    final TypeElement element = mock(TypeElement.class);
    when(element.getQualifiedName()).thenAnswer(invocation -> name(qualified));
    when(element.getAnnotation(RequestBinding.class)).thenAnswer(invocation -> binding(meta));
    when(type.asElement()).thenAnswer(invocation -> element);
    when(mirror.getAnnotationType()).thenAnswer(invocation -> type);
    when(mirror.getElementValues()).thenAnswer(invocation -> values(members));
    return mirror;
  }

  static void mirrors(final Element element, final AnnotationMirror... mirrors) {
    when(element.getAnnotationMirrors()).thenAnswer(invocation -> List.of(mirrors));
  }

  private static Map<ExecutableElement, AnnotationValue> values(final Map<String, Object> members) {
    final Map<ExecutableElement, AnnotationValue> values = new LinkedHashMap<>();
    for (final Map.Entry<String, Object> entry : members.entrySet()) {
      final ExecutableElement key = mock(ExecutableElement.class);
      when(key.getSimpleName()).thenAnswer(invocation -> name(entry.getKey()));
      final AnnotationValue value = mock(AnnotationValue.class);
      when(value.getValue()).thenAnswer(invocation -> entry.getValue());
      values.put(key, value);
    }
    return values;
  }

  private static RequestBinding binding(final Class<? extends Annotation> meta) {
    if (meta != RequestBinding.class) {
      return null;
    }
    return new RequestBinding() {
      @Override
      public BindingSource value() {
        return BindingSource.PATH;
      }

      @Override
      public Class<? extends Annotation> annotationType() {
        return RequestBinding.class;
      }
    };
  }
}
