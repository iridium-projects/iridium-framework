package cc.asylum.iridium.core.processor.bean;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.model.TypeRef;
import cc.asylum.iridium.codegen.bean.BeanContributor;
import cc.asylum.iridium.codegen.bean.BeanRegistration;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.model.Mirrors;
import cc.asylum.iridium.codegen.Register;
import cc.asylum.iridium.codegen.Service;
import cc.asylum.iridium.codegen.code.Types;
import cc.asylum.iridium.core.bean.BeanPool;
import cc.asylum.iridium.core.hook.OnShutdown;
import cc.asylum.iridium.core.hook.ShutdownHook;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

@Service(order = 20)
public final class ShutdownContributor implements BeanContributor {

  @Override
  public void contribute(final BeanRegistration registration) {
    for (final ExecutableElement method : registration.methods(OnShutdown.class)) {
      final TypeElement enclosing = (TypeElement) method.getEnclosingElement();
      if (Mirrors.meta(enclosing, Register.class) == null) {
        registration.error(method, "@OnShutdown methods must be declared on a @Register type");
        continue;
      }

      final String owner = registration.beanName(enclosing);
      if (!registration.claimed(owner)) {
        registration.error(enclosing, "no bean registered for '" + owner + "'");
        continue;
      }

      final String methodName = method.getSimpleName().toString();
      final String nested = enclosing.getSimpleName() + capitalize(methodName) + "Hook";
      registration.put(enclosing.getQualifiedName() + "." + methodName, Exprs.new_(TypeRef.of(nested)));
      registration.nest(types -> types.nest(nested, type -> {
        type.implements_(ShutdownHook.class);
        final TypeRef ownerType = Types.of(enclosing);
        type.method("run", run -> {
          run.public_().overrides();
          run.body(body -> body.expr(
              Exprs.invokeStatic(BeanPool.class, "instance")
                  .invoke("get", Exprs.classLit(ownerType))
                  .invoke(methodName)));
        });

        type.method("priority", priority -> {
          priority.public_().overrides();
          priority.returns(int.class);
          priority.body(body -> body.return_(Expr.lit(method.getAnnotation(OnShutdown.class).priority())));
        });
      }));
    }
  }

  private static String capitalize(final String value) {
    if (value.isEmpty()) {
      return value;
    }
    return Character.toUpperCase(value.charAt(0)) + value.substring(1);
  }
}
