package cc.asylum.iridium.web.router;

import cc.asylum.iridium.web.response.Response;

@FunctionalInterface
public interface Handler {

    Response<?> handle(final Request request) throws Exception;
}
