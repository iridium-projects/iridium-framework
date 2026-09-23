package cc.asylum.iridium.core.validation;

import cc.asylum.iridium.core.annotation.Internal;

@Internal
public interface Escaper<T> {

    T escape(final T value);
}
