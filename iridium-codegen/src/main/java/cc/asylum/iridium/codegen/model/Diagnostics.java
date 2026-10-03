package cc.asylum.iridium.codegen.model;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public final class Diagnostics {

  private Diagnostics() {
  }

  public static void error(final Messager messager, final Element element, final String message) {
    messager.printMessage(Diagnostic.Kind.ERROR, message, element);
  }

  public static void error(
    final Messager messager,
    final Element element,
    final AnnotationMirror mirror,
    final AnnotationValue value,
    final String message
  ) {
    messager.printMessage(Diagnostic.Kind.ERROR, message, element, mirror, value);
  }

  public static void warning(final Messager messager, final Element element, final String message) {
    messager.printMessage(Diagnostic.Kind.WARNING, message, element);
  }
}
