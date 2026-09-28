package cc.asylum.iridium.web.undertow;

import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.result.Unit;
import cc.asylum.iridium.web.response.ResponseWriter;
import cc.asylum.iridium.web.router.Request;
import cc.asylum.iridium.web.router.Router;
import cc.asylum.iridium.web.webserver.WebServer;
import cc.asylum.iridium.web.response.Response;
import cc.asylum.iridium.web.undertow.client.UndertowClients;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RequestTooBigException;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Internal
public final class UndertowWebServer implements WebServer {

  private static final Logger LOG = LoggerFactory.getLogger(UndertowWebServer.class);
  private static final long MAX_ENTITY_SIZE = 1_048_576L;

  private final ResponseWriter responseWriter = new ResponseWriter();
  private Undertow undertow;
  private Router router;

  @Override
  public Result<Unit, Exception> start(final int port, final String host) {
    return Result.of(() -> {
      if (router == null) {
        router = Router.load();
      }

      SLF4JBridgeHandler.removeHandlersForRootLogger();
      SLF4JBridgeHandler.install();

      undertow = Undertow.builder()
          .addHttpListener(port, host)
          .setHandler(this::handle)
          .build();

      undertow.start();

      LOG.info("Undertow server started on {}:{}", host, port);
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

  @Override
  public byte[] exchange(
      final String baseUrl,
      final String method,
      final String path,
      final String contentType,
      final byte[] body) {
    return UndertowClients.exchange(baseUrl, method, path, contentType, body);
  }

  @Override
  public void stop() {
    LOG.info("Stopping Undertow server");
    WebServer.super.stop();

    if (undertow != null) {
      undertow.stop();
    }
  }

  private void handle(final HttpServerExchange exchange) {
    if (exchange.isInIoThread() && needsWorker(exchange)) {
      exchange.dispatch(this::handle);
      return;
    }

    try {
      final Request request = toRequest(exchange);
      final Response<?> response = router.dispatch(request);

      write(exchange, response);
    } catch (final RequestTooBigException tooLarge) {
      sendStatus(exchange, 413, "Payload Too Large");
    } catch (final Exception e) {
      LOG.error("Unhandled exception while processing {} {}", exchange.getRequestMethod(), exchange.getRequestURI(), e);
      sendError(exchange);
    }
  }

  private boolean needsWorker(final HttpServerExchange exchange) {
    if (router == null) {
      return true;
    }

    return router.readsBody(
        exchange.getRequestMethod().toString(),
        exchange.getRequestURI());
  }

  private Request toRequest(final HttpServerExchange exchange) {
    return new Request(
        exchange.getRequestMethod().toString(),
        exchange.getRequestURI(),
        Request.split(exchange.getRequestURI()),
        headers(exchange.getRequestHeaders()),
        exchange.getQueryParameters(),
        () -> open(exchange));
  }

  private static Map<String, List<String>> headers(final HeaderMap source) {
    final Map<String, List<String>> headers = new LinkedHashMap<>();
    for (final HeaderValues values : source) {
      headers.put(values.getHeaderName().toString(), List.copyOf(values));
    }

    return headers;
  }

  private static InputStream open(final HttpServerExchange exchange) {
    exchange.setMaxEntitySize(MAX_ENTITY_SIZE);
    exchange.startBlocking();
    return exchange.getInputStream();
  }

  private void write(
      final HttpServerExchange exchange,
      final Response<?> response) {
    exchange.setStatusCode(response.status());
    response.headers().forEach((name, value) -> exchange.getResponseHeaders().put(new HttpString(name), value));
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, responseWriter.contentType(response));

    final byte[] body = responseWriter.writeBody(response);
    exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, body.length);
    exchange.getResponseSender().send(ByteBuffer.wrap(body));
  }

  private void sendError(final HttpServerExchange exchange) {
    sendStatus(exchange, 500, "Internal Server Error");
  }

  private void sendStatus(final HttpServerExchange exchange, final int status, final String message) {
    try {
      exchange.setStatusCode(status);
      exchange.startBlocking();
      exchange.getResponseSender().send(message);
    } catch (final Exception nested) {
      LOG.error("Failed to send error response", nested);
    }
  }
}
