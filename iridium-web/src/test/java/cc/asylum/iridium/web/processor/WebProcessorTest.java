package cc.asylum.iridium.web.processor;

import cc.asylum.iridium.core.component.Component;
import cc.asylum.iridium.web.controller.RestController;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class WebProcessorTest {

  @Test
  void processesAndFinishes() {
    final WebProcessor processor = new WebProcessor();
    processor.init(environment());
    assertEquals(SourceVersion.latestSupported(), processor.getSupportedSourceVersion());
    assertTrue(processor.getSupportedAnnotationTypes().contains(RestController.class.getCanonicalName()));
    assertTrue(processor.getSupportedAnnotationTypes().contains(Component.class.getCanonicalName()));

    final RoundEnvironment idle = mock(RoundEnvironment.class);
    when(idle.processingOver()).thenAnswer(invocation -> false);
    when(idle.getRootElements()).thenAnswer(invocation -> Set.of());
    assertFalse(processor.process(Set.<TypeElement>of(), idle));

    final RoundEnvironment done = mock(RoundEnvironment.class);
    when(done.processingOver()).thenAnswer(invocation -> true);
    assertFalse(processor.process(Set.<TypeElement>of(), done));
  }

  static ProcessingEnvironment environment() {
    final ProcessingEnvironment environment = mock(ProcessingEnvironment.class);
    when(environment.getElementUtils()).thenAnswer(invocation -> mock(Elements.class));
    when(environment.getTypeUtils()).thenAnswer(invocation -> mock(Types.class));
    when(environment.getFiler()).thenAnswer(invocation -> mock(Filer.class));
    when(environment.getMessager()).thenAnswer(invocation -> mock(Messager.class));
    when(environment.getLocale()).thenAnswer(invocation -> Locale.ROOT);
    when(environment.getOptions()).thenAnswer(invocation -> Map.of());
    when(environment.getSourceVersion()).thenAnswer(invocation -> SourceVersion.latestSupported());
    return environment;
  }
}
