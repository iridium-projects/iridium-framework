package cc.asylum.iridium.codegen.model;

import org.junit.jupiter.api.Test;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class ElementsTest {

  @Test
  void packagesAndPrefixes() {
    assertEquals("cc.asylum.iridium", Elements.rootPackage());
    final javax.lang.model.util.Elements utils = mock(javax.lang.model.util.Elements.class);
    final Element element = mock(Element.class);
    final PackageElement pkg = mock(PackageElement.class);
    final Name name = mock(Name.class);
    when(name.toString()).thenAnswer(invocation -> "a.b");
    when(pkg.getQualifiedName()).thenAnswer(invocation -> name);
    when(utils.getPackageOf(element)).thenAnswer(invocation -> pkg);
    assertEquals("a.b", Elements.packageOf(utils, element));
    assertEquals("", Elements.commonPrefix(Set.of()));
    assertEquals("a.b", Elements.commonPrefix(Set.of("a.b")));
    assertEquals("a.b", Elements.commonPrefix("a.b.c", "a.b.d"));
    assertEquals("", Elements.commonPrefix("a.b", "x.y"));
    assertEquals("a", Elements.commonPrefix("a", "a.b"));
    assertEquals("cc.asylum.iridium.gen", Elements.generatedPackage(utils, List.of()));
  }

  @Test
  void generatedPackageUsesCommonPrefix() {
    final javax.lang.model.util.Elements utils = mock(javax.lang.model.util.Elements.class);
    final Element left = packaged(utils, "app.web");
    final Element right = packaged(utils, "app.api");
    assertEquals("app.gen", Elements.generatedPackage(utils, List.of(left, right)));
  }

  @Test
  void decapitalize() {
    assertNull(Elements.decapitalize(null));
    assertEquals("", Elements.decapitalize(""));
    assertEquals("widget", Elements.decapitalize("Widget"));
    assertEquals("a", Elements.decapitalize("A"));
  }

  @Test
  void rootsAndAnnotated() {
    final RoundEnvironment round = mock(RoundEnvironment.class);
    final TypeElement type = mock(TypeElement.class);
    final Element field = mock(Element.class);
    when(type.getKind()).thenAnswer(invocation -> ElementKind.CLASS);
    when(field.getKind()).thenAnswer(invocation -> ElementKind.FIELD);
    when(round.getRootElements()).thenAnswer(invocation -> java.util.Set.<Element>of(type, field));
    assertEquals(List.of(type), Elements.rootTypes(round, ElementKind.CLASS));
    assertTrue(Elements.rootTypes(round).isEmpty());

    final ExecutableElement method = mock(ExecutableElement.class);
    when(method.getKind()).thenAnswer(invocation -> ElementKind.METHOD);
    when(round.getElementsAnnotatedWith(Marker.class)).thenAnswer(invocation -> java.util.Set.<Element>of(type, method));
    assertEquals(Set.of(type), Elements.annotatedTypes(round, Marker.class, ElementKind.CLASS));
    assertEquals(Set.of(method), Elements.annotatedMethods(round, Marker.class));
  }

  @Test
  void constructor() {
    final TypeElement none = mock(TypeElement.class);
    when(none.getEnclosedElements()).thenAnswer(invocation -> java.util.List.<Element>of());
    assertNull(Elements.constructor(none));

    final TypeElement one = mock(TypeElement.class);
    final ExecutableElement constructor = mock(ExecutableElement.class);
    final Element method = mock(Element.class);
    when(constructor.getKind()).thenAnswer(invocation -> ElementKind.CONSTRUCTOR);
    when(method.getKind()).thenAnswer(invocation -> ElementKind.METHOD);
    when(one.getEnclosedElements()).thenAnswer(invocation -> java.util.List.<Element>of(method, constructor));
    assertSame(constructor, Elements.constructor(one));

    final TypeElement two = mock(TypeElement.class);
    final ExecutableElement other = mock(ExecutableElement.class);
    when(other.getKind()).thenAnswer(invocation -> ElementKind.CONSTRUCTOR);
    when(two.getEnclosedElements()).thenAnswer(invocation -> java.util.List.<Element>of(constructor, other));
    assertNull(Elements.constructor(two));
  }

  private static Element packaged(final javax.lang.model.util.Elements utils, final String qualified) {
    final Element element = mock(Element.class);
    final PackageElement pkg = mock(PackageElement.class);
    final Name name = mock(Name.class);
    when(name.toString()).thenAnswer(invocation -> qualified);
    when(pkg.getQualifiedName()).thenAnswer(invocation -> name);
    when(utils.getPackageOf(element)).thenAnswer(invocation -> pkg);
    return element;
  }

  private @interface Marker {
  }
}
