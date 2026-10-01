package cc.asylum.iridium.core.processor.validation.check;

import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.core.processor.Mirrors;
import cc.asylum.iridium.core.validation.annotation.AssertFalse;
import cc.asylum.iridium.core.validation.annotation.AssertTrue;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class FlagChecksTest {

  @Test
  void emitsPrimitiveAndBoxedFlags() {
    final FlagChecks checks = new FlagChecks();
    assertTrue(checks.emit(field("none", TypeKind.BOOLEAN, null, null), Map.of()).isEmpty());
    assertEquals(2, checks.emit(field("flag", TypeKind.BOOLEAN, truth(), lie()), Map.of()).size());
    assertEquals(2, checks.emit(field("boxed", TypeKind.DECLARED, truth(), lie()), Map.of()).size());
  }

  private static ValidatedField field(final String name, final TypeKind kind, final AssertTrue truth, final AssertFalse lie) {
    final Element element = mock(Element.class);
    when(element.getAnnotation(AssertTrue.class)).thenAnswer(invocation -> truth);
    when(element.getAnnotation(AssertFalse.class)).thenAnswer(invocation -> lie);
    return new ValidatedField(name, Mirrors.primitive(kind), element, Exprs.name(name));
  }

  private static AssertTrue truth() {
    return new AssertTrue() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return AssertTrue.class;
      }
    };
  }

  private static AssertFalse lie() {
    return new AssertFalse() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return AssertFalse.class;
      }
    };
  }
}
