package cc.asylum.iridium.codegen.bean;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.expr.NewExpr;
import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.codegen.write.Registrar;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class BeanRegistrationTest {

  @Test
  void namesClaimsAndRegistration() {
    final Messager messager = mock(Messager.class);
    final RoundEnvironment round = mock(RoundEnvironment.class);
    final TypeElement type = mock(TypeElement.class);
    final ExecutableElement method = mock(ExecutableElement.class);
    when(type.getKind()).thenAnswer(invocation -> ElementKind.CLASS);
    when(method.getKind()).thenAnswer(invocation -> ElementKind.METHOD);
    when(round.getElementsAnnotatedWith(Marker.class)).thenAnswer(invocation -> java.util.Set.<Element>of(type, method));
    final Processing processing = new Processing(mock(javax.lang.model.util.Types.class), mock(javax.lang.model.util.Elements.class), messager, mock(javax.annotation.processing.Filer.class), round);
    final Registrar registrar = Registrar.of("Reg", Runnable.class, Object.class, "pool");
    final BeanRegistration registration = new BeanRegistration(processing, registrar);

    assertEquals(Set.of(type), registration.types(Marker.class, ElementKind.CLASS));
    assertEquals(Set.of(method), registration.methods(Marker.class));
    final Name simple = mock(Name.class);
    when(simple.toString()).thenAnswer(invocation -> "Widget");
    when(type.getSimpleName()).thenAnswer(invocation -> simple);
    assertEquals("widget", registration.beanName(type));
    assertTrue(registration.claim(type, "widget"));
    assertTrue(registration.claimed("widget"));
    assertFalse(registration.claim(type, "widget"));
    assertFalse(registration.claimed("other"));
    verify(messager).printMessage(Diagnostic.Kind.ERROR, "duplicate bean name 'widget'", type);

    registration.put("widget", Expr.nil());
    registration.nest(types -> {
    });
    registration.error(type, "boom");
    assertNotNull(registration.pool());
    verify(messager).printMessage(Diagnostic.Kind.ERROR, "boom", type);
  }

  @Test
  void argsAndConstruct() {
    final Messager messager = mock(Messager.class);
    final Processing processing = new Processing(mock(javax.lang.model.util.Types.class), mock(javax.lang.model.util.Elements.class), messager, mock(javax.annotation.processing.Filer.class), mock(RoundEnvironment.class));
    final Registrar registrar = Registrar.of("Reg", Runnable.class, Object.class, "pool");
    final BeanRegistration registration = new BeanRegistration(processing, registrar);
    assertTrue(registration.args(null).isEmpty());

    final ExecutableElement executable = mock(ExecutableElement.class);
    final VariableElement parameter = mock(VariableElement.class);
    final DeclaredType type = mock(DeclaredType.class);
    when(type.getKind()).thenAnswer(invocation -> TypeKind.DECLARED);
    when(type.toString()).thenAnswer(invocation -> "java.lang.String");
    when(type.asElement()).thenAnswer(invocation -> mock(Element.class));
    when(type.getTypeArguments()).thenAnswer(invocation -> java.util.List.<TypeMirror>of());
    when(parameter.asType()).thenAnswer(invocation -> type);
    final Name simple = mock(Name.class);
    when(simple.contentEquals("bound")).thenAnswer(invocation -> false);
    when(parameter.getSimpleName()).thenAnswer(invocation -> simple);
    when(executable.getParameters()).thenAnswer(invocation -> java.util.List.<VariableElement>of(parameter));
    assertEquals(1, registration.args(executable).size());
    assertEquals(1, registration.args(executable, registrar.pool()).size());

    final TypeElement missing = mock(TypeElement.class);
    when(missing.getEnclosedElements()).thenAnswer(invocation -> java.util.List.<Element>of());
    assertInstanceOf(cc.asylum.forgery.expr.NullExpr.class, registration.construct(missing));
    verify(messager).printMessage(Diagnostic.Kind.ERROR, "type must have exactly one constructor", missing);

    final TypeElement owner = mock(TypeElement.class);
    final Name qualified = mock(Name.class);
    when(qualified.toString()).thenAnswer(invocation -> "app.Widget");
    when(owner.getQualifiedName()).thenAnswer(invocation -> qualified);
    final ExecutableElement constructor = mock(ExecutableElement.class);
    when(constructor.getKind()).thenAnswer(invocation -> ElementKind.CONSTRUCTOR);
    when(constructor.getParameters()).thenAnswer(invocation -> java.util.List.<VariableElement>of());
    when(owner.getEnclosedElements()).thenAnswer(invocation -> java.util.List.<Element>of(constructor));
    final DeclaredType ownerType = mock(DeclaredType.class);
    when(ownerType.getKind()).thenAnswer(invocation -> TypeKind.DECLARED);
    when(ownerType.asElement()).thenAnswer(invocation -> owner);
    when(ownerType.getTypeArguments()).thenAnswer(invocation -> java.util.List.<TypeMirror>of());
    when(owner.asType()).thenAnswer(invocation -> ownerType);
    assertInstanceOf(NewExpr.class, registration.construct(owner));
  }

  @Test
  void bindWithoutProvidersIsEmpty() {
    final Processing processing = new Processing(mock(javax.lang.model.util.Types.class), mock(javax.lang.model.util.Elements.class), mock(Messager.class), mock(javax.annotation.processing.Filer.class), mock(RoundEnvironment.class));
    final BeanRegistration registration = new BeanRegistration(processing, Registrar.of("Reg", Runnable.class, Object.class, "pool"));
    assertTrue(registration.bind(mock(VariableElement.class)).isEmpty());
  }

  private @interface Marker {
  }
}
