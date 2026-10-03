package cc.asylum.iridium.codegen;

import org.junit.jupiter.api.Test;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class ProcessorTest {

  @Test
  void initProcessAndFinish() {
    final ProcessingEnvironment environment = mock(ProcessingEnvironment.class);
    final Elements elements = mock(Elements.class);
    final Types types = mock(Types.class);
    final Filer filer = mock(Filer.class);
    final Messager messager = mock(Messager.class);
    when(environment.getElementUtils()).thenAnswer(invocation -> elements);
    when(environment.getTypeUtils()).thenAnswer(invocation -> types);
    when(environment.getFiler()).thenAnswer(invocation -> filer);
    when(environment.getMessager()).thenAnswer(invocation -> messager);
    when(environment.getLocale()).thenAnswer(invocation -> java.util.Locale.ROOT);
    when(environment.getOptions()).thenAnswer(invocation -> java.util.Map.of());
    when(environment.getSourceVersion()).thenAnswer(invocation -> SourceVersion.latestSupported());

    final Probe probe = new Probe();
    probe.init(environment);
    assertSame(elements, probe.elements);
    assertSame(types, probe.types);
    assertSame(filer, probe.filer);
    assertSame(messager, probe.messager);
    assertEquals(SourceVersion.latestSupported(), probe.getSupportedSourceVersion());

    final RoundEnvironment round = mock(RoundEnvironment.class);
    when(round.processingOver()).thenAnswer(invocation -> false);
    assertFalse(probe.process(Set.<TypeElement>of(), round));
    assertEquals(1, probe.processed);

    when(round.processingOver()).thenAnswer(invocation -> true);
    assertFalse(probe.process(Set.<TypeElement>of(), round));
    assertEquals(1, probe.finished);

    new Processor() {
      @Override
      protected void process(final Processing processing) {
      }
    }.finish(new Processing(types, elements, messager, filer, round));
  }

  private static class Probe extends Processor {

    private int processed;
    private int finished;

    @Override
    protected void process(final Processing processing) {
      processed++;
    }

    @Override
    protected void finish(final Processing processing) {
      finished++;
    }
  }
}
