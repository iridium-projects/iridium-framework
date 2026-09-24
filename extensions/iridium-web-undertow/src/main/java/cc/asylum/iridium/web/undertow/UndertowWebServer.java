package cc.asylum.iridium.web.undertow;

import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.result.Unit;
import cc.asylum.iridium.web.response.ResponseWriter;
import cc.asylum.iridium.web.router.Request;
import cc.asylum.iridium.web.router.Router;
import cc.asylum.iridium.web.webserver.WebServer;
import cc.asylum.iridium.web.response.Response;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Internal
public final class UndertowWebServer implements WebServer {

  private static final Logger LOG = LoggerFactory.getLogger(UndertowWebServer.class);

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
          .setServerOption(UndertowOptions.MAX_ENTITY_SIZE, 10_000_000L)
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
  public void stop() {
    LOG.info("Stopping Undertow server");
    WebServer.super.stop();

    if (undertow != null) {
      undertow.stop();
    }
  }

  private void handle(final HttpServerExchange exchange) {
    if (exchange.isInIoThread()) {
      exchange.dispatch(this::handle);
      return;
    }

    final long start = System.nanoTime();

    try {
      final Request request = toRequest(exchange);
      final Response<?> response = router.dispatch(request);

      write(exchange, response);

      LOG.debug("{} {} -> {} ({} ms)",
          request.method(),
          request.path(),
          response.status(),
          elapsedMillis(start));
    } catch (final Exception e) {
      LOG.error("Unhandled exception while processing {} {}", exchange.getRequestMethod(), exchange.getRequestURI(), e);
      sendError(exchange);
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
    exchange.getQueryParameters().forEach((name, values) -> query.put(name, new ArrayList<>(values)));

    exchange.startBlocking();
    final byte[] body = exchange.getInputStream().readAllBytes();

    return new Request(
        exchange.getRequestMethod().toString(),
        exchange.getRequestURI(),
        headers,
        query,
        Map.of(),
        body);
  }

  private void write(
      final HttpServerExchange exchange,
      final Response<?> response) throws Exception {
    exchange.setStatusCode(response.status());
    response.headers().forEach((name, value) -> exchange.getResponseHeaders().put(new HttpString(name), value));

    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, responseWriter.contentType(response));
    sendBody(exchange, responseWriter.writeBody(response));
  }

  private void sendError(final HttpServerExchange exchange) {
    try {
      exchange.setStatusCode(500);
      sendBody(exchange, "Internal Server Error".getBytes(StandardCharsets.UTF_8));
    } catch (final Exception nested) {
      LOG.error("Failed to send error response", nested);
    }
  }

  private void sendBody(
      final HttpServerExchange exchange,
      final byte[] body) throws Exception {
    if (exchange.isBlocking()) {
      exchange.getOutputStream().write(body);
      return;
    }

    exchange.getResponseSender().send(ByteBuffer.wrap(body));
  }

  private static long elapsedMillis(final long startNanos) {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
  }
}
