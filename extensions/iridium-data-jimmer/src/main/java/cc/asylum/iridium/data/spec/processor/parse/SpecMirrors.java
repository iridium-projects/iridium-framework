package cc.asylum.iridium.data.spec.processor.parse;

import cc.asylum.iridium.codegen.model.Mirrors;
import cc.asylum.iridium.core.util.Lists;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public final class SpecMirrors {

  private final Elements elements;

  public SpecMirrors(final Elements elements) {
    this.elements = elements;
  }

  public AnnotationMirror first(final Element element, final Class<? extends Annotation> type) {
    return Lists.first(Mirrors.all(element, type));
  }

  public List<AnnotationMirror> all(final Element element, final Class<? extends Annotation> type) {
    return Mirrors.all(element, type);
  }

  public List<AnnotationMirror> nested(final AnnotationMirror mirror, final String member) {
    final Object value = member(mirror, member);
    if (!(value instanceof final List<?> list)) {
      return List.of();
    }
    final List<AnnotationMirror> mirrors = new ArrayList<>();
    for (final Object item : list) {
      final Object unwrapped = item instanceof final AnnotationValue annotation ? annotation.getValue() : item;
      if (unwrapped instanceof final AnnotationMirror nested) {
        mirrors.add(nested);
      }
    }
    return mirrors;
  }

  public List<String> strings(final AnnotationMirror mirror, final String member) {
    final Object value = member(mirror, member);
    if (!(value instanceof final List<?> list)) {
      return List.of();
    }
    final List<String> values = new ArrayList<>();
    for (final Object item : list) {
      final Object unwrapped = item instanceof final AnnotationValue annotation ? annotation.getValue() : item;
      if (unwrapped != null) {
        values.add(unwrapped.toString());
      }
    }
    return values;
  }

  public String string(final AnnotationMirror mirror, final String member) {
    final Object value = member(mirror, member);
    return value == null ? "" : value.toString();
  }

  public boolean bool(final AnnotationMirror mirror, final String member) {
    final Object value = member(mirror, member);
    return value instanceof Boolean bool && bool;
  }

  public Object member(final AnnotationMirror mirror, final String name) {
    return Mirrors.member(elements, mirror, name);
  }
}
