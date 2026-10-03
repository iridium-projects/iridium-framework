package cc.asylum.iridium.core.processor.validation.check;

import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.core.processor.Mirrors;
import cc.asylum.iridium.core.validation.annotation.Digits;
import cc.asylum.iridium.core.validation.annotation.Max;
import cc.asylum.iridium.core.validation.annotation.Min;
import cc.asylum.iridium.core.validation.annotation.Negative;
import cc.asylum.iridium.core.validation.annotation.NegativeOrZero;
import cc.asylum.iridium.core.validation.annotation.Positive;
import cc.asylum.iridium.core.validation.annotation.PositiveOrZero;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class NumericChecksTest {

  @Test
  void primitiveBoundsSignsAndDigits() {
    final NumericChecks checks = new NumericChecks(mock(Types.class), mock(Elements.class));
    final Element bare = mock(Element.class);
    assertTrue(checks.emit(field("n", Mirrors.primitive(TypeKind.INT), bare), Map.of()).isEmpty());

    final Element marked = mock(Element.class);
    when(marked.getAnnotation(Min.class)).thenAnswer(invocation -> min(1));
    when(marked.getAnnotation(Max.class)).thenAnswer(invocation -> max(9));
    when(marked.getAnnotation(Positive.class)).thenAnswer(invocation -> positive());
    when(marked.getAnnotation(Negative.class)).thenAnswer(invocation -> negative());
    when(marked.getAnnotation(PositiveOrZero.class)).thenAnswer(invocation -> positiveOrZero());
    when(marked.getAnnotation(NegativeOrZero.class)).thenAnswer(invocation -> negativeOrZero());
    when(marked.getAnnotation(Digits.class)).thenAnswer(invocation -> digits(3, 2));
    assertEquals(7, checks.emit(field("n", Mirrors.primitive(TypeKind.LONG), marked), Map.of()).size());
    assertEquals(7, checks.emit(field("boxed", Mirrors.declared("java.lang.Integer"), marked), Map.of()).size());
  }

  @Test
  void bigDecimalAndBigIntegerCompare() {
    final Types types = mock(Types.class);
    final Elements elements = mock(Elements.class);
    final TypeMirror decimal = Mirrors.declared(BigDecimal.class.getCanonicalName());
    final TypeMirror integer = Mirrors.declared(BigInteger.class.getCanonicalName());
    same(types, elements, decimal, BigDecimal.class);
    same(types, elements, integer, BigInteger.class);
    final NumericChecks checks = new NumericChecks(types, elements);
    final Element marked = mock(Element.class);
    when(marked.getAnnotation(Min.class)).thenAnswer(invocation -> min(0));
    when(marked.getAnnotation(Max.class)).thenAnswer(invocation -> max(10));
    when(marked.getAnnotation(Positive.class)).thenAnswer(invocation -> positive());
    when(marked.getAnnotation(Digits.class)).thenAnswer(invocation -> digits(4, 0));
    assertEquals(4, checks.emit(field("money", decimal, marked), Map.of()).size());
    assertEquals(4, checks.emit(field("count", integer, marked), Map.of()).size());
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

  private static Min min(final long value) {
    return new Min() {
      @Override
      public long value() {
        return value;
      }

      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Min.class;
      }
    };
  }

  private static Max max(final long value) {
    return new Max() {
      @Override
      public long value() {
        return value;
      }

      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Max.class;
      }
    };
  }

  private static Digits digits(final int integer, final int fraction) {
    return new Digits() {
      @Override
      public int integer() {
        return integer;
      }

      @Override
      public int fraction() {
        return fraction;
      }

      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Digits.class;
      }
    };
  }

  private static Positive positive() {
    return new Positive() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Positive.class;
      }
    };
  }

  private static Negative negative() {
    return new Negative() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Negative.class;
      }
    };
  }

  private static PositiveOrZero positiveOrZero() {
    return new PositiveOrZero() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return PositiveOrZero.class;
      }
    };
  }

  private static NegativeOrZero negativeOrZero() {
    return new NegativeOrZero() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return NegativeOrZero.class;
      }
    };
  }
}
