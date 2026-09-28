package cc.asylum.iridium.codegen;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Set;

public abstract class Processor extends AbstractProcessor {

  protected Elements elements;
  protected Types types;
  protected Filer filer;
  protected Messager messager;

  @Override
  public synchronized void init(final ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.elements = processingEnv.getElementUtils();
    this.types = processingEnv.getTypeUtils();
    this.filer = processingEnv.getFiler();
    this.messager = processingEnv.getMessager();
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public final boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    final Processing processing = new Processing(types, elements, messager, filer, roundEnv);
    if (roundEnv.processingOver()) {
      finish(processing);
      return false;
    }
    process(processing);
    return false;
  }

  protected abstract void process(Processing processing);

  protected void finish(final Processing processing) {
  }
}
