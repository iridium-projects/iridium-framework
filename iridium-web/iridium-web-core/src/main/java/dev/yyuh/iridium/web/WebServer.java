package dev.yyuh.iridium.web;

import de.yyuh.iridium.core.bean.BeanPool;
import de.yyuh.iridium.core.hook.ShutdownHook;
import de.yyuh.iridium.core.result.Result;

import java.util.Comparator;

public interface WebServer {

    Result<String, String> start(final int port);

    default void stop() {
        BeanPool.instance().all(ShutdownHook.class).stream()
                .sorted(Comparator.comparingInt(ShutdownHook::priority))
                .forEach(ShutdownHook::run);
    }

    Result<String, String> registerRoutes();
}
