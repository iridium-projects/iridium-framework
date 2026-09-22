package de.yyuh.iridium.codegen.processor;

import com.io7m.jodist.AnnotationSpec;
import com.io7m.jodist.ClassName;
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

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Internal
@SupportedAnnotationTypes({
        "de.yyuh.iridium.core.component.Component",
        "de.yyuh.iridium.core.bean.Bean",
        "de.yyuh.iridium.core.hook.OnShutdown"
})
public final class BeanProcessor extends AbstractProcessor {

    private static final String GENERATED_PACKAGE = "de.yyuh.iridium.gen";
    private static final String GENERATED_CLASS = "BeanRegistrarGenerated";
    private static final String AUTHOR = "Iridium";

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
        final ClassName beanPool = ClassName.get(BeanPool.class);
        final ClassName registrar = ClassName.get(BeanRegistrar.class);

        final MethodSpec.Builder register = MethodSpec.methodBuilder("register")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(beanPool, "pool");

        for (final TypeElement component : components) {
            final ClassName type = ClassName.get(component);
            register.addStatement("pool.put($S, new $T())", decapitalize(type.simpleName()), type);
        }

        for (final ExecutableElement method : beanMethods) {
            final TypeElement enclosing = (TypeElement) method.getEnclosingElement();
            final ClassName enclosingType = ClassName.get(enclosing);
            final String methodName = method.getSimpleName().toString();
            register.addStatement("pool.put($S, new $T().$N())", methodName, enclosingType, methodName);
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

        final JavaFile javaFile = JavaFile.builder(GENERATED_PACKAGE, typeSpec).build();

        try {
            javaFile.writeTo(processingEnv.getFiler());
            writeServiceFile();
        } catch (final IOException e) {
            throw new RuntimeException("Failed to generate " + GENERATED_CLASS, e);
        }
    }

    private static String decapitalize(final String name) {
        if (name.isEmpty()) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private void writeServiceFile() throws IOException {
        final javax.annotation.processing.Filer filer = processingEnv.getFiler();
        final var serviceFile = filer.createResource(
                javax.tools.StandardLocation.CLASS_OUTPUT,
                "",
                "META-INF/services/" + BeanRegistrar.class.getName()
        );

        try (final Writer writer = serviceFile.openWriter()) {
            writer.write(GENERATED_PACKAGE + "." + GENERATED_CLASS);
            writer.write(System.lineSeparator());
        }
    }
}
