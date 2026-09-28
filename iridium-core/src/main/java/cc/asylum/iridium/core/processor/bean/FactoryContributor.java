package cc.asylum.iridium.core.processor.bean;

import cc.asylum.iridium.codegen.bean.BeanContributor;
import cc.asylum.iridium.codegen.bean.BeanRegistration;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.model.Mirrors;
import cc.asylum.iridium.codegen.Register;
import cc.asylum.iridium.codegen.Service;
import cc.asylum.iridium.core.bean.Bean;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

@Service(order = 10)
public final class FactoryContributor implements BeanContributor {

  @Override
  public void contribute(final BeanRegistration registration) {
    for (final ExecutableElement method : registration.methods(Bean.class)) {
      final TypeElement enclosing = (TypeElement) method.getEnclosingElement();
      if (Mirrors.meta(enclosing, Register.class) == null) {
        registration.error(method, "@Bean methods must be declared on a @Register type");
        continue;
      }

      final String name = method.getSimpleName().toString();
      if (!registration.claim(method, name)) {
        continue;
      }

      registration.put(name, Exprs.invoke(
          registration.pool().invoke("get", Exprs.classLit(enclosing)),
          name,
          registration.args(method)));
    }
  }
}
