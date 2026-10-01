package cc.asylum.iridium.config.processor;

import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.config.Value;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class ValueBinderTest {

  @Test
  void bindsAnnotatedParametersAndSkipsTheRest() {
    final Processing processing = new Processing(
        mock(javax.lang.model.util.Types.class),
        mock(javax.lang.model.util.Elements.class),
        mock(Messager.class),
        mock(javax.annotation.processing.Filer.class),
        mock(javax.annotation.processing.RoundEnvironment.class));
    final ValueBinder binder = new ValueBinder();
    assertTrue(binder.bind(mock(VariableElement.class), processing).isEmpty());

    final VariableElement parameter = mock(VariableElement.class);
    final Value value = mock(Value.class);
    when(value.value()).thenAnswer(invocation -> "plain");
    when(parameter.getAnnotation(Value.class)).thenAnswer(invocation -> value);
    final Name name = mock(Name.class);
    when(name.toString()).thenAnswer(invocation -> "count");
    when(parameter.getSimpleName()).thenAnswer(invocation -> name);
    final TypeMirror type = mock(TypeMirror.class);
    when(type.getKind()).thenAnswer(invocation -> TypeKind.INT);
    when(parameter.asType()).thenAnswer(invocation -> type);
    assertTrue(binder.bind(parameter, processing).isEmpty());

    final VariableElement bound = mock(VariableElement.class);
    final Value placeholder = mock(Value.class);
    when(placeholder.value()).thenAnswer(invocation -> "${app.count}");
    when(bound.getAnnotation(Value.class)).thenAnswer(invocation -> placeholder);
    when(bound.getSimpleName()).thenAnswer(invocation -> name);
    final TypeMirror integer = mock(TypeMirror.class);
    when(integer.getKind()).thenAnswer(invocation -> TypeKind.INT);
    when(bound.asType()).thenAnswer(invocation -> integer);
    assertTrue(binder.bind(bound, processing).isPresent());
  }
}
