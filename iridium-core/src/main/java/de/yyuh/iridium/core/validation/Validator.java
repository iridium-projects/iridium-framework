package de.yyuh.iridium.core.validation;

import de.yyuh.iridium.core.annotation.Internal;
import de.yyuh.iridium.core.result.Result;

import java.util.List;

@Internal
public interface Validator<T> {

    Result<T, List<ConstraintViolation>> validate(final T value);
}
