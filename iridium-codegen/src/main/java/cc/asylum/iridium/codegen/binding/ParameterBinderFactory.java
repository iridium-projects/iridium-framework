package cc.asylum.iridium.codegen.binding;

import cc.asylum.iridium.core.annotation.Internal;

import javax.annotation.processing.Messager;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

@Internal
public interface ParameterBinderFactory {

  ParameterBinder create(Types types, Elements elements, Messager messager, RequestValues values);
}
