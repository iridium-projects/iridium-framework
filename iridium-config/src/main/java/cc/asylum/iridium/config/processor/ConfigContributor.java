package cc.asylum.iridium.config.processor;

import cc.asylum.iridium.codegen.bean.BeanContributor;
import cc.asylum.iridium.codegen.bean.BeanRegistration;
import cc.asylum.iridium.codegen.Service;
import cc.asylum.iridium.config.ConfigurationProperties;
import cc.asylum.iridium.core.component.Component;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashSet;
import java.util.Set;

@Service(order = -10)
public final class ConfigContributor implements BeanContributor {

  @Override
  public void contribute(final BeanRegistration registration) {
    final Set<TypeElement> configs = new LinkedHashSet<>();
    configs.addAll(registration.types(ConfigurationProperties.class, ElementKind.CLASS));
    configs.addAll(registration.types(ConfigurationProperties.class, ElementKind.RECORD));
    for (final TypeElement config : configs) {
      if (config.getAnnotation(Component.class) != null) {
        registration.error(config, "@ConfigurationProperties cannot be combined with @Component");
        continue;
      }
      final String name = registration.beanName(config);
      if (!registration.claim(config, name)) {
        continue;
      }
      final var init = ConfigBinding.bindRoot(registration.processing, config);
      if (init != null) {
        registration.put(name, init);
      }
    }
  }
}
