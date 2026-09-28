package cc.asylum.iridium.core.processor.bean;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.codegen.Processor;
import cc.asylum.iridium.codegen.Register;
import cc.asylum.iridium.codegen.Service;
import cc.asylum.iridium.codegen.bean.BeanContributor;
import cc.asylum.iridium.codegen.bean.BeanRegistration;
import cc.asylum.iridium.codegen.model.Elements;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.model.Mirrors;
import cc.asylum.iridium.codegen.code.Types;
import cc.asylum.iridium.codegen.write.Registrar;
import cc.asylum.iridium.core.bean.BeanPool;
import cc.asylum.iridium.core.bean.BeanRegistrar;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

@Service
public final class BeanProcessor extends Processor {

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of("*");
  }

  @Override
  protected void process(final Processing processing) {
    final var roots = Elements.rootTypes(processing.round(), ElementKind.CLASS, ElementKind.RECORD, ElementKind.INTERFACE);
    final Registrar registrar = Registrar.of("BeanRegistrarGenerated", BeanRegistrar.class, BeanPool.class, "pool");
    final BeanRegistration registration = new BeanRegistration(processing, registrar);

    final List<BeanContributor> contributors = contributors();
    contributors.stream().filter(contributor -> order(contributor) < 0).forEach(contributor -> contributor.contribute(registration));
    registerAnnotated(registration);
    contributors.stream().filter(contributor -> order(contributor) >= 0).forEach(contributor -> contributor.contribute(registration));

    if (registrar.empty()) {
      return;
    }

    registrar.write(processing.filer(), Elements.generatedPackage(processing.elements(), roots), roots.toArray(new Element[0]));
  }

  private void registerAnnotated(final BeanRegistration registration) {
    final Set<TypeElement> constructed = new LinkedHashSet<>();
    final List<TypeElement> suffixed = new ArrayList<>();

    for (final Element element : registration.processing.round().getRootElements()) {
      if (!(element instanceof final TypeElement type)) {
        continue;
      }

      final Register register = Mirrors.meta(type, Register.class);
      if (register == null) {
        continue;
      }

      if (register.suffix().isEmpty()) {
        constructed.add(type);
      } else {
        suffixed.add(type);
      }

    }
    for (final TypeElement type : BeanOrder.sort(constructed)) {
      put(registration, type, registration.construct(type));
    }

    for (final TypeElement type : suffixed) {
      put(registration, type, Exprs.new_(Types.of(type.getQualifiedName() + Mirrors.meta(type, Register.class).suffix())));
    }
  }

  private void put(final BeanRegistration registration, final TypeElement type, final Expr value) {
    final String name = registration.beanName(type);

    if (registration.claim(type, name)) {
      registration.put(name, value);
    }
  }

  private static List<BeanContributor> contributors() {
    return ServiceLoader.load(BeanContributor.class, BeanContributor.class.getClassLoader()).stream().map(ServiceLoader.Provider::get).sorted(Comparator.comparingInt(BeanProcessor::order)).toList();
  }

  private static int order(final BeanContributor contributor) {
    final Service service = contributor.getClass().getAnnotation(Service.class);
    return service == null ? 0 : service.order();
  }
}
