package cc.asylum.iridium.codegen.support;

import com.io7m.jodist.CodeBlock;
import com.io7m.jodist.TypeName;
import cc.asylum.iridium.codegen.writer.ConfigBinding;
import cc.asylum.iridium.config.Value;
import cc.asylum.iridium.core.annotation.Internal;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Internal
public final class ModelSupport {

  private ModelSupport() {
  }

  public static String rootPackage() {
    final String pkg = Internal.class.getPackageName();
    final int index = pkg.indexOf(".core");
    return index < 0 ? pkg : pkg.substring(0, index);
  }

  public static String packageOf(
      final Elements elements,
      final Element element) {
    return elements.getPackageOf(element).getQualifiedName().toString();
  }

  public static String generatedPackage(
      final Elements elements,
      final Collection<? extends Element> origins) {
    final Set<String> packages = new LinkedHashSet<>();
    for (final Element origin : origins) {
      packages.add(packageOf(elements, origin));
    }
    final String prefix = commonPrefix(packages);
    return prefix.isEmpty() ? rootPackage() + ".gen" : prefix + ".gen";
  }

  public static String commonPrefix(final Set<String> packages) {
    String prefix = null;
    for (final String pkg : packages) {
      prefix = prefix == null ? pkg : commonPrefix(prefix, pkg);
    }
    return prefix == null ? "" : prefix;
  }

  public static String commonPrefix(
      final String a,
      final String b) {
    final String[] left = a.split("\\.");
    final String[] right = b.split("\\.");
    final int length = Math.min(left.length, right.length);
    int i = 0;
    while (i < length && left[i].equals(right[i])) {
      i++;
    }
    return String.join(".", Arrays.copyOf(left, i));
  }

  public static String decapitalize(final String name) {
    if (name.isEmpty()) {
      return name;
    }
    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
  }

  public static List<TypeElement> rootTypes(
      final RoundEnvironment roundEnv,
      final ElementKind... kinds) {
    final Set<ElementKind> allowed = Set.of(kinds);
    final List<TypeElement> result = new ArrayList<>();
    for (final Element root : roundEnv.getRootElements()) {
      if (allowed.contains(root.getKind())) {
        result.add((TypeElement) root);
      }
    }
    return result;
  }

  public static <A extends Annotation> Set<TypeElement> annotatedTypes(
      final RoundEnvironment roundEnv,
      final Class<A> annotation,
      final ElementKind kind) {
    return roundEnv.getElementsAnnotatedWith(annotation).stream()
        .filter(element -> element.getKind() == kind)
        .map(TypeElement.class::cast)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public static <A extends Annotation> Set<ExecutableElement> annotatedMethods(
      final RoundEnvironment roundEnv,
      final Class<A> annotation) {
    return roundEnv.getElementsAnnotatedWith(annotation).stream()
        .filter(element -> element.getKind() == ElementKind.METHOD)
        .map(ExecutableElement.class::cast)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public static ExecutableElement resolveConstructor(final TypeElement type) {
    final List<ExecutableElement> constructors = type.getEnclosedElements()
        .stream()
        .filter(element -> element.getKind() == ElementKind.CONSTRUCTOR)
        .map(ExecutableElement.class::cast)
        .toList();

    return constructors.size() == 1 ? constructors.get(0) : null;
  }

  public static CodeBlock dependencyArgs(
      final ExecutableElement executable,
      final BindingContext binding) {
    return dependencyArgs(executable, CodeBlock.of("pool"), binding);
  }

  public static CodeBlock dependencyArgs(
      final ExecutableElement executable,
      final CodeBlock pool,
      final BindingContext binding) {
    final CodeBlock.Builder args = CodeBlock.builder();
    boolean first = true;
    if (executable != null) {
      for (final VariableElement parameter : executable.getParameters()) {
        if (!first) {
          args.add(", ");
        }
        first = false;
        if (parameter.getAnnotation(Value.class) != null) {
          final CodeBlock bound = ConfigBinding.bindValue(binding, parameter);
          args.add(bound == null ? CodeBlock.of("null") : bound);
        } else {
          args.add("$L.get($T.class)", pool, TypeName.get(parameter.asType()));
        }
      }
    }
    return args.build();
  }
}
