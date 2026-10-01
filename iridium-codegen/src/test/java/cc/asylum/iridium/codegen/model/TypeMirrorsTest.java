package cc.asylum.iridium.codegen.model;

import org.junit.jupiter.api.Test;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class TypeMirrorsTest {

  @Test
  void assignableAndSame() {
    final javax.lang.model.util.Types types = mock(javax.lang.model.util.Types.class);
    final javax.lang.model.util.Elements elements = mock(javax.lang.model.util.Elements.class);
    final TypeMirror type = mock(TypeMirror.class);
    final TypeElement target = mock(TypeElement.class);
    final TypeMirror targetType = mock(TypeMirror.class);
    when(target.asType()).thenAnswer(invocation -> targetType);
    when(elements.getTypeElement("java.lang.String")).thenAnswer(invocation -> target);
    when(types.isAssignable(type, targetType)).thenAnswer(invocation -> true);
    when(types.isSameType(type, targetType)).thenAnswer(invocation -> true);
    assertTrue(TypeMirrors.isAssignable(types, elements, type, String.class));
    assertTrue(TypeMirrors.isAssignable(types, elements, type, "java.lang.String"));
    assertTrue(TypeMirrors.isSame(types, elements, type, String.class));
    assertTrue(TypeMirrors.isSame(types, elements, type, "java.lang.String"));
    when(elements.getTypeElement("missing.Type")).thenAnswer(invocation -> null);
    assertFalse(TypeMirrors.isAssignable(types, elements, type, "missing.Type"));
    assertFalse(TypeMirrors.isSame(types, elements, type, "missing.Type"));
  }

  @Test
  void qualifiedAndBoxed() {
    final javax.lang.model.util.Types types = mock(javax.lang.model.util.Types.class);
    final TypeMirror type = mock(TypeMirror.class);
    final TypeElement element = typeElement("pkg.Widget", ElementKind.CLASS);
    when(types.asElement(type)).thenAnswer(invocation -> element);
    assertEquals("pkg.Widget", TypeMirrors.qualified(types, type));
    when(types.asElement(type)).thenAnswer(invocation -> mock(Element.class));
    when(type.getKind()).thenAnswer(invocation -> TypeKind.ERROR);
    when(type.toString()).thenAnswer(invocation -> "raw");
    assertEquals("raw", TypeMirrors.qualified(types, type));

    assertEquals("java.lang.Boolean", TypeMirrors.boxed("boolean"));
    assertEquals("java.lang.Byte", TypeMirrors.boxed("byte"));
    assertEquals("java.lang.Short", TypeMirrors.boxed("short"));
    assertEquals("java.lang.Integer", TypeMirrors.boxed("int"));
    assertEquals("java.lang.Long", TypeMirrors.boxed("long"));
    assertEquals("java.lang.Float", TypeMirrors.boxed("float"));
    assertEquals("java.lang.Double", TypeMirrors.boxed("double"));
    assertEquals("java.lang.Character", TypeMirrors.boxed("char"));
    assertEquals("pkg.Widget", TypeMirrors.boxed("pkg.Widget"));

    assertEquals("java.lang.Boolean", TypeMirrors.boxed(types, kind(TypeKind.BOOLEAN)));
    assertEquals("java.lang.Byte", TypeMirrors.boxed(types, kind(TypeKind.BYTE)));
    assertEquals("java.lang.Short", TypeMirrors.boxed(types, kind(TypeKind.SHORT)));
    assertEquals("java.lang.Integer", TypeMirrors.boxed(types, kind(TypeKind.INT)));
    assertEquals("java.lang.Long", TypeMirrors.boxed(types, kind(TypeKind.LONG)));
    assertEquals("java.lang.Float", TypeMirrors.boxed(types, kind(TypeKind.FLOAT)));
    assertEquals("java.lang.Double", TypeMirrors.boxed(types, kind(TypeKind.DOUBLE)));
    assertEquals("java.lang.Character", TypeMirrors.boxed(types, kind(TypeKind.CHAR)));
    when(types.asElement(type)).thenAnswer(invocation -> element);
    assertEquals("pkg.Widget", TypeMirrors.boxed(types, type));
  }

  @Test
  void stringEnumAndOptional() {
    final javax.lang.model.util.Types types = mock(javax.lang.model.util.Types.class);
    final TypeMirror string = mock(TypeMirror.class);
    when(types.asElement(string)).thenAnswer(invocation -> typeElement("java.lang.String", ElementKind.CLASS));
    assertTrue(TypeMirrors.isString(types, string));
    final TypeMirror sequence = mock(TypeMirror.class);
    when(types.asElement(sequence)).thenAnswer(invocation -> typeElement("java.lang.CharSequence", ElementKind.INTERFACE));
    assertTrue(TypeMirrors.isString(types, sequence));
    final TypeMirror other = mock(TypeMirror.class);
    when(types.asElement(other)).thenAnswer(invocation -> typeElement("java.lang.Integer", ElementKind.CLASS));
    assertFalse(TypeMirrors.isString(types, other));
    assertFalse(TypeMirrors.isEnum(types, other));
    final TypeMirror enumeration = mock(TypeMirror.class);
    when(types.asElement(enumeration)).thenAnswer(invocation -> typeElement("pkg.Color", ElementKind.ENUM));
    assertTrue(TypeMirrors.isEnum(types, enumeration));
    when(types.asElement(other)).thenAnswer(invocation -> mock(Element.class));
    when(other.toString()).thenAnswer(invocation -> "x");
    assertFalse(TypeMirrors.isEnum(types, other));

    assertTrue(TypeMirrors.optionalValue(types, kind(TypeKind.INT)).isEmpty());
    final DeclaredType notOptional = declared("java.util.List", List.of());
    when(types.asElement(notOptional)).thenAnswer(invocation -> (Element) notOptional.asElement());
    assertTrue(TypeMirrors.optionalValue(types, notOptional).isEmpty());
    final DeclaredType empty = declared("java.util.Optional", List.of());
    when(types.asElement(empty)).thenAnswer(invocation -> (Element) empty.asElement());
    assertTrue(TypeMirrors.optionalValue(types, empty).isEmpty());
    final TypeMirror argument = kind(TypeKind.INT);
    final DeclaredType optional = declared("java.util.Optional", List.of(argument));
    when(types.asElement(optional)).thenAnswer(invocation -> (Element) optional.asElement());
    assertSame(argument, TypeMirrors.optionalValue(types, optional).orElseThrow());
  }

  private static TypeMirror kind(final TypeKind kind) {
    final TypeMirror type = mock(TypeMirror.class);
    when(type.getKind()).thenAnswer(invocation -> kind);
    return type;
  }

  private static TypeElement typeElement(final String qualified, final ElementKind kind) {
    final TypeElement element = mock(TypeElement.class);
    final Name name = mock(Name.class);
    when(name.toString()).thenAnswer(invocation -> qualified);
    when(element.getQualifiedName()).thenAnswer(invocation -> name);
    when(element.getKind()).thenAnswer(invocation -> kind);
    return element;
  }

  private static DeclaredType declared(final String qualified, final List<? extends TypeMirror> arguments) {
    final DeclaredType type = mock(DeclaredType.class);
    when(type.getKind()).thenAnswer(invocation -> TypeKind.DECLARED);
    when(type.asElement()).thenAnswer(invocation -> typeElement(qualified, ElementKind.CLASS));
    when(type.getTypeArguments()).thenAnswer(invocation -> arguments);
    return type;
  }
}
