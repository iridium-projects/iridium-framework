package dev.yyuh.iridium.web.webserver;

import de.yyuh.iridium.core.annotation.Internal;
import dev.yyuh.iridium.web.router.Router;

@Internal
public interface WebRegistrar {

    void register(final Router router);
}
