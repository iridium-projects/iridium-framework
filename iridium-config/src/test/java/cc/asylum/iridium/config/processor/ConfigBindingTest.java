package cc.asylum.iridium.config.processor;

import cc.asylum.iridium.config.Default;
import cc.asylum.iridium.config.Value;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class ConfigBindingTest {

  @Test
  void validatesPrefixes() {
    assertTrue(ConfigBinding.validPrefix(""));
    assertTrue(ConfigBinding.validPrefix("app.name"));
    assertTrue(ConfigBinding.validPrefix("app-name_1"));
    assertFalse(ConfigBinding.validPrefix(".app"));
    assertFalse(ConfigBinding.validPrefix("app."));
    assertFalse(ConfigBinding.validPrefix("app..name"));
    assertFalse(ConfigBinding.validPrefix("app name"));
    assertFalse(ConfigBinding.validPrefix("app/name"));
    assertTrue(ConfigBinding.PLACEHOLDER.matcher("${key}").matches());
    assertTrue(ConfigBinding.PLACEHOLDER.matcher("${key:default}").matches());
    assertFalse(ConfigBinding.PLACEHOLDER.matcher("key").matches());
    assertFalse(ConfigBinding.PLACEHOLDER.matcher("${}").matches());
  }

  @Test
  void readsDefaultsAndAnnotations() {
    final VariableElement parameter = mock(VariableElement.class);
    final RecordComponentElement component = mock(RecordComponentElement.class);
    final Default onParameter = mock(Default.class);
    when(onParameter.value()).thenAnswer(invocation -> "from-param");
    when(parameter.getAnnotation(Default.class)).thenAnswer(invocation -> onParameter);
    assertEquals("from-param", ConfigBinding.defaultOf(parameter, component));
    assertEquals(onParameter, ConfigBinding.annotation(parameter, component, Default.class));

    final VariableElement bare = mock(VariableElement.class);
    final Default onComponent = mock(Default.class);
    when(onComponent.value()).thenAnswer(invocation -> "from-component");
    when(component.getAnnotation(Default.class)).thenAnswer(invocation -> onComponent);
    assertEquals("from-component", ConfigBinding.defaultOf(bare, component));
    assertNull(ConfigBinding.defaultOf(bare, null));
    assertNull(ConfigBinding.annotation(bare, null, Value.class));
    assertEquals(onComponent, ConfigBinding.annotation(bare, component, Default.class));
  }

  @Test
  void findsConstructors() {
    final TypeElement type = mock(TypeElement.class);
    when(type.getKind()).thenAnswer(invocation -> ElementKind.CLASS);
    final ExecutableElement only = constructor(0);
    when(type.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(only, mock(Element.class)));
    assertEquals(only, ConfigBinding.constructorOf(type));

    final ExecutableElement extra = constructor(1);
    when(type.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(only, extra));
    assertNull(ConfigBinding.constructorOf(type));

    final TypeElement record = mock(TypeElement.class);
    when(record.getKind()).thenAnswer(invocation -> ElementKind.RECORD);
    final RecordComponentElement component = mock(RecordComponentElement.class);
    when(record.getRecordComponents()).thenAnswer(invocation -> List.of(component));
    final ExecutableElement canonical = constructor(1);
    final ExecutableElement compact = constructor(0);
    final Element method = mock(Element.class);
    when(method.getKind()).thenAnswer(invocation -> ElementKind.METHOD);
    when(record.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(method, compact, canonical));
    assertEquals(canonical, ConfigBinding.constructorOf(record));

    when(record.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(compact));
    assertNull(ConfigBinding.constructorOf(record));
  }

  private static ExecutableElement constructor(final int parameters) {
    final ExecutableElement constructor = mock(ExecutableElement.class);
    when(constructor.getKind()).thenAnswer(invocation -> ElementKind.CONSTRUCTOR);
    when(constructor.getModifiers()).thenAnswer(invocation -> Set.of(Modifier.PUBLIC));
    final List<VariableElement> values = new java.util.ArrayList<>();
    for (int i = 0; i < parameters; i++) {
      final int index = i;
      final VariableElement parameter = mock(VariableElement.class);
      final Name name = mock(Name.class);
      when(name.toString()).thenAnswer(invocation -> "arg" + index);
      when(parameter.getSimpleName()).thenAnswer(invocation -> name);
      when(parameter.asType()).thenAnswer(invocation -> mock(TypeMirror.class));
      values.add(parameter);
    }
    when(constructor.getParameters()).thenAnswer(invocation -> values);
    return constructor;
  }
}
