package cc.asylum.iridium.core.processor.bean;

import cc.asylum.iridium.codegen.model.Elements;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BeanOrder {

  private BeanOrder() {
  }

  static List<TypeElement> sort(final Set<TypeElement> components) {
    final Map<TypeElement, Set<TypeElement>> deps = new IdentityHashMap<>();
    for (final TypeElement component : components) {
      deps.put(component, dependencies(component, components));
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

  private static Set<TypeElement> dependencies(final TypeElement component, final Set<TypeElement> components) {
    final Set<TypeElement> deps = Collections.newSetFromMap(new IdentityHashMap<>());
    final ExecutableElement constructor = Elements.constructor(component);
    if (constructor == null) {
      return deps;
    }

    for (final VariableElement parameter : constructor.getParameters()) {
      for (final TypeElement candidate : components) {
        if (candidate != component && candidate.asType().equals(parameter.asType())) {
          deps.add(candidate);
        }
      }
    }

    return deps;
  }
}
