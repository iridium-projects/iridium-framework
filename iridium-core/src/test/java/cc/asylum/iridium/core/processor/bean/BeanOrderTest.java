package cc.asylum.iridium.core.processor.bean;

import cc.asylum.iridium.core.processor.Mirrors;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class BeanOrderTest {

  @Test
  void sortsDependenciesAndBreaksCycles() {
    final TypeMirror aType = Mirrors.declared("app.A");
    final TypeMirror bType = Mirrors.declared("app.B");
    final TypeMirror cType = Mirrors.declared("app.C");
    final TypeElement a = type(aType, parameter(bType));
    final TypeElement b = type(bType);
    final TypeElement c = type(cType, parameter(aType), parameter(Mirrors.declared("java.lang.String")));
    final TypeElement cycleLeft = type(Mirrors.declared("app.L"), parameter(Mirrors.declared("app.R")));
    final TypeElement cycleRight = type(Mirrors.declared("app.R"), parameter(Mirrors.declared("app.L")));
    final TypeElement bare = mock(TypeElement.class);
    when(bare.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of());
    when(bare.asType()).thenAnswer(invocation -> Mirrors.declared("app.Bare"));

    final List<TypeElement> ordered = BeanOrder.sort(Set.of(a, b, c, bare));
    assertEquals(4, ordered.size());
    assertTrue(ordered.indexOf(b) < ordered.indexOf(a));
    assertEquals(2, BeanOrder.sort(Set.of(cycleLeft, cycleRight)).size());
  }

  @Test
  void ignoresMultipleConstructors() {
    final TypeElement type = mock(TypeElement.class);
    final ExecutableElement first = mock(ExecutableElement.class);
    final ExecutableElement second = mock(ExecutableElement.class);
    when(first.getKind()).thenAnswer(invocation -> ElementKind.CONSTRUCTOR);
    when(second.getKind()).thenAnswer(invocation -> ElementKind.CONSTRUCTOR);
    when(type.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(first, second));
    when(type.asType()).thenAnswer(invocation -> Mirrors.declared("app.Many"));
    assertEquals(List.of(type), BeanOrder.sort(Set.of(type)));
  }

  private static TypeElement type(final TypeMirror type, final VariableElement... parameters) {
    final TypeElement element = mock(TypeElement.class);
    final ExecutableElement constructor = mock(ExecutableElement.class);
    when(constructor.getKind()).thenAnswer(invocation -> ElementKind.CONSTRUCTOR);
    when(constructor.getParameters()).thenAnswer(invocation -> List.of(parameters));
    when(element.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor));
    when(element.asType()).thenAnswer(invocation -> type);
    return element;
  }

  private static VariableElement parameter(final TypeMirror type) {
    final VariableElement parameter = mock(VariableElement.class);
    when(parameter.asType()).thenAnswer(invocation -> type);
    return parameter;
  }
}
