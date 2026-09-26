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
import cc.asylum.iridium.config.Value;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.bean.Bean;
import cc.asylum.iridium.core.bean.BeanPool;
import cc.asylum.iridium.core.bean.BeanRegistrar;
import cc.asylum.iridium.core.component.Component;
import cc.asylum.iridium.core.hook.OnShutdown;
import cc.asylum.iridium.web.controller.RestController;
import cc.asylum.iridium.web.http.HttpClient;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Internal
public final class BeanProcessor extends IridiumProcessor {

  private static final String GENERATED_CLASS = "BeanRegistrarGenerated";

  private static List<TypeElement> orderByDependencies(final Set<TypeElement> components) {
    final Map<TypeElement, Set<TypeElement>> deps = new IdentityHashMap<>();
    for (final TypeElement component : components) {
      deps.put(component, constructorDependencies(component, components));
    }
    final List<TypeElement> ordered = new ArrayList<>();
    final Set<TypeElement> placed = Collections.newSetFromMap(new IdentityHashMap<>());
    while (placed.size() < components.size()) {
      boolean progressed = false;
      for (final TypeElement component : components) {
        if (placed.contains(component) || !placed.containsAll(deps.get(component))) {
          continue;
        }
        ordered.add(component);
        placed.add(component);
        progressed = true;
      }
      if (!progressed) {
        components.stream().filter(component -> !placed.contains(component)).forEach(ordered::add);
        break;
      }
    }
    return ordered;
  }

  private static Set<TypeElement> constructorDependencies(
      final TypeElement component,
      final Set<TypeElement> components) {
    final Set<TypeElement> deps = Collections.newSetFromMap(new IdentityHashMap<>());
    final ExecutableElement constructor = ModelSupport.resolveConstructor(component);
    if (constructor == null) {
      return deps;
    }
    for (final VariableElement parameter : constructor.getParameters()) {
      if (parameter.getAnnotation(Value.class) != null) {
        continue;
      }
      for (final TypeElement candidate : components) {
        if (candidate != component && candidate.asType().equals(parameter.asType())) {
          deps.add(candidate);
        }
      }
    }
    return deps;
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(
        Component.class.getCanonicalName(),
        RestController.class.getCanonicalName(),
        Bean.class.getCanonicalName(),
        OnShutdown.class.getCanonicalName(),
        ConfigurationProperties.class.getCanonicalName(),
        HttpClient.class.getCanonicalName());
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
    final Set<TypeElement> clients = ModelSupport.annotatedTypes(roundEnv, HttpClient.class, ElementKind.INTERFACE);
    if (components.isEmpty() && configs.isEmpty() && beanMethods.isEmpty() && hookMethods.isEmpty()
        && clients.isEmpty()) {
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

    for (final TypeElement client : clients) {
      final String impl = client.getQualifiedName() + "Undertow";
      final String name = ModelSupport.decapitalize(client.getSimpleName().toString());
      if (!names.add(name)) {
        Diagnostics.error(messager, client, "duplicate bean name '" + name + "'");
        continue;
      }
      register.addStatement("pool.put($S, new $L())", name, impl);
    }

    final List<TypeElement> ordered = orderByDependencies(components);

    for (final TypeElement component : ordered) {
      final ClassName type = ClassName.get(component);
      final String name = ModelSupport.decapitalize(type.simpleName());
      if (!names.add(name)) {
        Diagnostics.error(messager, component, "duplicate bean name '" + name + "'");
        continue;
      }
      final ExecutableElement constructor = ModelSupport.resolveConstructor(component);
      if (constructor == null) {
        Diagnostics.error(messager, component, "type must have exactly one constructor");
        continue;
      }
      register.addStatement("pool.put($S, new $T($L))",
          name, type, ModelSupport.dependencyArgs(constructor, binding));
    }

    for (final ExecutableElement method : beanMethods) {
      final TypeElement enclosing = (TypeElement) method.getEnclosingElement();
      final String methodName = method.getSimpleName().toString();
      if (!names.add(methodName)) {
        Diagnostics.error(messager, method, "duplicate bean name '" + methodName + "'");
        continue;
      }
      if (enclosing.getAnnotation(Component.class) == null
          && enclosing.getAnnotation(RestController.class) == null) {
        Diagnostics.error(messager, method, "@Bean methods must be declared on a @Component");
        continue;
      }
      final String owner = ModelSupport.decapitalize(enclosing.getSimpleName().toString());
      register.addStatement("pool.put($S, pool.get($T.class).$N($L))",
          methodName,
          ClassName.get(enclosing),
          methodName,
          ModelSupport.dependencyArgs(method, binding));
      if (!names.contains(owner)) {
        Diagnostics.error(messager, enclosing, "no bean registered for '" + owner + "'");
      }
    }

    final List<ExecutableElement> pendingHooks = new ArrayList<>();
    for (final ExecutableElement method : hookMethods) {
      final TypeElement enclosing = (TypeElement) method.getEnclosingElement();
      if (enclosing.getAnnotation(Component.class) == null
          && enclosing.getAnnotation(RestController.class) == null) {
        Diagnostics.error(messager, method, "@OnShutdown methods must be declared on a @Component");
        continue;
      }
      pendingHooks.add(method);
    }

    for (final ExecutableElement method : pendingHooks) {
      final TypeElement enclosing = (TypeElement) method.getEnclosingElement();
      final String methodName = method.getSimpleName().toString();
      final int priority = method.getAnnotation(OnShutdown.class).priority();
      final String owner = ModelSupport.decapitalize(enclosing.getSimpleName().toString());
      if (!names.contains(owner)) {
        Diagnostics.error(messager, enclosing, "no bean registered for '" + owner + "'");
        continue;
      }
      register.addStatement("pool.put($S, $L)",
          enclosing.getQualifiedName() + "." + methodName,
          HookWriter.shutdownHook(ClassName.get(enclosing), methodName, priority));
    }

    final List<Element> origins = new ArrayList<>(roots);
    origins.addAll(clients);
    SourceWriter.writeJava(filer, pkg, SourceWriter.generatedType(GENERATED_CLASS)
        .addSuperinterface(ClassName.get(BeanRegistrar.class))
        .addMethod(register.build())
        .build(), origins.toArray(new Element[0]));
    SourceWriter.writeService(filer, BeanRegistrar.class, pkg + "." + GENERATED_CLASS,
        roots.toArray(new TypeElement[0]));
  }
}
