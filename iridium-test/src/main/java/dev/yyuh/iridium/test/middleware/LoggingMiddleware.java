package dev.yyuh.iridium.test.middleware;

import de.yyuh.iridium.core.component.Component;
import dev.yyuh.iridium.web.middleware.Middleware;
import dev.yyuh.iridium.web.response.Response;
import dev.yyuh.iridium.web.router.Request;

@Component
public final class LoggingMiddleware implements Middleware {

    @Override
    public Response<?> handle(final Request request, final Next next) throws Exception {
        return next.proceed();
    }
}
