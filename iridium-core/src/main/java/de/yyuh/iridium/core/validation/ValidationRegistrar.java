package de.yyuh.iridium.core.validation;

import de.yyuh.iridium.core.annotation.Internal;

@Internal
public interface ValidationRegistrar {

    void register(final ValidatorRegistry registry);
}
