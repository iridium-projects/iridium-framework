package cc.asylum.iridium.codegen.model;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Mirrors {

  private Mirrors() {
  }

  public static <A extends Annotation> A meta(final Element element, final Class<A> meta) {
    for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {
      final A found = mirror.getAnnotationType().asElement().getAnnotation(meta);
      if (found != null) {
        return found;
      }
    }

    return null;
  }

  public static AnnotationMirror withMeta(final Element element, final Class<? extends Annotation> meta) {
    for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {
      if (mirror.getAnnotationType().asElement().getAnnotation(meta) != null) {
        return mirror;
      }
    }
    return null;
  }

  public static boolean hasMeta(final Element element, final Class<? extends Annotation> meta) {
    return meta(element, meta) != null;
  }

  public static AnnotationMirror of(final Element element, final Class<? extends Annotation> type) {
    final String name = type.getCanonicalName();
    for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {
      final Element annotationElement = mirror.getAnnotationType().asElement();
      if (annotationElement instanceof final TypeElement typeElement && typeElement.getQualifiedName().contentEquals(name)) {
        return mirror;
      }
    }

    return null;
  }

  public static List<AnnotationMirror> all(final Element element, final Class<? extends Annotation> type) {
    final String name = type.getCanonicalName();
    final List<AnnotationMirror> found = new ArrayList<>();
    for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {
      if (qualified(mirror).equals(name)) {
        found.add(mirror);
      }
    }
    return found;
  }

  public static String qualified(final AnnotationMirror mirror) {
    return ((TypeElement) mirror.getAnnotationType().asElement()).getQualifiedName().toString();
  }

  public static Object member(final javax.lang.model.util.Elements elements, final AnnotationMirror mirror, final String name) {
    for (final Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elements.getElementValuesWithDefaults(mirror).entrySet()) {
      if (entry.getKey().getSimpleName().contentEquals(name)) {
        return entry.getValue().getValue();
      }
    }
    return null;
  }

  public static Object memberValue(final AnnotationMirror mirror, final String member) {
    if (mirror == null) {
      return null;
    }
    for (final Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
      if (entry.getKey().getSimpleName().contentEquals(member)) {
        return entry.getValue().getValue();
      }
    }
    return null;
  }

  public static String string(final AnnotationMirror mirror, final String member, final String fallback) {
    final Object value = memberValue(mirror, member);
    return value == null ? fallback : value.toString();
  }

  public static boolean bool(final AnnotationMirror mirror, final String member, final boolean fallback) {
    final Object value = memberValue(mirror, member);
    return value instanceof final Boolean bool ? bool : fallback;
  }

  public static String string(final Element element, final Class<? extends Annotation> type, final String member, final String fallback) {
    return string(of(element, type), member, fallback);
  }
}
