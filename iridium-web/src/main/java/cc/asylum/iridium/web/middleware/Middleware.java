package cc.asylum.iridium.web.middleware;

import cc.asylum.iridium.web.router.Request;
import cc.asylum.iridium.web.response.Response;

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
