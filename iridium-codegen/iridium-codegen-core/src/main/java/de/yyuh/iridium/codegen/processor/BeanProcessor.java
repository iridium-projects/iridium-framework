package de.yyuh.iridium.codegen.processor;

import com.io7m.jodist.AnnotationSpec;
import com.io7m.jodist.ClassName;
import com.io7m.jodist.CodeBlock;
import com.io7m.jodist.JavaFile;
import com.io7m.jodist.MethodSpec;
import com.io7m.jodist.TypeName;
import com.io7m.jodist.TypeSpec;
import de.yyuh.iridium.codegen.annotation.Generated;
import de.yyuh.iridium.core.annotation.Internal;
import de.yyuh.iridium.core.bean.Bean;
import de.yyuh.iridium.core.bean.BeanPool;
import de.yyuh.iridium.core.bean.BeanRegistrar;
import de.yyuh.iridium.core.component.Component;
import de.yyuh.iridium.core.hook.OnShutdown;
import de.yyuh.iridium.core.hook.ShutdownHook;
import de.yyuh.iridium.core.inject.Inject;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Internal
@SupportedAnnotationTypes({
    "de.yyuh.iridium.core.component.Component",
    "de.yyuh.iridium.core.bean.Bean",
    "de.yyuh.iridium.core.hook.OnShutdown"
})
public final class BeanProcessor extends AbstractProcessor {

  private static final String GENERATED_CLASS = "BeanRegistrarGenerated";
  private static final String DEFAULT_PACKAGE = "de.yyuh.iridium.gen";
  private static final String AUTHOR = "Iridium";

  private String generatedPackage = DEFAULT_PACKAGE;

  @Override
  public synchronized void init(final javax.annotation.processing.ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
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

    final Set<TypeElement> components = roundEnv.getElementsAnnotatedWith(Component.class).stream()
        .filter(element -> element.getKind() == ElementKind.CLASS)
        .map(TypeElement.class::cast)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    final Set<ExecutableElement> beanMethods = roundEnv.getElementsAnnotatedWith(Bean.class).stream()
        .filter(element -> element.getKind() == ElementKind.METHOD)
        .map(ExecutableElement.class::cast)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    final Set<ExecutableElement> hookMethods = roundEnv.getElementsAnnotatedWith(OnShutdown.class).stream()
        .filter(element -> element.getKind() == ElementKind.METHOD)
        .map(ExecutableElement.class::cast)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    if (components.isEmpty() && beanMethods.isEmpty() && hookMethods.isEmpty()) {
      return false;
    }

    generate(components, beanMethods, hookMethods);
    return false;
  }

