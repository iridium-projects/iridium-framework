package cc.asylum.iridium.data.spec.processor;

import cc.asylum.iridium.codegen.Service;
import cc.asylum.iridium.web.processor.binding.ParameterBinder;
import cc.asylum.iridium.web.processor.binding.ParameterBinderFactory;
import cc.asylum.iridium.web.processor.binding.convert.RequestValues;

import javax.annotation.processing.Messager;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

@Service
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
