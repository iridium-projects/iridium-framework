package cc.asylum.iridium.core.processor.validation;

import cc.asylum.iridium.core.processor.Mirrors;
import cc.asylum.iridium.core.validation.annotation.NotNull;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class ValidationProcessorTest {

  @Test
  void processAndFinishWriteRegistrar() throws Exception {
    final Elements elements = mock(Elements.class);
    final ProcessingEnvironment environment = environment(elements);
    final ValidationProcessor processor = new ValidationProcessor();
    processor.init(environment);
    assertEquals(18, processor.getSupportedAnnotationTypes().size());
    assertEquals(SourceVersion.latestSupported(), processor.getSupportedSourceVersion());

    final TypeElement type = mock(TypeElement.class);
    when(type.getKind()).thenAnswer(invocation -> ElementKind.CLASS);
    when(type.getSimpleName()).thenAnswer(invocation -> Mirrors.name("Widget"));
    when(type.getQualifiedName()).thenAnswer(invocation -> Mirrors.name("app.Widget"));
    when(type.asType()).thenAnswer(invocation -> Mirrors.declared("app.Widget"));
    final VariableElement field = mock(VariableElement.class);
    when(field.getKind()).thenAnswer(invocation -> ElementKind.FIELD);
    when(field.getSimpleName()).thenAnswer(invocation -> Mirrors.name("name"));
    when(field.asType()).thenAnswer(invocation -> Mirrors.declared("java.lang.String"));
    when(field.getAnnotation(NotNull.class)).thenAnswer(invocation -> notNull());
    when(field.getAnnotationMirrors()).thenAnswer(invocation -> java.util.List.of());
    Mirrors.constrain(field, NotNull.class);
    when(type.getEnclosedElements()).thenAnswer(invocation -> java.util.List.<Element>of(field));
    final PackageElement pkg = mock(PackageElement.class);
    when(pkg.getQualifiedName()).thenAnswer(invocation -> Mirrors.name("app"));
    when(elements.getPackageOf(type)).thenAnswer(invocation -> pkg);

    final RoundEnvironment round = mock(RoundEnvironment.class);
    when(round.processingOver()).thenAnswer(invocation -> false);
    when(round.getRootElements()).thenAnswer(invocation -> Set.<Element>of(type));
    assertFalse(processor.process(Set.of(), round));

    when(round.processingOver()).thenAnswer(invocation -> true);
    assertFalse(processor.process(Set.of(), round));
    assertFalse(processor.process(Set.of(), round));
  }

  private static ProcessingEnvironment environment(final Elements elements) throws Exception {
    final ProcessingEnvironment environment = mock(ProcessingEnvironment.class);
    final Filer filer = mock(Filer.class);
    final JavaFileObject file = mock(JavaFileObject.class);
    when(file.openWriter()).thenAnswer(invocation -> new StringWriter());
    when(filer.createSourceFile(anyString(), any(Element[].class))).thenAnswer(invocation -> file);
    final FileObject resource = mock(FileObject.class);
    when(resource.openWriter()).thenAnswer(invocation -> new StringWriter());
    when(filer.createResource(any(StandardLocation.class), anyString(), anyString(), any(Element[].class))).thenAnswer(invocation -> resource);
    when(environment.getElementUtils()).thenAnswer(invocation -> elements);
    when(environment.getTypeUtils()).thenAnswer(invocation -> mock(Types.class));
    when(environment.getFiler()).thenAnswer(invocation -> filer);
    when(environment.getMessager()).thenAnswer(invocation -> mock(Messager.class));
    when(environment.getLocale()).thenAnswer(invocation -> Locale.ROOT);
    when(environment.getOptions()).thenAnswer(invocation -> Map.of());
    when(environment.getSourceVersion()).thenAnswer(invocation -> SourceVersion.latestSupported());
    return environment;
  }

  private static NotNull notNull() {
    return new NotNull() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return NotNull.class;
      }
    };
  }
}
