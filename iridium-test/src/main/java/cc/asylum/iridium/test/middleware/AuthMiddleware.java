package cc.asylum.iridium.test.middleware;

import cc.asylum.iridium.core.component.Component;
import cc.asylum.iridium.web.middleware.Middleware;
import cc.asylum.iridium.web.response.Response;
import cc.asylum.iridium.web.router.Request;

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
