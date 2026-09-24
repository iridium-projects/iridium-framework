package cc.asylum.iridium.codegen.processor;

import com.io7m.jodist.ClassName;
import com.io7m.jodist.MethodSpec;
import cc.asylum.iridium.codegen.IridiumProcessor;
import cc.asylum.iridium.codegen.support.BindingContext;
import cc.asylum.iridium.codegen.support.Diagnostics;
import cc.asylum.iridium.codegen.support.ModelSupport;
import cc.asylum.iridium.codegen.support.SourceWriter;
import cc.asylum.iridium.codegen.writer.ConfigBinding;
import cc.asylum.iridium.codegen.writer.HookWriter;
import cc.asylum.iridium.config.ConfigurationProperties;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.bean.Bean;
import cc.asylum.iridium.core.bean.BeanPool;
import cc.asylum.iridium.core.bean.BeanRegistrar;
import cc.asylum.iridium.core.component.Component;
import cc.asylum.iridium.core.hook.OnShutdown;
import cc.asylum.iridium.web.controller.RestController;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashSet;
import java.util.Set;

@Internal
public final class BeanProcessor extends IridiumProcessor {

  private static final String GENERATED_CLASS = "BeanRegistrarGenerated";

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(
        Component.class.getCanonicalName(),
        RestController.class.getCanonicalName(),
        Bean.class.getCanonicalName(),
        OnShutdown.class.getCanonicalName(),
        ConfigurationProperties.class.getCanonicalName());
  }

  @Override
  protected void processRound(final RoundEnvironment roundEnv) {
    final Set<TypeElement> components = new LinkedHashSet<>();
    components.addAll(ModelSupport.annotatedTypes(roundEnv, Component.class, ElementKind.CLASS));
    components.addAll(ModelSupport.annotatedTypes(roundEnv, RestController.class, ElementKind.CLASS));
    final Set<TypeElement> configs = new LinkedHashSet<>();
    configs.addAll(ModelSupport.annotatedTypes(roundEnv, ConfigurationProperties.class, ElementKind.CLASS));
    configs.addAll(ModelSupport.annotatedTypes(roundEnv, ConfigurationProperties.class, ElementKind.RECORD));
    final Set<ExecutableElement> beanMethods = ModelSupport.annotatedMethods(roundEnv, Bean.class);
    final Set<ExecutableElement> hookMethods = ModelSupport.annotatedMethods(roundEnv, OnShutdown.class);
    if (components.isEmpty() && configs.isEmpty() && beanMethods.isEmpty() && hookMethods.isEmpty()) {
      return;
    }

    final var roots = ModelSupport.rootTypes(roundEnv, ElementKind.CLASS, ElementKind.RECORD);
    final String pkg = ModelSupport.generatedPackage(elements, roots);

    final MethodSpec.Builder register = MethodSpec.methodBuilder("register")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ClassName.get(BeanPool.class), "pool");

    final BindingContext binding = new BindingContext(types, elements, messager);
    final Set<String> names = new LinkedHashSet<>();
    for (final TypeElement config : configs) {
      if (config.getAnnotation(Component.class) != null) {
        Diagnostics.error(messager, config, "@ConfigurationProperties cannot be combined with @Component");
        continue;
      }
      final String name = ModelSupport.decapitalize(config.getSimpleName().toString());
      if (!names.add(name)) {
        Diagnostics.error(messager, config, "duplicate bean name '" + name + "'");
        continue;
      }
      final var init = ConfigBinding.bindRoot(binding, config);
      if (init != null) {
        register.addStatement("pool.put($S, $L)", name, init);
      }
    }

    for (final TypeElement component : components) {
      final ClassName type = ClassName.get(component);
      final String name = ModelSupport.decapitalize(type.simpleName());
      if (!names.add(name)) {
        Diagnostics.error(messager, component, "duplicate bean name '" + name + "'");
        continue;
      }
      final ExecutableElement constructor = ModelSupport.resolveConstructor(component);
      if (constructor == null) {
        register.addStatement("pool.put($S, new $T())", name, type);
      } else {
        register.addStatement("pool.put($S, new $T($L))",
            name, type, ModelSupport.dependencyArgs(constructor, binding));
      }
    }

    for (final ExecutableElement method : beanMethods) {
      final TypeElement enclosing = (TypeElement) method.getEnclosingElement();
      final String methodName = method.getSimpleName().toString();
      if (!names.add(methodName)) {
        Diagnostics.error(messager, method, "duplicate bean name '" + methodName + "'");
        continue;
      }
      register.addStatement("pool.put($S, new $T().$N($L))",
          methodName,
          ClassName.get(enclosing),
          methodName,
          ModelSupport.dependencyArgs(method, binding)
      );
    }

    for (final ExecutableElement method : hookMethods) {
      final TypeElement enclosing = (TypeElement) method.getEnclosingElement();
      final String methodName = method.getSimpleName().toString();
      final int priority = method.getAnnotation(OnShutdown.class).priority();
      register.addStatement("pool.put($S, $L)",
          enclosing.getQualifiedName() + "." + methodName,
          HookWriter.shutdownHook(
              ClassName.get(enclosing),
              methodName,
              priority,
              ModelSupport.dependencyArgs(ModelSupport.resolveConstructor(enclosing), binding)
          )
      );
    }

    SourceWriter.writeJava(filer, pkg, SourceWriter.generatedType(GENERATED_CLASS)
        .addSuperinterface(ClassName.get(BeanRegistrar.class))
        .addMethod(register.build())
        .build(), roots.toArray(new TypeElement[0]));
    SourceWriter.writeService(filer, BeanRegistrar.class, pkg + "." + GENERATED_CLASS,
        roots.toArray(new TypeElement[0]));
  }
}
