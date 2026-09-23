package cc.asylum.iridium.codegen.processor;

import com.io7m.jodist.ClassName;
import com.io7m.jodist.MethodSpec;
import com.io7m.jodist.TypeName;
import com.io7m.jodist.TypeSpec;
import cc.asylum.iridium.codegen.IridiumProcessor;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.bean.Bean;
import cc.asylum.iridium.core.bean.BeanPool;
import cc.asylum.iridium.core.bean.BeanRegistrar;
import cc.asylum.iridium.core.component.Component;
import cc.asylum.iridium.core.hook.OnShutdown;
import cc.asylum.iridium.core.hook.ShutdownHook;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@Internal
public final class BeanProcessor extends IridiumProcessor {

  private static final String GENERATED_CLASS = "BeanRegistrarGenerated";

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(
        Component.class.getCanonicalName(),
        Bean.class.getCanonicalName(),
        OnShutdown.class.getCanonicalName());
  }

  @Override
  protected void processRound(final RoundEnvironment roundEnv) {
    final Set<TypeElement> components = annotatedTypes(roundEnv, Component.class, ElementKind.CLASS);
    final Set<ExecutableElement> beanMethods = annotatedMethods(roundEnv, Bean.class);
    final Set<ExecutableElement> hookMethods = annotatedMethods(roundEnv, OnShutdown.class);
    if (components.isEmpty() && beanMethods.isEmpty() && hookMethods.isEmpty()) {
      return;
    }

    final String pkg = generatedPackage(rootTypes(roundEnv, ElementKind.CLASS, ElementKind.RECORD));

    final MethodSpec.Builder register = MethodSpec.methodBuilder("register")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ClassName.get(BeanPool.class), "pool");

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
      final String methodName = method.getSimpleName().toString();
      register.addStatement("pool.put($S, new $T().$N($L))",
          methodName, ClassName.get(enclosing), methodName, dependencyArgs(method));
    }

    for (final ExecutableElement method : hookMethods) {
      final TypeElement enclosing = (TypeElement) method.getEnclosingElement();
      final ClassName enclosingType = ClassName.get(enclosing);
      final String methodName = method.getSimpleName().toString();
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
      register.addStatement("pool.put($S, $L)", enclosing.getQualifiedName() + "." + methodName, hookImpl);
    }

    writeJava(pkg, generatedType(GENERATED_CLASS)
        .addSuperinterface(ClassName.get(BeanRegistrar.class))
        .addMethod(register.build())
        .build());
    writeService(BeanRegistrar.class, pkg + "." + GENERATED_CLASS);
  }
}
