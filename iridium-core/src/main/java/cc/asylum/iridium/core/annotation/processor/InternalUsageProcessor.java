package cc.asylum.iridium.core.annotation.processor;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import cc.asylum.iridium.core.annotation.Generated;
import cc.asylum.iridium.core.annotation.Internal;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

@Internal
@SupportedAnnotationTypes("*")
public final class InternalUsageProcessor extends AbstractProcessor {

  private static final String ALLOWED_PREFIX = allowedPrefix();

  private Trees trees;

  @Override
  public synchronized void init(final javax.annotation.processing.ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.trees = resolveTrees(processingEnv);
  }

  private static Trees resolveTrees(javax.annotation.processing.ProcessingEnvironment environment) {
    for (int depth = 0; depth < 4 && environment != null; depth++) {
      try {
        return Trees.instance(environment);
      } catch (final IllegalArgumentException notCompilersOwn) {
        environment = delegateOf(environment);
      }
    }
    return null;
  }

  private static javax.annotation.processing.ProcessingEnvironment delegateOf(
      final javax.annotation.processing.ProcessingEnvironment environment) {
    try {
      final Field field = environment.getClass().getDeclaredField("delegate");
      field.setAccessible(true);
      final Object delegate = field.get(environment);
      return delegate instanceof final javax.annotation.processing.ProcessingEnvironment unwrapped
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

      new InternalScanner(root).scan(path, null);
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

  private final class InternalScanner extends TreePathScanner<Void, Void> {

    private final Element root;
    private final Set<String> seen = new HashSet<>();

    InternalScanner(final Element root) {
      this.root = root;
    }

    @Override
    public Void visitIdentifier(final IdentifierTree node, final Void unused) {
      checkType();
      return super.visitIdentifier(node, unused);
    }

    @Override
    public Void visitMemberSelect(final MemberSelectTree node, final Void unused) {
      checkType();
      return super.visitMemberSelect(node, unused);
    }

    private void checkType() {
      final Element element = trees.getElement(getCurrentPath());
      if (element instanceof final TypeElement type
          && type.getAnnotation(Internal.class) != null
          && seen.add(type.getQualifiedName().toString())) {
        processingEnv.getMessager().printMessage(
            Diagnostic.Kind.WARNING,
            "Usage of internal type '" + type.getQualifiedName()
                + "' may change or be removed without notice.",
            root);
      }
    }
  }
}
