package dev.yyuh.iridium.web.undertow;

import de.yyuh.iridium.core.component.Component;
import de.yyuh.iridium.core.result.Result;
import de.yyuh.iridium.core.result.Unit;
import dev.yyuh.iridium.web.router.Request;
import dev.yyuh.iridium.web.router.Router;
import dev.yyuh.iridium.web.webserver.WebServer;
import dev.yyuh.iridium.web.response.Response;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public final class UndertowWebServer implements WebServer {

    private final ResponseWriter responseWriter;
    private Undertow undertow;
    private Router router;

    public UndertowWebServer(final ResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public Result<Unit, Exception> start(final int port, final String host) {
        return Result.of(() -> {
            if (router == null) {
                router = Router.load();
            }

            undertow = Undertow.builder()
                    .addHttpListener(port, host)
                    .setHandler(this::handle)
                    .build();

            undertow.start();
            return Unit.INSTANCE;
        });
    }

    @Override
    public Result<Unit, Exception> registerRoutes() {
        return Result.of(() -> {
            router = Router.load();
            return Unit.INSTANCE;
        });
    }

    private void handle(final HttpServerExchange exchange) {
        try {
            final Request request = toRequest(exchange);
            final Response<?> response = router.dispatch(request);
            write(exchange, response);
        } catch (final Exception e) {
            exchange.setStatusCode(500);
            exchange.getResponseSender().send("Internal Server Error");
        }
    }

    private Request toRequest(final HttpServerExchange exchange) throws Exception {
        final Map<String, List<String>> headers = new LinkedHashMap<>();

        exchange.getRequestHeaders().forEach(header -> {
            final List<String> values = new ArrayList<>();
            header.forEach(values::add);
            headers.put(header.getHeaderName().toString(), values);
        });

        final Map<String, List<String>> query = new LinkedHashMap<>();
        exchange.getQueryParameters().forEach((name, values) ->
                query.put(name, new ArrayList<>(values)));

        final byte[] body = exchange.getInputStream().readAllBytes();

        return new Request(
                exchange.getRequestMethod().toString(),
                exchange.getRequestURI(),
                headers,
                query,
                Map.of(),
                body
        );
    }

    private void write(final HttpServerExchange exchange, final Response<?> response) {
        exchange.setStatusCode(response.status());
        response.headers().forEach((name, value) ->
                exchange.getResponseHeaders().put(new HttpString(name), value));

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, responseWriter.contentType(response));
        exchange.getResponseSender().send(ByteBuffer.wrap(responseWriter.writeBody(response)));
    }
}
