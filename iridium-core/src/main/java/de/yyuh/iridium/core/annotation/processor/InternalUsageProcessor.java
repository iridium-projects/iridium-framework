package de.yyuh.iridium.core.annotation.processor;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import de.yyuh.iridium.core.annotation.Internal;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.Set;

@Internal
@SupportedAnnotationTypes("*")
public final class InternalUsageProcessor extends AbstractProcessor {

  private static final String ALLOWED_PREFIX = "de.yyuh.iridium";

  private Trees trees;

  @Override
  public synchronized void init(final javax.annotation.processing.ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.trees = Trees.instance(processingEnv);
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    if (roundEnv.processingOver()) {
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
    for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {
      final Element annotation = mirror.getAnnotationType().asElement();
      if (annotation instanceof final TypeElement type
          && type.getQualifiedName().contentEquals("de.yyuh.iridium.codegen.annotation.Generated")) {
        return true;
      }
    }
    return false;
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
