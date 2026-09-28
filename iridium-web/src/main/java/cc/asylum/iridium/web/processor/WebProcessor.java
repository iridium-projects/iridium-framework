package cc.asylum.iridium.web.processor;

import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.codegen.Processor;
import cc.asylum.iridium.codegen.Service;
import cc.asylum.iridium.web.processor.route.WebModel;
import cc.asylum.iridium.core.component.Component;
import cc.asylum.iridium.web.controller.RestController;

import java.util.Set;

@Service
public final class WebProcessor extends Processor {

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(RestController.class.getCanonicalName(), Component.class.getCanonicalName());
  }

  @Override
  protected void process(final Processing processing) {
    WebModel.generate(processing);
  }
}
