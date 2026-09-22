package dev.yyuh.iridium.web;

import dev.yyuh.iridium.web.response.Response;

@FunctionalInterface
public interface Handler {

    Response<?> handle(final Request request) throws Exception;
}
