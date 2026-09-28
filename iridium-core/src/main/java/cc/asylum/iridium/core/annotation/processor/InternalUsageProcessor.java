package cc.asylum.iridium.core.annotation.processor;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import cc.asylum.iridium.codegen.Service;
import cc.asylum.iridium.codegen.Generated;
import cc.asylum.iridium.core.annotation.Internal;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.lang.reflect.Field;
import java.util.Set;

@Internal
@Service(isolating = true)
@SupportedAnnotationTypes("*")
public final class InternalUsageProcessor extends AbstractProcessor {

  private static final String ALLOWED_PREFIX = allowedPrefix();

  private Trees trees;

  @Override
  public synchronized void init(final ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.trees = resolveTrees(processingEnv);
  }

  private static Trees resolveTrees(ProcessingEnvironment environment) {
    for (int depth = 0; depth < 4 && environment != null; depth++) {
      try {
        return Trees.instance(environment);
      } catch (final IllegalArgumentException notCompilersOwn) {
        environment = delegateOf(environment);
      }
    }

    return null;
  }

  private static ProcessingEnvironment delegateOf(final ProcessingEnvironment environment) {
    try {
      final Field field = environment.getClass().getDeclaredField("delegate");
      field.setAccessible(true);
      final Object delegate = field.get(environment);
      return delegate instanceof final ProcessingEnvironment unwrapped
        ? unwrapped
        : null;

    } catch (final ReflectiveOperationException | SecurityException notUnwrappable) {
      return null;
    }
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    if (roundEnv.processingOver() || trees == null) {
      return false;
    }

    for (final Element root : roundEnv.getRootElements()) {
      final String pkg = processingEnv.getElementUtils().getPackageOf(root).getQualifiedName().toString();
      if (pkg.startsWith(ALLOWED_PREFIX) || isGenerated(root)) {
        continue;
      }

      final TreePath path = trees.getPath(root);
      if (path == null) {
        continue;
      }

      new InternalScanner(trees, processingEnv, root).scan(path, null);
    }

    return false;
  }

  private boolean isGenerated(final Element element) {
    return element.getAnnotation(Generated.class) != null;
  }

  private static String allowedPrefix() {
    final String pkg = Internal.class.getPackageName();
    final int index = pkg.indexOf(".core");
    return index < 0 ? pkg : pkg.substring(0, index);
  }
}
