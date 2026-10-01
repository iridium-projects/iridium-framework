package cc.asylum.iridium.codegen;

import org.junit.jupiter.api.Test;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

final class ProcessingTest {

  @Test
  void exposesComponents() {
    final Types types = mock(Types.class);
    final Elements elements = mock(Elements.class);
    final Messager messager = mock(Messager.class);
    final Filer filer = mock(Filer.class);
    final RoundEnvironment round = mock(RoundEnvironment.class);
    final Processing processing = new Processing(types, elements, messager, filer, round);
    assertSame(types, processing.types());
    assertSame(elements, processing.elements());
    assertSame(messager, processing.messager());
    assertSame(filer, processing.filer());
    assertSame(round, processing.round());
  }
}
