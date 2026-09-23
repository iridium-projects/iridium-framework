package cc.asylum.iridium.test.middleware;

import cc.asylum.iridium.core.component.Component;
import cc.asylum.iridium.web.middleware.Middleware;
import cc.asylum.iridium.web.response.Response;
import cc.asylum.iridium.web.router.Request;

@Component
public final class LoggingMiddleware implements Middleware {

    @Override
    public Response<?> handle(final Request request, final Next next) throws Exception {
        return next.proceed();
    }
}
