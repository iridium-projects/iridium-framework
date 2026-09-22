package dev.yyuh.iridium.web.webserver;

import de.yyuh.iridium.core.bean.BeanPool;
import de.yyuh.iridium.core.hook.ShutdownHook;
import de.yyuh.iridium.core.result.Result;
import de.yyuh.iridium.core.result.Unit;

import java.util.Comparator;

public interface WebServer {

    Result<Unit, Exception> start(final int port, final String host);

    default void stop() {
        BeanPool.instance().all(ShutdownHook.class).stream()
                .sorted(Comparator.comparingInt(ShutdownHook::priority))
                .forEach(ShutdownHook::run);
    }

    Result<Unit, Exception> registerRoutes();
}
