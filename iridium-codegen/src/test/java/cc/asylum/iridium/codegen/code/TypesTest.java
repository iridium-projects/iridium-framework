package cc.asylum.iridium.codegen.code;

import cc.asylum.forgery.model.ArrayRef;
import cc.asylum.forgery.model.ClassRef;
import cc.asylum.forgery.model.ParameterizedRef;
import cc.asylum.forgery.model.TypeRef;
import cc.asylum.forgery.model.TypeVarRef;
import cc.asylum.forgery.model.WildcardRef;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class TypesTest {

  @Test
  void ofClassNameAndPackage() {
    assertEquals(TypeRef.STRING, Types.of(String.class));
    assertEquals("java.lang.String", ((ClassRef) Types.of("java.lang", "String")).canonicalName());
    assertEquals("Local", ((ClassRef) Types.of("", "Local")).canonicalName());
    assertEquals("Local", ((ClassRef) Types.of(null, "Local")).canonicalName());
    assertEquals("a.B", ((ClassRef) Types.of("a.B")).canonicalName());
  }

  @Test
  void ofTypeElement() {
    final TypeElement element = typeElement("pkg.Widget");
    assertEquals("pkg.Widget", ((ClassRef) Types.of(element)).canonicalName());
  }

  @Test
  void primitivesAndDefault() {
    assertSame(TypeRef.BOOLEAN, Types.of(kind(TypeKind.BOOLEAN)));
    assertSame(TypeRef.BYTE, Types.of(kind(TypeKind.BYTE)));
    assertSame(TypeRef.SHORT, Types.of(kind(TypeKind.SHORT)));
    assertSame(TypeRef.INT, Types.of(kind(TypeKind.INT)));
    assertSame(TypeRef.LONG, Types.of(kind(TypeKind.LONG)));
    assertSame(TypeRef.CHAR, Types.of(kind(TypeKind.CHAR)));
    assertSame(TypeRef.FLOAT, Types.of(kind(TypeKind.FLOAT)));
    assertSame(TypeRef.DOUBLE, Types.of(kind(TypeKind.DOUBLE)));
    assertSame(TypeRef.VOID, Types.of(kind(TypeKind.VOID)));
    final TypeMirror error = kind(TypeKind.ERROR);
    when(error.toString()).thenAnswer(invocation -> "error.Type");
    assertEquals("error.Type", ((ClassRef) Types.of(error)).canonicalName());
  }

  @Test
  void arrayTypevarAndDeclared() {
    final TypeMirror component = kind(TypeKind.INT);
    final ArrayType array = mock(ArrayType.class);
    when(array.getKind()).thenAnswer(invocation -> TypeKind.ARRAY);
    when(array.getComponentType()).thenAnswer(invocation -> component);
    assertInstanceOf(ArrayRef.class, Types.of(array));

    final TypeVariable variable = mock(TypeVariable.class);
    when(variable.getKind()).thenAnswer(invocation -> TypeKind.TYPEVAR);
    when(variable.asElement()).thenAnswer(invocation -> named("T"));
    assertInstanceOf(TypeVarRef.class, Types.of(variable));

    final DeclaredType raw = declared("java.lang.String", List.of());
    assertEquals("java.lang.String", ((ClassRef) Types.of(raw)).canonicalName());

    final DeclaredType parameterized = declared("java.util.List", List.of(kind(TypeKind.INT)));
    assertInstanceOf(ParameterizedRef.class, Types.of(parameterized));

    final DeclaredType unknown = mock(DeclaredType.class);
    when(unknown.getKind()).thenAnswer(invocation -> TypeKind.DECLARED);
    when(unknown.asElement()).thenAnswer(invocation -> named("not-a-type"));
    when(unknown.toString()).thenAnswer(invocation -> "not.a.Type");
    when(unknown.getTypeArguments()).thenAnswer(invocation -> java.util.List.<TypeMirror>of());
    assertEquals("not.a.Type", ((ClassRef) Types.of(unknown)).canonicalName());
  }

  @Test
  void wildcardsAndFactories() {
    final WildcardType unbounded = mock(WildcardType.class);
    when(unbounded.getKind()).thenAnswer(invocation -> TypeKind.WILDCARD);
    when(unbounded.getExtendsBound()).thenAnswer(invocation -> null);
    when(unbounded.getSuperBound()).thenAnswer(invocation -> null);
    assertInstanceOf(WildcardRef.class, Types.of(unbounded));

    final WildcardType extendsBound = mock(WildcardType.class);
    when(extendsBound.getKind()).thenAnswer(invocation -> TypeKind.WILDCARD);
    when(extendsBound.getExtendsBound()).thenAnswer(invocation -> kind(TypeKind.INT));
    assertInstanceOf(WildcardRef.class, Types.of(extendsBound));

    final WildcardType superBound = mock(WildcardType.class);
    when(superBound.getKind()).thenAnswer(invocation -> TypeKind.WILDCARD);
    when(superBound.getExtendsBound()).thenAnswer(invocation -> null);
    when(superBound.getSuperBound()).thenAnswer(invocation -> kind(TypeKind.INT));
    assertInstanceOf(WildcardRef.class, Types.of(superBound));

    assertInstanceOf(ParameterizedRef.class, Types.parameterized(List.class, TypeRef.STRING));
    assertInstanceOf(ParameterizedRef.class, Types.parameterized(TypeRef.of(List.class), TypeRef.STRING));
    assertInstanceOf(ParameterizedRef.class, Types.list(TypeRef.STRING));
    assertInstanceOf(ParameterizedRef.class, Types.optional(TypeRef.STRING));
    assertInstanceOf(WildcardRef.class, Types.wildcardExtends(TypeRef.STRING));
  }

  private static TypeMirror kind(final TypeKind kind) {
    final TypeMirror type = mock(TypeMirror.class);
    when(type.getKind()).thenAnswer(invocation -> kind);
    return type;
  }

  private static Element named(final String name) {
    final Element element = mock(Element.class);
    final Name simple = mock(Name.class);
    when(simple.toString()).thenAnswer(invocation -> name);
    when(element.getSimpleName()).thenAnswer(invocation -> simple);
    return element;
  }

  private static TypeElement typeElement(final String qualified) {
    final TypeElement element = mock(TypeElement.class);
    final Name name = mock(Name.class);
    when(name.toString()).thenAnswer(invocation -> qualified);
    when(element.getQualifiedName()).thenAnswer(invocation -> name);
    return element;
  }

  private static DeclaredType declared(final String qualified, final List<? extends TypeMirror> arguments) {
    final DeclaredType type = mock(DeclaredType.class);
    when(type.getKind()).thenAnswer(invocation -> TypeKind.DECLARED);
    when(type.asElement()).thenAnswer(invocation -> typeElement(qualified));
    when(type.getTypeArguments()).thenAnswer(invocation -> arguments);
    return type;
  }
}
