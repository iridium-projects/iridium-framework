package cc.asylum.iridium.codegen.support;

import cc.asylum.iridium.core.annotation.Internal;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.Map;

@Internal
public final class MirrorSupport {

  private MirrorSupport() {
  }

  public static <A extends Annotation> A metaAnnotation(
      final Element element,
      final Class<A> meta
  ) {
    for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {
      final A found = mirror.getAnnotationType().asElement().getAnnotation(meta);

      if (found != null) {
        return found;
      }
    }

    return null;
  }

  public static AnnotationMirror annotationWithMeta(
      final Element element,
      final Class<? extends Annotation> meta
  ) {
    for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {
      if (mirror.getAnnotationType().asElement().getAnnotation(meta) != null) {
        return mirror;
      }
    }
    return null;
  }

  public static boolean hasMeta(
      final Element element,
      final Class<? extends Annotation> meta
  ) {
    return metaAnnotation(element, meta) != null;
  }

  public static AnnotationMirror mirrorOf(
      final Element element,
      final Class<? extends Annotation> type
  ) {
    final String name = type.getCanonicalName();

    for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {
      final Element annotationElement = mirror.getAnnotationType().asElement();

      if (annotationElement instanceof final TypeElement typeElement
          && typeElement.getQualifiedName().contentEquals(name)) {
        return mirror;
      }
    }
    return null;
  }

  public static Object memberValue(
      final AnnotationMirror mirror,
      final String member
  ) {
    if (mirror == null) {
      return null;
    }

    final Map<? extends ExecutableElement, ? extends AnnotationValue> members = mirror.getElementValues();
    for (final Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : members.entrySet()) {
      if (entry.getKey().getSimpleName().contentEquals(member)) {
        return entry.getValue().getValue();
      }
    }

    return null;
  }

  public static String stringMember(
      final AnnotationMirror mirror,
      final String member,
      final String fallback
  ) {
    final Object value = memberValue(mirror, member);
    return value == null ? fallback : value.toString();
  }

  public static boolean booleanMember(
      final AnnotationMirror mirror,
      final String member,
      final boolean fallback
  ) {
    final Object value = memberValue(mirror, member);
    return value instanceof final Boolean bool ? bool : fallback;
  }

  public static int intMember(
      final AnnotationMirror mirror,
      final String member,
      final int fallback
  ) {
    final Object value = memberValue(mirror, member);
    return value instanceof final Integer integer ? integer : fallback;
  }

  public static long longMember(
      final AnnotationMirror mirror,
      final String member,
      final long fallback
  ) {
    final Object value = memberValue(mirror, member);
    return value instanceof final Number number ? number.longValue() : fallback;
  }

  public static String stringValue(
      final Element element,
      final Class<? extends Annotation> type,
      final String member,
      final String fallback
  ) {
    return stringMember(mirrorOf(element, type), member, fallback);
  }

  public static boolean booleanValue(
      final Element element,
      final Class<? extends Annotation> type,
      final String member,
      final boolean fallback
  ) {
    return booleanMember(mirrorOf(element, type), member, fallback);
  }
}
