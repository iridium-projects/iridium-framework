package cc.asylum.iridium.web.processor.binding;

import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.web.processor.binding.convert.RequestValues;

import javax.annotation.processing.Messager;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

@Internal
public interface ParameterBinderFactory {

  ParameterBinder create(
    final Types types,
    final Elements elements,
    final Messager messager,
    final RequestValues values);
}
