package cc.asylum.iridium.config.processor.walk;

import cc.asylum.iridium.codegen.Processing;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class ConfigTypesTest {

  @Test
  void classifiesPrimitivesBoxedCollectionsAndEnums() {
    final javax.lang.model.util.Types types = mock(javax.lang.model.util.Types.class);
    final javax.lang.model.util.Elements elements = mock(javax.lang.model.util.Elements.class);
    final Processing processing = new Processing(
        types,
        elements,
        mock(Messager.class),
        mock(javax.annotation.processing.Filer.class),
        mock(javax.annotation.processing.RoundEnvironment.class));
    final ConfigTypes configTypes = new ConfigTypes(processing);

    assertNotNull(configTypes.scalar(kind(TypeKind.BOOLEAN)));
    assertNotNull(configTypes.scalar(kind(TypeKind.INT)));
    assertNotNull(configTypes.scalar(kind(TypeKind.LONG)));
    assertNotNull(configTypes.scalar(kind(TypeKind.DOUBLE)));
    assertNotNull(configTypes.scalar(kind(TypeKind.FLOAT)));
    assertNotNull(configTypes.scalar(kind(TypeKind.SHORT)));
    assertNotNull(configTypes.scalar(kind(TypeKind.BYTE)));
    assertNull(configTypes.scalar(kind(TypeKind.CHAR)));
    assertNull(configTypes.scalar(kind(TypeKind.VOID)));

    stubSame(types, elements, String.class, Boolean.class, Integer.class, Long.class, Double.class, Float.class, Short.class, Byte.class);

    assertNotNull(configTypes.scalar(declared(types, "java.lang.String", ElementKind.CLASS)));
    assertNotNull(configTypes.scalar(declared(types, "java.lang.Boolean", ElementKind.CLASS)));
    assertNotNull(configTypes.scalar(declared(types, "java.lang.Integer", ElementKind.CLASS)));
    assertNotNull(configTypes.scalar(declared(types, "java.lang.Long", ElementKind.CLASS)));
    assertNotNull(configTypes.scalar(declared(types, "java.lang.Double", ElementKind.CLASS)));
    assertNotNull(configTypes.scalar(declared(types, "java.lang.Float", ElementKind.CLASS)));
    assertNotNull(configTypes.scalar(declared(types, "java.lang.Short", ElementKind.CLASS)));
    assertNotNull(configTypes.scalar(declared(types, "java.lang.Byte", ElementKind.CLASS)));
    assertNotNull(configTypes.scalar(declared(types, "pkg.Mode", ElementKind.ENUM)));

    final TypeMirror widget = declared(types, "pkg.Widget", ElementKind.CLASS);
    assertNull(configTypes.scalar(widget));
    assertFalse(configTypes.isCollection(widget));
    assertTrue(configTypes.isObject(widget));
    assertTrue(configTypes.isCollection(declared(types, "java.util.List", ElementKind.INTERFACE)));
    assertTrue(configTypes.isCollection(declared(types, "java.util.Set", ElementKind.INTERFACE)));
    assertTrue(configTypes.isCollection(declared(types, "java.util.Map", ElementKind.INTERFACE)));
    assertFalse(configTypes.isCollection(kind(TypeKind.INT)));
    when(types.asElement(widget)).thenAnswer(invocation -> mock(Element.class));
    assertFalse(configTypes.isCollection(widget));
    assertFalse(configTypes.isObject(kind(TypeKind.INT)));
    assertFalse(configTypes.isObject(declared(types, "java.util.List", ElementKind.INTERFACE)));

    final DeclaredType optional = mock(DeclaredType.class);
    when(optional.getKind()).thenAnswer(invocation -> TypeKind.DECLARED);
    final TypeElement optionalElement = typeElement("java.util.Optional", ElementKind.CLASS);
    when(optional.asElement()).thenAnswer(invocation -> optionalElement);
    when(types.asElement(optional)).thenAnswer(invocation -> optionalElement);
    when(optional.getTypeArguments()).thenAnswer(invocation -> List.<TypeMirror>of(kind(TypeKind.INT)));
    assertFalse(configTypes.isObject(optional));
    assertTrue(configTypes.same(declared(types, "java.lang.String", ElementKind.CLASS), String.class));
    assertFalse(configTypes.same(widget, Integer.class));
  }

  private static void stubSame(
      final javax.lang.model.util.Types types,
      final javax.lang.model.util.Elements elements,
      final Class<?>... targets
  ) {
    final java.util.Map<String, TypeMirror> mirrors = new java.util.LinkedHashMap<>();
    for (final Class<?> target : targets) {
      final TypeElement element = typeElement(target.getCanonicalName(), ElementKind.CLASS);
      final TypeMirror mirror = mock(TypeMirror.class);
      when(element.asType()).thenAnswer(invocation -> mirror);
      when(elements.getTypeElement(target.getCanonicalName())).thenAnswer(invocation -> element);
      mirrors.put(target.getCanonicalName(), mirror);
    }
    when(types.isSameType(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> {
          final Object left = invocation.getArgument(0);
          final Object right = invocation.getArgument(1);
          if (!(left instanceof TypeMirror type) || type.getKind() != TypeKind.DECLARED) {
            return false;
          }
          final Element owner = types.asElement(type);
          if (!(owner instanceof TypeElement typeElement)) {
            return false;
          }
          final String qualified = typeElement.getQualifiedName().toString();
          return right == mirrors.get(qualified);
        });
  }

  private static TypeMirror kind(final TypeKind kind) {
    final TypeMirror type = mock(TypeMirror.class);
    when(type.getKind()).thenAnswer(invocation -> kind);
    return type;
  }

  private static TypeMirror declared(
      final javax.lang.model.util.Types types,
      final String qualified,
      final ElementKind kind
  ) {
    final DeclaredType type = mock(DeclaredType.class);
    when(type.getKind()).thenAnswer(invocation -> TypeKind.DECLARED);
    final TypeElement element = typeElement(qualified, kind);
    when(type.asElement()).thenAnswer(invocation -> element);
    when(types.asElement(type)).thenAnswer(invocation -> element);
    when(type.getTypeArguments()).thenAnswer(invocation -> List.<TypeMirror>of());
    return type;
  }

  private static TypeElement typeElement(final String qualified, final ElementKind kind) {
    final TypeElement element = mock(TypeElement.class);
    final Name name = mock(Name.class);
    when(name.toString()).thenAnswer(invocation -> qualified);
    when(name.contentEquals(qualified)).thenAnswer(invocation -> true);
    when(element.getQualifiedName()).thenAnswer(invocation -> name);
    when(element.getKind()).thenAnswer(invocation -> kind);
    return element;
  }
}
