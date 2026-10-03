package cc.asylum.iridium.core.processor.validation.check;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.core.processor.Mirrors;
import cc.asylum.iridium.core.validation.annotation.NotBlank;
import cc.asylum.iridium.core.validation.annotation.NotEmpty;
import cc.asylum.iridium.core.validation.annotation.NotNull;
import cc.asylum.iridium.core.validation.annotation.Size;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class PresenceChecksTest {

  @Test
  void skipsPrimitiveNullAndEmitsPresence() {
    final Types types = mock(Types.class);
    final Elements elements = mock(Elements.class);
    final PresenceChecks checks = new PresenceChecks(types, elements);
    final Element primitive = element();
    when(primitive.getAnnotation(NotNull.class)).thenAnswer(invocation -> notNull());
    assertTrue(checks.emit(field("flag", Mirrors.primitive(TypeKind.BOOLEAN), primitive), Map.of()).isEmpty());

    final Element present = element();
    when(present.getAnnotation(NotNull.class)).thenAnswer(invocation -> notNull());
    when(present.getAnnotation(NotBlank.class)).thenAnswer(invocation -> notBlank());
    when(present.getAnnotation(NotEmpty.class)).thenAnswer(invocation -> notEmpty());
    final Size size = size(1, 8);
    when(present.getAnnotation(Size.class)).thenAnswer(invocation -> size);
    final TypeMirror string = Mirrors.declared("java.lang.String");
    same(types, elements, string, CharSequence.class.getCanonicalName(), true);
    assertEquals(4, checks.emit(field("name", string, present), Map.of()).size());
  }

  @Test
  void sizesArraysAndCollections() {
    final Types types = mock(Types.class);
    final Elements elements = mock(Elements.class);
    final PresenceChecks checks = new PresenceChecks(types, elements);
    final Element array = element();
    when(array.getAnnotation(NotEmpty.class)).thenAnswer(invocation -> notEmpty());
    when(array.getAnnotation(Size.class)).thenAnswer(invocation -> size(0, 2));
    assertEquals(2, checks.emit(field("items", Mirrors.array(Mirrors.primitive(TypeKind.INT)), array), Map.of()).size());

    final Element collection = element();
    when(collection.getAnnotation(Size.class)).thenAnswer(invocation -> size(0, 1));
    final TypeMirror list = Mirrors.declared("java.util.List");
    same(types, elements, list, CharSequence.class.getCanonicalName(), false);
    assertEquals(1, checks.emit(field("values", list, collection), Map.of()).size());
  }

  private static void same(final Types types, final Elements elements, final TypeMirror type, final String fqcn, final boolean value) {
    final javax.lang.model.element.TypeElement target = mock(javax.lang.model.element.TypeElement.class);
    final TypeMirror targetType = mock(TypeMirror.class);
    when(target.asType()).thenAnswer(invocation -> targetType);
    when(elements.getTypeElement(fqcn)).thenAnswer(invocation -> target);
    when(types.isAssignable(type, targetType)).thenAnswer(invocation -> value);
  }

  private static ValidatedField field(final String name, final TypeMirror type, final Element element) {
    return new ValidatedField(name, type, element, Exprs.name(name));
  }

  private static Element element() {
    final Element element = mock(Element.class);
    when(element.getAnnotationMirrors()).thenAnswer(invocation -> java.util.List.of());
    return element;
  }

  private static NotNull notNull() {
    return new NotNull() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return NotNull.class;
      }
    };
  }

  private static NotBlank notBlank() {
    return new NotBlank() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return NotBlank.class;
      }
    };
  }

  private static NotEmpty notEmpty() {
    return new NotEmpty() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return NotEmpty.class;
      }
    };
  }

  private static Size size(final int min, final int max) {
    return new Size() {
      @Override
      public int min() {
        return min;
      }

      @Override
      public int max() {
        return max;
      }

      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Size.class;
      }
    };
  }
}
