package cc.asylum.iridium.core.validation;

import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.result.Result;

import java.util.List;

@Internal
public interface Validator<T> {

  Result<T, List<ConstraintViolation>> validate(final T value);
}
