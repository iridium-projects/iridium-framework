package cc.asylum.iridium.core.annotation.processor;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import cc.asylum.iridium.core.annotation.Internal;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.Set;

final class InternalScanner extends TreePathScanner<Void, Void> {

  private final Trees trees;
  private final ProcessingEnvironment processingEnv;
  private final Element root;
  private final Set<String> seen = new HashSet<>();

  public InternalScanner(
    final Trees trees,
    final ProcessingEnvironment processingEnv,
    final Element root
  ) {
    this.trees = trees;
    this.processingEnv = processingEnv;
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
    if (element instanceof final TypeElement type && type.getAnnotation(Internal.class) != null && seen.add(type.getQualifiedName().toString())) {
      processingEnv.getMessager().printMessage(
        Diagnostic.Kind.WARNING,
        "Usage of internal type '%s' may change or be removed without notice.".formatted(type.getQualifiedName()),
        root);
    }
  }
}
