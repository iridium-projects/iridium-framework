package cc.asylum.iridium.codegen.model;

import org.junit.jupiter.api.Test;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class MirrorsTest {

  @Test
  void metaLookup() {
    final Element element = mock(Element.class);
    final AnnotationMirror hit = annotation(Meta.class, "other.Ann");
    final AnnotationMirror miss = annotation(null, "other.Miss");
    when(element.getAnnotationMirrors()).thenAnswer(invocation -> java.util.List.<AnnotationMirror>of(miss, hit));
    assertEquals(Meta.class, Mirrors.meta(element, Meta.class).annotationType());
    assertSame(hit, Mirrors.withMeta(element, Meta.class));
    assertTrue(Mirrors.hasMeta(element, Meta.class));
    when(element.getAnnotationMirrors()).thenAnswer(invocation -> java.util.List.<AnnotationMirror>of());
    assertNull(Mirrors.meta(element, Meta.class));
    assertNull(Mirrors.withMeta(element, Meta.class));
    assertFalse(Mirrors.hasMeta(element, Meta.class));
  }

  @Test
  void ofAllAndQualified() {
    final Element element = mock(Element.class);
    final AnnotationMirror match = annotation(null, Meta.class.getCanonicalName());
    final AnnotationMirror other = annotation(null, "no.Match");
    when(element.getAnnotationMirrors()).thenAnswer(invocation -> java.util.List.<AnnotationMirror>of(other, match));
    assertSame(match, Mirrors.of(element, Meta.class));
    assertEquals(List.of(match), Mirrors.all(element, Meta.class));
    assertEquals(Meta.class.getCanonicalName(), Mirrors.qualified(match));
    when(element.getAnnotationMirrors()).thenAnswer(invocation -> java.util.List.<AnnotationMirror>of(other));
    assertNull(Mirrors.of(element, Meta.class));
    assertTrue(Mirrors.all(element, Meta.class).isEmpty());
  }

  @Test
  void members() {
    final AnnotationMirror mirror = mock(AnnotationMirror.class);
    final ExecutableElement key = executable("value");
    final AnnotationValue annotationValue = mock(AnnotationValue.class);
    when(annotationValue.getValue()).thenAnswer(invocation -> "text");
    when(mirror.getElementValues()).thenAnswer(invocation -> java.util.Map.of(key, annotationValue));
    assertEquals("text", Mirrors.memberValue(mirror, "value"));
    assertNull(Mirrors.memberValue(mirror, "missing"));
    assertNull(Mirrors.memberValue(null, "value"));
    assertEquals("text", Mirrors.string(mirror, "value", "fallback"));
    assertEquals("fallback", Mirrors.string(mirror, "missing", "fallback"));
    assertEquals("fallback", Mirrors.bool(mirror, "flag", true) ? "fallback" : "no");
    when(annotationValue.getValue()).thenAnswer(invocation -> Boolean.FALSE);
    assertFalse(Mirrors.bool(mirror, "value", true));
    when(annotationValue.getValue()).thenAnswer(invocation -> "nope");
    assertTrue(Mirrors.bool(mirror, "value", true));

    final javax.lang.model.util.Elements elements = mock(javax.lang.model.util.Elements.class);
    final ExecutableElement member = executable("author");
    final AnnotationValue memberValue = mock(AnnotationValue.class);
    when(memberValue.getValue()).thenAnswer(invocation -> "Iridium");
    when(elements.getElementValuesWithDefaults(mirror)).thenAnswer(invocation -> java.util.Map.of(member, memberValue));
    assertEquals("Iridium", Mirrors.member(elements, mirror, "author"));
    assertNull(Mirrors.member(elements, mirror, "date"));
  }

  @Test
  void stringFromElement() {
    final Element element = mock(Element.class);
    final AnnotationMirror mirror = annotation(null, Meta.class.getCanonicalName());
    final ExecutableElement key = executable("value");
    final AnnotationValue annotationValue = mock(AnnotationValue.class);
    when(annotationValue.getValue()).thenAnswer(invocation -> "kept");
    when(mirror.getElementValues()).thenAnswer(invocation -> java.util.Map.of(key, annotationValue));
    when(element.getAnnotationMirrors()).thenAnswer(invocation -> java.util.List.<AnnotationMirror>of(mirror));
    assertEquals("kept", Mirrors.string(element, Meta.class, "value", "fallback"));
    assertEquals("fallback", Mirrors.string(element, Retention.class, "value", "fallback"));
  }

  private static AnnotationMirror annotation(final Class<? extends java.lang.annotation.Annotation> meta, final String qualified) {
    final AnnotationMirror mirror = mock(AnnotationMirror.class);
    final DeclaredType type = mock(DeclaredType.class);
    final TypeElement element = mock(TypeElement.class);
    final Name name = mock(Name.class);
    when(name.toString()).thenAnswer(invocation -> qualified);
    when(name.contentEquals(qualified)).thenAnswer(invocation -> true);
    when(element.getQualifiedName()).thenAnswer(invocation -> name);
    when(element.getAnnotation(Meta.class)).thenAnswer(invocation -> meta == Meta.class ? sample() : null);
    when(type.asElement()).thenAnswer(invocation -> element);
    when(mirror.getAnnotationType()).thenAnswer(invocation -> type);
    return mirror;
  }

  private static Meta sample() {
    return new Meta() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Meta.class;
      }
    };
  }

  private static ExecutableElement executable(final String simple) {
    final ExecutableElement element = mock(ExecutableElement.class);
    final Name name = mock(Name.class);
    when(name.contentEquals(simple)).thenAnswer(invocation -> true);
    when(element.getSimpleName()).thenAnswer(invocation -> name);
    return element;
  }

  @Retention(RetentionPolicy.RUNTIME)
  private @interface Meta {
  }
}
