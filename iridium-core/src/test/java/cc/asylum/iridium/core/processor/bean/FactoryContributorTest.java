package cc.asylum.iridium.core.processor.bean;

import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.codegen.Register;
import cc.asylum.iridium.codegen.bean.BeanRegistration;
import cc.asylum.iridium.codegen.write.Registrar;
import cc.asylum.iridium.core.bean.Bean;
import cc.asylum.iridium.core.bean.BeanPool;
import cc.asylum.iridium.core.processor.Mirrors;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class FactoryContributorTest {

  @Test
  void registersClaimedFactoryAndReportsMissingOwner() {
    final Messager messager = mock(Messager.class);
    final RoundEnvironment round = mock(RoundEnvironment.class);
    final TypeElement owner = owner(true);
    final TypeElement stray = owner(false);
    final ExecutableElement factory = method("widget", owner);
    final ExecutableElement again = method("widget", owner);
    final ExecutableElement orphan = method("missing", stray);
    when(round.getElementsAnnotatedWith(Bean.class)).thenAnswer(invocation -> Set.<Element>of(factory, again, orphan));
    final Processing processing = new Processing(
      mock(javax.lang.model.util.Types.class),
      mock(javax.lang.model.util.Elements.class),
      messager,
      mock(javax.annotation.processing.Filer.class),
      round);
    final BeanRegistration registration = new BeanRegistration(processing, Registrar.of("Reg", Runnable.class, BeanPool.class, "pool"));
    registration.claim(owner, "owner");

    new FactoryContributor().contribute(registration);
    verify(messager).printMessage(Diagnostic.Kind.ERROR, "@Bean methods must be declared on a @Register type", orphan);
    verify(messager).printMessage(
      org.mockito.ArgumentMatchers.eq(Diagnostic.Kind.ERROR),
      org.mockito.ArgumentMatchers.eq("duplicate bean name 'widget'"),
      org.mockito.ArgumentMatchers.any());
  }

  private static TypeElement owner(final boolean registered) {
    final TypeElement type = mock(TypeElement.class);
    when(type.getKind()).thenAnswer(invocation -> ElementKind.CLASS);
    when(type.getSimpleName()).thenAnswer(invocation -> Mirrors.name(registered ? "Owner" : "Stray"));
    when(type.getQualifiedName()).thenAnswer(invocation -> Mirrors.name(registered ? "app.Owner" : "app.Stray"));
    when(type.asType()).thenAnswer(invocation -> Mirrors.declared(registered ? "app.Owner" : "app.Stray"));
    if (registered) {
      final AnnotationMirror mirror = mock(AnnotationMirror.class);
      final DeclaredType annotationType = mock(DeclaredType.class);
      final TypeElement annotation = mock(TypeElement.class);
      when(annotation.getAnnotation(Register.class)).thenAnswer(invocation -> register());
      when(annotationType.asElement()).thenAnswer(invocation -> annotation);
      when(mirror.getAnnotationType()).thenAnswer(invocation -> annotationType);
      when(type.getAnnotationMirrors()).thenAnswer(invocation -> List.of(mirror));
    } else {
      when(type.getAnnotationMirrors()).thenAnswer(invocation -> List.of());
    }
    return type;
  }

  private static ExecutableElement method(final String name, final TypeElement owner) {
    final ExecutableElement method = mock(ExecutableElement.class);
    when(method.getKind()).thenAnswer(invocation -> ElementKind.METHOD);
    when(method.getSimpleName()).thenAnswer(invocation -> Mirrors.name(name));
    when(method.getEnclosingElement()).thenAnswer(invocation -> owner);
    when(method.getParameters()).thenAnswer(invocation -> List.<VariableElement>of());
    return method;
  }

  private static Register register() {
    return new Register() {
      @Override
      public String suffix() {
        return "";
      }

      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Register.class;
      }
    };
  }
}
