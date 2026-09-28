package cc.asylum.iridium.core.processor.validation;

import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.codegen.Processor;
import cc.asylum.iridium.codegen.Service;

import java.util.Set;

@Service
public final class ValidationProcessor extends Processor {

  private final ValidationModel model = new ValidationModel();

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return ValidationModel.annotations();
  }

  @Override
  protected void process(final Processing processing) {
    model.round(processing);
  }

  @Override
  protected void finish(final Processing processing) {
    model.finish(processing.filer());
  }
}
