package dev.yyuh.iridium.test.middleware;

import de.yyuh.iridium.core.component.Component;
import dev.yyuh.iridium.web.middleware.Middleware;
import dev.yyuh.iridium.web.response.Response;
import dev.yyuh.iridium.web.router.Request;

@Component
public final class AuthMiddleware implements Middleware {

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public Response<?> handle(final Request request, final Next next) throws Exception {
        if (request.path().startsWith("/admin") && request.header("Authorization").isEmpty()) {
            return Response.status(401).body("Unauthorized");
        }
        return next.proceed();
    }
}
