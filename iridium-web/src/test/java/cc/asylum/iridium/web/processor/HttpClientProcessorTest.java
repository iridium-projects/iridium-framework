package cc.asylum.iridium.web.processor;

import cc.asylum.iridium.web.http.HttpClient;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class HttpClientProcessorTest {

  @Test
  void processesAndFinishes() {
    final HttpClientProcessor processor = new HttpClientProcessor();
    processor.init(WebProcessorTest.environment());
    assertEquals(Set.of(HttpClient.class.getCanonicalName()), processor.getSupportedAnnotationTypes());

    final RoundEnvironment idle = mock(RoundEnvironment.class);
    when(idle.processingOver()).thenAnswer(invocation -> false);
    when(idle.getElementsAnnotatedWith(HttpClient.class)).thenAnswer(invocation -> Set.of());
    assertFalse(processor.process(Set.<TypeElement>of(), idle));

    final RoundEnvironment done = mock(RoundEnvironment.class);
    when(done.processingOver()).thenAnswer(invocation -> true);
    assertFalse(processor.process(Set.<TypeElement>of(), done));
  }
}
