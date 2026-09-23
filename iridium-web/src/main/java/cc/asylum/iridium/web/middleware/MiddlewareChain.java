package cc.asylum.iridium.web.middleware;

import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.web.router.Handler;
import cc.asylum.iridium.web.router.Request;
import cc.asylum.iridium.web.response.Response;

import java.util.List;

@Internal
public final class MiddlewareChain {

    private final List<Middleware> middlewares;
    private final Handler terminal;

    public MiddlewareChain(final List<Middleware> middlewares, final Handler terminal) {
        this.middlewares = List.copyOf(middlewares);
        this.terminal = terminal;
    }

    public Response<?> invoke(final Request request) throws Exception {
        return next(0, request).proceed();
    }

    private Middleware.Next next(final int index, final Request request) {
        if (index == middlewares.size()) {
            return () -> terminal.handle(request);
        }
        final Middleware middleware = middlewares.get(index);
        return () -> middleware.handle(request, next(index + 1, request));
    }
}
