package dev.yyuh.iridium.web.middleware;

import dev.yyuh.iridium.web.Request;
import dev.yyuh.iridium.web.response.Response;

@FunctionalInterface
public interface Middleware {

    Response<?> handle(Request request, Next next) throws Exception;

    default int priority() {
        return 0;
    }

    interface Next {
        Response<?> proceed() throws Exception;
    }
}