  private void generate(final Set<TypeElement> components, final Set<ExecutableElement> beanMethods,
      final Set<ExecutableElement> hookMethods) {
    final Set<String> packages = new LinkedHashSet<>();
    for (final TypeElement component : components) {
      packages.add(packageOf(component));
    }
    for (final ExecutableElement method : beanMethods) {
      packages.add(packageOf(method));
    }
    for (final ExecutableElement method : hookMethods) {
      packages.add(packageOf(method));
    }
    final String basePackage = commonPrefix(packages);
    if (!basePackage.isEmpty()) {
      generatedPackage = basePackage + ".gen";
    }

    final ClassName beanPool = ClassName.get(BeanPool.class);
    final ClassName registrar = ClassName.get(BeanRegistrar.class);

    final MethodSpec.Builder register = MethodSpec.methodBuilder("register")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(beanPool, "pool");

    for (final TypeElement component : components) {
      final ClassName type = ClassName.get(component);
      final ExecutableElement constructor = resolveConstructor(component);
      if (constructor == null) {
        register.addStatement("pool.put($S, new $T())", decapitalize(type.simpleName()), type);
      } else {
        register.addStatement("pool.put($S, new $T($L))",
            decapitalize(type.simpleName()), type, dependencyArgs(constructor));
      }
    }

    for (final ExecutableElement method : beanMethods) {
      final TypeElement enclosing = (TypeElement) method.getEnclosingElement();
      final ClassName enclosingType = ClassName.get(enclosing);
      final String methodName = method.getSimpleName().toString();
      register.addStatement("pool.put($S, new $T().$N($L))",
          methodName, enclosingType, methodName, dependencyArgs(method));
    }

    for (final ExecutableElement method : hookMethods) {
      final TypeElement enclosing = (TypeElement) method.getEnclosingElement();
      final ClassName enclosingType = ClassName.get(enclosing);
      final String methodName = method.getSimpleName().toString();
      final String name = enclosing.getQualifiedName() + "." + methodName;
      final int priority = method.getAnnotation(OnShutdown.class).priority();

      final TypeSpec hookImpl = TypeSpec.anonymousClassBuilder("")
          .addSuperinterface(ClassName.get(ShutdownHook.class))
          .addMethod(MethodSpec.methodBuilder("run")
              .addAnnotation(Override.class)
              .addModifiers(Modifier.PUBLIC)
              .addStatement("new $T().$N()", enclosingType, methodName)
              .build())
          .addMethod(MethodSpec.methodBuilder("priority")
              .addAnnotation(Override.class)
              .addModifiers(Modifier.PUBLIC)
              .returns(TypeName.INT)
              .addStatement("return $L", priority)
              .build())
          .build();

      register.addStatement("pool.put($S, $L)", name, hookImpl);
    }

    final AnnotationSpec generated = AnnotationSpec.builder(ClassName.get(Generated.class))
        .addMember("author", "$S", AUTHOR)
        .addMember("date", "$S", Instant.now().toString())
        .build();

    final TypeSpec typeSpec = TypeSpec.classBuilder(GENERATED_CLASS)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addSuperinterface(registrar)
        .addAnnotation(generated)
        .addMethod(register.build())
        .build();

    final JavaFile javaFile = JavaFile.builder(generatedPackage, typeSpec).build();

    try {
      javaFile.writeTo(processingEnv.getFiler());
      writeServiceFile();
    } catch (final IOException e) {
      throw new RuntimeException("Failed to generate " + generatedPackage + "." + GENERATED_CLASS, e);
    }
  }

  private static String decapitalize(final String name) {
    if (name.isEmpty()) {
      return name;
    }
    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
  }

  private ExecutableElement resolveConstructor(final TypeElement component) {
    final List<ExecutableElement> constructors = new ArrayList<>();
    ExecutableElement annotated = null;
    for (final Element enclosed : component.getEnclosedElements()) {
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

  private CodeBlock dependencyArgs(final ExecutableElement executable) {
    final CodeBlock.Builder args = CodeBlock.builder();
    boolean first = true;
    for (final VariableElement parameter : executable.getParameters()) {
      if (!first) {
        args.add(", ");
      }
      first = false;
      args.add("pool.get($T.class)", TypeName.get(parameter.asType()));
    }
    return args.build();
  }

  private String packageOf(final javax.lang.model.element.Element element) {
    return processingEnv.getElementUtils()
        .getPackageOf(element)
        .getQualifiedName()
        .toString();
  }

  private static String commonPrefix(final Set<String> packages) {
    String prefix = null;
    for (final String pkg : packages) {
      prefix = prefix == null ? pkg : commonPrefix(prefix, pkg);
    }
    return prefix == null ? "" : prefix;
  }

  private static String commonPrefix(final String a, final String b) {
    final String[] left = a.split("\\.");
    final String[] right = b.split("\\.");
    final int length = Math.min(left.length, right.length);
    int i = 0;
    while (i < length && left[i].equals(right[i])) {
      i++;
    }
    return String.join(".", Arrays.copyOf(left, i));
  }

  private void writeServiceFile() throws IOException {
    final javax.annotation.processing.Filer filer = processingEnv.getFiler();
    final var serviceFile = filer.createResource(
        javax.tools.StandardLocation.CLASS_OUTPUT,
        "",
        "META-INF/services/" + BeanRegistrar.class.getName());

    try (final Writer writer = serviceFile.openWriter()) {
      writer.write(generatedPackage + "." + GENERATED_CLASS);
      writer.write(System.lineSeparator());
    }
  }
}
