package cc.asylum.iridium.config.processor;

import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.codegen.bean.BeanRegistration;
import cc.asylum.iridium.codegen.write.Registrar;
import cc.asylum.iridium.config.ConfigurationProperties;
import cc.asylum.iridium.core.component.Component;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class ConfigContributorTest {

  @Test
  void contributesConcreteConfigsAndSkipsConflicts() {
    final Messager messager = mock(Messager.class);
    final RoundEnvironment round = mock(RoundEnvironment.class);
    final TypeElement ok = config("app.Server", "server", false);
    final ExecutableElement constructor = mock(ExecutableElement.class);
    when(constructor.getKind()).thenAnswer(invocation -> ElementKind.CONSTRUCTOR);
    when(constructor.getModifiers()).thenAnswer(invocation -> Set.of(Modifier.PUBLIC));
    when(constructor.getParameters()).thenAnswer(invocation -> List.of());
    when(ok.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor));

    final TypeElement component = config("app.Mixed", "mixed", true);
    final TypeElement duplicate = config("app.ServerAgain", "again", false);
    when(duplicate.getSimpleName()).thenAnswer(invocation -> name("Server"));
    when(round.getElementsAnnotatedWith(ConfigurationProperties.class)).thenAnswer(invocation -> Set.<Element>of(ok, component, duplicate));

    final Processing processing = new Processing(
        mock(javax.lang.model.util.Types.class),
        mock(javax.lang.model.util.Elements.class),
        messager,
        mock(javax.annotation.processing.Filer.class),
        round);
    final BeanRegistration registration = new BeanRegistration(processing, Registrar.of("Reg", Runnable.class, Object.class, "pool"));
    new ConfigContributor().contribute(registration);
    verify(messager).printMessage(
        javax.tools.Diagnostic.Kind.ERROR,
        "@ConfigurationProperties cannot be combined with @Component",
        component);
  }

  @Test
  void skipsInvalidConfigurationTypes() {
    final Messager messager = mock(Messager.class);
    final RoundEnvironment round = mock(RoundEnvironment.class);
    final TypeElement valid = config("app.Ready", "ready", false);
    final ExecutableElement constructor = mock(ExecutableElement.class);
    when(constructor.getKind()).thenAnswer(invocation -> ElementKind.CONSTRUCTOR);
    when(constructor.getModifiers()).thenAnswer(invocation -> Set.of(Modifier.PUBLIC));
    when(constructor.getParameters()).thenAnswer(invocation -> List.of());
    when(valid.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor));
    final TypeElement invalid = config("app.Broken", ".bad", false);
    when(round.getElementsAnnotatedWith(ConfigurationProperties.class)).thenAnswer(invocation -> Set.<Element>of(valid, invalid));
    final Processing processing = new Processing(
        mock(javax.lang.model.util.Types.class),
        mock(javax.lang.model.util.Elements.class),
        messager,
        mock(javax.annotation.processing.Filer.class),
        round);
    new ConfigContributor().contribute(new BeanRegistration(processing, Registrar.of("Reg", Runnable.class, Object.class, "pool")));
    verify(messager).printMessage(
        javax.tools.Diagnostic.Kind.ERROR,
        "invalid configuration prefix '.bad'",
        invalid);
  }

  private static TypeElement config(final String qualified, final String prefix, final boolean component) {
    final TypeElement type = mock(TypeElement.class);
    final Name qualifiedName = name(qualified);
    when(type.getQualifiedName()).thenAnswer(invocation -> qualifiedName);
    when(type.getKind()).thenAnswer(invocation -> ElementKind.CLASS);
    when(type.getSimpleName()).thenAnswer(invocation -> name(qualified.substring(qualified.lastIndexOf('.') + 1)));
    when(type.getModifiers()).thenAnswer(invocation -> Set.of(Modifier.PUBLIC));
    when(type.getTypeParameters()).thenAnswer(invocation -> List.<TypeParameterElement>of());
    when(type.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of());
    final ConfigurationProperties properties = mock(ConfigurationProperties.class);
    when(properties.value()).thenAnswer(invocation -> prefix);
    when(type.getAnnotation(ConfigurationProperties.class)).thenAnswer(invocation -> properties);
    if (component) {
      when(type.getAnnotation(Component.class)).thenAnswer(invocation -> mock(Component.class));
    }
    return type;
  }

  private static Name name(final String value) {
    final Name name = mock(Name.class);
    when(name.toString()).thenAnswer(invocation -> value);
    return name;
  }
}
