package cc.asylum.iridium.codegen.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
final class DiagnosticsTest {

  @Mock
  private Messager messager;

  @Mock
  private Element element;

  @Mock
  private AnnotationMirror mirror;

  @Mock
  private AnnotationValue value;

  @Test
  void errorAndWarning() {
    Diagnostics.error(messager, element, "bad");
    Diagnostics.error(messager, element, mirror, value, "worse");
    Diagnostics.warning(messager, element, "careful");
    verify(messager).printMessage(Diagnostic.Kind.ERROR, "bad", element);
    verify(messager).printMessage(Diagnostic.Kind.ERROR, "worse", element, mirror, value);
    verify(messager).printMessage(Diagnostic.Kind.WARNING, "careful", element);
  }
}
