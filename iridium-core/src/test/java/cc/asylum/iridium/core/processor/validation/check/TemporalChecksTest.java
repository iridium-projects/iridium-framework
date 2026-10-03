package cc.asylum.iridium.core.processor.validation.check;

import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.core.processor.Mirrors;
import cc.asylum.iridium.core.validation.annotation.Future;
import cc.asylum.iridium.core.validation.annotation.Past;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class TemporalChecksTest {

  @Test
  void datesAndTemporalTypes() {
    final Types types = mock(Types.class);
    final Elements elements = mock(Elements.class);
    final Messager messager = mock(Messager.class);
    final TemporalChecks checks = new TemporalChecks(types, elements, messager);
    final Element empty = mock(Element.class);
    assertTrue(checks.emit(field("instant", Mirrors.declared("java.time.Instant"), empty), Map.of()).isEmpty());

    final TypeMirror date = Mirrors.declared(Date.class.getCanonicalName());
    same(types, elements, date, Date.class);
    final Element dated = mock(Element.class);
    when(dated.getAnnotation(Past.class)).thenAnswer(invocation -> past());
    when(dated.getAnnotation(Future.class)).thenAnswer(invocation -> future());
    assertEquals(2, checks.emit(field("created", date, dated), Map.of()).size());

    final TypeMirror instant = Mirrors.declared("java.time.Instant");
    final TypeElement instantElement = (TypeElement) ((javax.lang.model.type.DeclaredType) instant).asElement();
    when(instantElement.asType()).thenAnswer(invocation -> instant);
    when(types.asElement(instant)).thenAnswer(invocation -> instantElement);
    final Element timed = mock(Element.class);
    when(timed.getAnnotation(Past.class)).thenAnswer(invocation -> past());
    when(timed.getAnnotation(Future.class)).thenAnswer(invocation -> future());
    assertEquals(2, checks.emit(field("at", instant, timed), Map.of()).size());
  }

  @Test
  void unknownTypeReportsAndReturnsNull() {
    final Types types = mock(Types.class);
    final Messager messager = mock(Messager.class);
    final TemporalChecks checks = new TemporalChecks(types, mock(Elements.class), messager);
    final TypeMirror type = Mirrors.primitive(javax.lang.model.type.TypeKind.INT);
    when(types.asElement(type)).thenAnswer(invocation -> null);
    final Element element = mock(Element.class);
    when(element.getAnnotation(Past.class)).thenAnswer(invocation -> past());
    assertNull(checks.emit(field("n", type, element), Map.of()).get(0));
    verify(messager).printMessage(
      org.mockito.ArgumentMatchers.eq(Diagnostic.Kind.ERROR),
      org.mockito.ArgumentMatchers.contains("java.util.Date"),
      org.mockito.ArgumentMatchers.eq(element));
  }

  private static void same(final Types types, final Elements elements, final TypeMirror type, final Class<?> target) {
    final TypeElement element = mock(TypeElement.class);
    final TypeMirror mirror = mock(TypeMirror.class);
    when(element.asType()).thenAnswer(invocation -> mirror);
    when(elements.getTypeElement(target.getCanonicalName())).thenAnswer(invocation -> element);
    when(types.isSameType(type, mirror)).thenAnswer(invocation -> true);
  }

  private static ValidatedField field(final String name, final TypeMirror type, final Element element) {
    return new ValidatedField(name, type, element, Exprs.name(name));
  }

  private static Past past() {
    return new Past() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Past.class;
      }
    };
  }

  private static Future future() {
    return new Future() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Future.class;
      }
    };
  }
}
