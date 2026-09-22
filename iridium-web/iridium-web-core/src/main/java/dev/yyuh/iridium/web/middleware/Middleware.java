package dev.yyuh.iridium.web.middleware;

import dev.yyuh.iridium.web.router.Request;
import dev.yyuh.iridium.web.response.Response;

@FunctionalInterface
public interface Middleware {

    Response<?> handle(final Request request, final Next next) throws Exception;

    default int priority() {
        return 0;
    }

    interface Next {
        Response<?> proceed() throws Exception;
    }
}
