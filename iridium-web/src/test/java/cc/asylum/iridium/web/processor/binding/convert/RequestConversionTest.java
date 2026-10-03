package cc.asylum.iridium.web.processor.binding.convert;

import cc.asylum.forgery.expr.Expr;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class RequestConversionTest {

  @Test
  void convertsBodyPrimitivesAndBoxed() {
    final Types types = mock(Types.class);
    final RequestConversion conversion = new RequestConversion(types);
    final Expr raw = Expr.lit("raw");

    final TypeMirror bytes = type(TypeKind.ARRAY, "byte[]");
    assertSame(raw, conversion.convert(bytes, raw, true));

    final TypeMirror text = declared(types, "java.lang.String");
    assertNotNull(conversion.convert(text, raw, true));
    assertNotNull(conversion.convert(text, raw, false));

    assertNotNull(conversion.convert(type(TypeKind.BOOLEAN, "boolean"), raw, false));
    assertNotNull(conversion.convert(type(TypeKind.BYTE, "byte"), raw, false));
    assertNotNull(conversion.convert(type(TypeKind.SHORT, "short"), raw, false));
    assertNotNull(conversion.convert(type(TypeKind.INT, "int"), raw, false));
    assertNotNull(conversion.convert(type(TypeKind.LONG, "long"), raw, false));
    assertNotNull(conversion.convert(type(TypeKind.CHAR, "char"), raw, false));
    assertNotNull(conversion.convert(type(TypeKind.FLOAT, "float"), raw, false));
    assertNotNull(conversion.convert(type(TypeKind.DOUBLE, "double"), raw, false));
    final TypeMirror voidType = type(TypeKind.VOID, "void");
    when(types.erasure(voidType)).thenAnswer(invocation -> voidType);
    assertNotNull(conversion.convert(voidType, raw, false));

    assertNotNull(conversion.convert(declared(types, "java.lang.Boolean"), raw, false));
    assertNotNull(conversion.convert(declared(types, "java.lang.Byte"), raw, false));
    assertNotNull(conversion.convert(declared(types, "java.lang.Short"), raw, false));
    assertNotNull(conversion.convert(declared(types, "java.lang.Integer"), raw, false));
    assertNotNull(conversion.convert(declared(types, "java.lang.Long"), raw, false));
    assertNotNull(conversion.convert(declared(types, "java.lang.Character"), raw, false));
    assertNotNull(conversion.convert(declared(types, "java.lang.Float"), raw, false));
    assertNotNull(conversion.convert(declared(types, "java.lang.Double"), raw, false));

    final TypeMirror custom = declared(types, "cc.asylum.Item");
    when(types.erasure(custom)).thenAnswer(invocation -> custom);
    assertNotNull(conversion.convert(custom, raw, false));
  }

  @Test
  void wrapsOptional() {
    final Types types = mock(Types.class);
    final RequestConversion conversion = new RequestConversion(types);
    final Expr raw = Expr.lit("1");
    assertNotNull(conversion.optionalWrap(declared(types, "java.lang.Long"), raw));
    assertNotNull(conversion.optionalWrap(declared(types, "java.lang.Integer"), raw));
    assertNotNull(conversion.optionalWrap(declared(types, "java.lang.Double"), raw));
    assertNotNull(conversion.optionalWrap(declared(types, "java.lang.Float"), raw));
    assertNotNull(conversion.optionalWrap(declared(types, "java.lang.Boolean"), raw));
    assertNotNull(conversion.optionalWrap(declared(types, "java.lang.Short"), raw));
    assertNotNull(conversion.optionalWrap(declared(types, "java.lang.Byte"), raw));
    assertNotNull(conversion.optionalWrap(declared(types, "java.lang.String"), raw));
    assertNotNull(conversion.optionalWrap(type(TypeKind.INT, "int"), raw));
  }

  private static TypeMirror declared(final Types types, final String qualified) {
    final TypeMirror type = mock(javax.lang.model.type.DeclaredType.class);
    when(type.getKind()).thenAnswer(invocation -> TypeKind.DECLARED);
    when(type.toString()).thenAnswer(invocation -> qualified);
    final TypeElement element = mock(TypeElement.class);
    final Name name = mock(Name.class);
    when(name.toString()).thenAnswer(invocation -> qualified);
    when(element.getQualifiedName()).thenAnswer(invocation -> name);
    when(types.asElement(type)).thenAnswer(invocation -> element);
    return type;
  }

  private static TypeMirror type(final TypeKind kind, final String text) {
    final TypeMirror type = mock(TypeMirror.class);
    when(type.getKind()).thenAnswer(invocation -> kind);
    when(type.toString()).thenAnswer(invocation -> text);
    return type;
  }
}
