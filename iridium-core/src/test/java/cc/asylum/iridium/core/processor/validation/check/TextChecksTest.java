package cc.asylum.iridium.core.processor.validation.check;

import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.core.validation.annotation.Email;
import cc.asylum.iridium.core.validation.annotation.Pattern;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class TextChecksTest {

  @Test
  void matchesNamedPatternsAndEmail() {
    final TextChecks checks = new TextChecks();
    final Element empty = mock(Element.class);
    assertTrue(checks.emit(field("plain", empty), Map.of()).isEmpty());

    final Element marked = mock(Element.class);
    when(marked.getAnnotation(Pattern.class)).thenAnswer(invocation -> pattern("a+"));
    when(marked.getAnnotation(Email.class)).thenAnswer(invocation -> email());
    assertEquals(2, checks.emit(field("mail", marked), Map.of("PATTERN_0", "a+", "EMAIL_0", TextChecks.EMAIL_REGEX)).size());
  }

  @Test
  void missingPatternNameFails() {
    final TextChecks checks = new TextChecks();
    final Element marked = mock(Element.class);
    when(marked.getAnnotation(Pattern.class)).thenAnswer(invocation -> pattern("z+"));
    assertThrows(IllegalStateException.class, () -> checks.emit(field("code", marked), Map.of("PATTERN_0", "a+")));
  }

  private static ValidatedField field(final String name, final Element element) {
    return new ValidatedField(name, mock(TypeMirror.class), element, Exprs.name(name));
  }

  private static Pattern pattern(final String regexp) {
    return new Pattern() {
      @Override
      public String regexp() {
        return regexp;
      }

      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Pattern.class;
      }
    };
  }

  private static Email email() {
    return new Email() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Email.class;
      }
    };
  }
}
