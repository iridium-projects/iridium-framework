package de.yyuh.iridium.core.bean;

import de.yyuh.iridium.core.annotation.Internal;

@Internal
public interface BeanRegistrar {

    void register(final BeanPool pool);
}
