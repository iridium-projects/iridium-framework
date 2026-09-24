package cc.asylum.iridium.codegen.binding;

import com.io7m.jodist.MethodSpec;
import cc.asylum.iridium.core.annotation.Internal;

import javax.lang.model.element.VariableElement;
import java.util.Optional;

@Internal
public interface ParameterBinder {

  boolean matches(final VariableElement parameter);

  Optional<String> emit(
    final MethodSpec.Builder handle,
    final VariableElement parameter
  );
}
