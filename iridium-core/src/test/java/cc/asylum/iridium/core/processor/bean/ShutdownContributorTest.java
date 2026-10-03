package cc.asylum.iridium.core.processor.bean;

import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.codegen.Register;
import cc.asylum.iridium.codegen.bean.BeanRegistration;
import cc.asylum.iridium.codegen.write.Registrar;
import cc.asylum.iridium.core.bean.BeanPool;
import cc.asylum.iridium.core.hook.OnShutdown;
import cc.asylum.iridium.core.processor.Mirrors;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class ShutdownContributorTest {

  @Test
  void nestsHookAndReportsUnregisteredOwners() {
    final Messager messager = mock(Messager.class);
    final RoundEnvironment round = mock(RoundEnvironment.class);
    final TypeElement owner = owner("Owner", "app.Owner", true);
    final TypeElement missing = owner("Missing", "app.Missing", true);
    final TypeElement stray = owner("Stray", "app.Stray", false);
    final ExecutableElement close = method("close", owner, 5);
    final ExecutableElement blank = method("", owner, 0);
    final ExecutableElement unclaimed = method("stop", missing, 1);
    final ExecutableElement orphan = method("halt", stray, 2);
    when(round.getElementsAnnotatedWith(OnShutdown.class)).thenAnswer(invocation -> Set.<Element>of(close, blank, unclaimed, orphan));
    final Processing processing = new Processing(
      mock(javax.lang.model.util.Types.class),
      mock(javax.lang.model.util.Elements.class),
      messager,
      mock(javax.annotation.processing.Filer.class),
      round);
    final BeanRegistration registration = new BeanRegistration(processing, Registrar.of("Reg", Runnable.class, BeanPool.class, "pool"));
    registration.claim(owner, "owner");

    new ShutdownContributor().contribute(registration);
    verify(messager).printMessage(Diagnostic.Kind.ERROR, "@OnShutdown methods must be declared on a @Register type", orphan);
    verify(messager).printMessage(Diagnostic.Kind.ERROR, "no bean registered for 'missing'", missing);
  }

  private static TypeElement owner(final String simple, final String qualified, final boolean registered) {
    final TypeElement type = mock(TypeElement.class);
    when(type.getKind()).thenAnswer(invocation -> ElementKind.CLASS);
    when(type.getSimpleName()).thenAnswer(invocation -> Mirrors.name(simple));
    when(type.getQualifiedName()).thenAnswer(invocation -> Mirrors.name(qualified));
    when(type.asType()).thenAnswer(invocation -> Mirrors.declared(qualified));
    if (!registered) {
      when(type.getAnnotationMirrors()).thenAnswer(invocation -> List.of());
      return type;
    }
    final AnnotationMirror mirror = mock(AnnotationMirror.class);
    final DeclaredType annotationType = mock(DeclaredType.class);
    final TypeElement annotation = mock(TypeElement.class);
    when(annotation.getAnnotation(Register.class)).thenAnswer(invocation -> register());
    when(annotationType.asElement()).thenAnswer(invocation -> annotation);
    when(mirror.getAnnotationType()).thenAnswer(invocation -> annotationType);
    when(type.getAnnotationMirrors()).thenAnswer(invocation -> List.of(mirror));
    return type;
  }

  private static ExecutableElement method(final String name, final TypeElement owner, final int priority) {
    final ExecutableElement method = mock(ExecutableElement.class);
    when(method.getKind()).thenAnswer(invocation -> ElementKind.METHOD);
    when(method.getSimpleName()).thenAnswer(invocation -> Mirrors.name(name));
    when(method.getEnclosingElement()).thenAnswer(invocation -> owner);
    when(method.getAnnotation(OnShutdown.class)).thenAnswer(invocation -> shutdown(priority));
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

  private static OnShutdown shutdown(final int priority) {
    return new OnShutdown() {
      @Override
      public int priority() {
        return priority;
      }

      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return OnShutdown.class;
      }
    };
  }
}
