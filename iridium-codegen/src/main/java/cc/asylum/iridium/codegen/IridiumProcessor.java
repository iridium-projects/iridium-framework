package cc.asylum.iridium.codegen;

import com.io7m.jodist.AnnotationSpec;
import com.io7m.jodist.ClassName;
import com.io7m.jodist.CodeBlock;
import com.io7m.jodist.JavaFile;
import com.io7m.jodist.TypeName;
import com.io7m.jodist.TypeSpec;
import cc.asylum.iridium.core.annotation.Generated;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.inject.Inject;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Internal
public abstract class IridiumProcessor extends AbstractProcessor {

  protected static final String DEFAULT_PACKAGE = rootPackage() + ".gen";
  protected static final String AUTHOR = "Iridium";

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

  protected AnnotationSpec generatedAnnotation() {
    return AnnotationSpec.builder(ClassName.get(Generated.class))
        .addMember("author", "$S", AUTHOR)
        .addMember("date", "$S", Instant.now().toString())
        .build();
  }

  protected TypeSpec.Builder generatedType(final String name) {
    return TypeSpec.classBuilder(name)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addAnnotation(generatedAnnotation());
  }

  protected void writeJava(final String pkg, final TypeSpec spec) {
    try {
      JavaFile.builder(pkg, spec).build().writeTo(filer);
    } catch (final IOException e) {
      throw new RuntimeException("Failed to generate " + pkg + "." + spec.name, e);
    }
  }

  protected void writeService(final Class<?> service, final String implFqcn) {
    writeService(service.getName(), implFqcn);
  }

