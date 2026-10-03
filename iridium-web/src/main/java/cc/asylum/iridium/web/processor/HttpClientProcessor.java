package cc.asylum.iridium.web.processor;

import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.codegen.Processor;
import cc.asylum.iridium.codegen.Service;
import cc.asylum.iridium.web.processor.client.HttpClientModel;
import cc.asylum.iridium.web.http.HttpClient;

import java.util.Set;

@Service(isolating = true)
public final class HttpClientProcessor extends Processor {

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(HttpClient.class.getCanonicalName());
  }

  @Override
  protected void process(final Processing processing) {
    HttpClientModel.generate(processing);
  }
}
