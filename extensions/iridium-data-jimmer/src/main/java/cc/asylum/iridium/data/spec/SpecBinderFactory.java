package cc.asylum.iridium.data.spec;

import cc.asylum.iridium.codegen.binding.ParameterBinder;
import cc.asylum.iridium.codegen.binding.ParameterBinderFactory;
import cc.asylum.iridium.codegen.binding.RequestValues;

import javax.annotation.processing.Messager;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public final class SpecBinderFactory implements ParameterBinderFactory {

  @Override
  public ParameterBinder create(
      final Types types,
      final Elements elements,
      final Messager messager,
      final RequestValues values
  ) {
    return new SpecBinding(types, elements, messager, values);
  }
}