  protected void writeService(final String serviceName, final String implFqcn) {
    try {
      final var file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + serviceName);
      try (final Writer writer = file.openWriter()) {
        writer.write(implFqcn);
        writer.write(System.lineSeparator());
      }
    } catch (final IOException e) {
      throw new RuntimeException("Failed to write service file for " + serviceName, e);
    }
  }

  protected String packageOf(final Element element) {
    return elements.getPackageOf(element).getQualifiedName().toString();
  }

  protected String generatedPackage(final Collection<? extends Element> origins) {
    final Set<String> packages = new LinkedHashSet<>();
    for (final Element origin : origins) {
      packages.add(packageOf(origin));
    }
    final String prefix = commonPrefix(packages);
    return prefix.isEmpty() ? DEFAULT_PACKAGE : prefix + ".gen";
  }

  protected static String commonPrefix(final Set<String> packages) {
    String prefix = null;
    for (final String pkg : packages) {
      prefix = prefix == null ? pkg : commonPrefix(prefix, pkg);
    }
    return prefix == null ? "" : prefix;
  }

  protected static String commonPrefix(final String a, final String b) {
    final String[] left = a.split("\\.");
    final String[] right = b.split("\\.");
    final int length = Math.min(left.length, right.length);
    int i = 0;
    while (i < length && left[i].equals(right[i])) {
      i++;
    }
    return String.join(".", Arrays.copyOf(left, i));
  }

  protected static String decapitalize(final String name) {
    if (name.isEmpty()) {
      return name;
    }
    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
  }

  protected List<TypeElement> rootTypes(final RoundEnvironment roundEnv, final ElementKind... kinds) {
    final Set<ElementKind> allowed = Set.of(kinds);
    final List<TypeElement> result = new ArrayList<>();
    for (final Element root : roundEnv.getRootElements()) {
      if (allowed.contains(root.getKind())) {
        result.add((TypeElement) root);
      }
    }
    return result;
  }

  protected <A extends Annotation> Set<TypeElement> annotatedTypes(
      final RoundEnvironment roundEnv, final Class<A> annotation, final ElementKind kind) {
    return roundEnv.getElementsAnnotatedWith(annotation).stream()
        .filter(element -> element.getKind() == kind)
        .map(TypeElement.class::cast)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  protected <A extends Annotation> Set<ExecutableElement> annotatedMethods(
      final RoundEnvironment roundEnv, final Class<A> annotation) {
    return roundEnv.getElementsAnnotatedWith(annotation).stream()
        .filter(element -> element.getKind() == ElementKind.METHOD)
        .map(ExecutableElement.class::cast)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  protected ExecutableElement resolveConstructor(final TypeElement type) {
    final List<ExecutableElement> constructors = new ArrayList<>();
    ExecutableElement annotated = null;
    for (final Element enclosed : type.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
        final ExecutableElement constructor = (ExecutableElement) enclosed;
        constructors.add(constructor);
        if (constructor.getAnnotation(Inject.class) != null) {
          annotated = constructor;
        }
      }
    }
    if (annotated != null) {
      return annotated;
    }
    return constructors.size() == 1 ? constructors.get(0) : null;
  }

  protected CodeBlock dependencyArgs(final ExecutableElement executable) {
    return dependencyArgs(executable, CodeBlock.of("pool"));
  }

  protected CodeBlock dependencyArgs(final ExecutableElement executable, final CodeBlock pool) {
    final CodeBlock.Builder args = CodeBlock.builder();
    boolean first = true;
    if (executable != null) {
      for (final VariableElement parameter : executable.getParameters()) {
        if (!first) {
          args.add(", ");
        }
        first = false;
        args.add("$L.get($T.class)", pool, TypeName.get(parameter.asType()));
      }
    }
    return args.build();
  }

  protected boolean isAssignable(final TypeMirror type, final Class<?> target) {
    return isAssignable(type, target.getCanonicalName());
  }

  protected boolean isAssignable(final TypeMirror type, final String fqcn) {
    final TypeElement target = elements.getTypeElement(fqcn);
    return target != null && types.isAssignable(type, target.asType());
  }

  protected boolean isSameType(final TypeMirror type, final Class<?> target) {
    return isSameType(type, target.getCanonicalName());
  }

  protected boolean isSameType(final TypeMirror type, final String fqcn) {
    final TypeElement target = elements.getTypeElement(fqcn);
    return target != null && types.isSameType(type, target.asType());
  }

  protected static String rootPackage() {
    final String pkg = Internal.class.getPackageName();
    final int index = pkg.indexOf(".core");
    return index < 0 ? pkg : pkg.substring(0, index);
  }

  protected <A extends Annotation> A metaAnnotation(final Element element, final Class<A> meta) {
    for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {
      final A found = mirror.getAnnotationType().asElement().getAnnotation(meta);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  protected AnnotationMirror annotationWithMeta(final Element element, final Class<? extends Annotation> meta) {
    for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {
      if (mirror.getAnnotationType().asElement().getAnnotation(meta) != null) {
        return mirror;
      }
    }
    return null;
  }

  protected boolean hasMeta(final Element element, final Class<? extends Annotation> meta) {
    return metaAnnotation(element, meta) != null;
  }

  protected AnnotationMirror mirrorOf(final Element element, final Class<? extends Annotation> type) {
    final String name = type.getCanonicalName();
    for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {
      final Element annotationElement = mirror.getAnnotationType().asElement();
      if (annotationElement instanceof final TypeElement typeElement
          && typeElement.getQualifiedName().contentEquals(name)) {
        return mirror;
      }
    }
    return null;
  }

  protected String stringMember(final AnnotationMirror mirror, final String member, final String fallback) {
    final Object value = memberValue(mirror, member);
    return value == null ? fallback : value.toString();
  }

  protected boolean booleanMember(final AnnotationMirror mirror, final String member, final boolean fallback) {
    final Object value = memberValue(mirror, member);
    return value instanceof final Boolean bool ? bool : fallback;
  }

  protected int intMember(final AnnotationMirror mirror, final String member, final int fallback) {
    final Object value = memberValue(mirror, member);
    return value instanceof final Integer integer ? integer : fallback;
  }

  protected long longMember(final AnnotationMirror mirror, final String member, final long fallback) {
    final Object value = memberValue(mirror, member);
    return value instanceof final Number number ? number.longValue() : fallback;
  }

  protected Object memberValue(final AnnotationMirror mirror, final String member) {
    if (mirror == null) {
      return null;
    }
    for (final Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues()
        .entrySet()) {
      if (entry.getKey().getSimpleName().contentEquals(member)) {
        return entry.getValue().getValue();
      }
    }
    return null;
  }

  protected String stringValue(final Element element, final Class<? extends Annotation> type,
      final String member, final String fallback) {
    return stringMember(mirrorOf(element, type), member, fallback);
  }

  protected boolean booleanValue(final Element element, final Class<? extends Annotation> type,
      final String member, final boolean fallback) {
    return booleanMember(mirrorOf(element, type), member, fallback);
  }

  protected void error(final Element element, final String message) {
    messager.printMessage(Diagnostic.Kind.ERROR, message, element);
  }
}
