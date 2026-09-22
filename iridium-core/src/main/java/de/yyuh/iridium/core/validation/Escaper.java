package de.yyuh.iridium.core.validation;

import de.yyuh.iridium.core.annotation.Internal;

@Internal
public interface Escaper<T> {

    T escape(final T value);
}
