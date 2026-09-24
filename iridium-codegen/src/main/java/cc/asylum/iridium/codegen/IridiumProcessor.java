package cc.asylum.iridium.codegen;

import cc.asylum.iridium.core.annotation.Internal;

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

@Internal
public abstract class IridiumProcessor extends AbstractProcessor {

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
    if (roundEnv.processingOver()) {
      finish();
      return false;
    }
    processRound(roundEnv);
    return false;
  }

  protected abstract void processRound(RoundEnvironment roundEnv);

  protected void finish() {
  }
}
