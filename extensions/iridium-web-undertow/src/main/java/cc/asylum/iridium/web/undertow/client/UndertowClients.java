package cc.asylum.iridium.web.undertow.client;

import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.util.Strings;
import io.avaje.jsonb.Jsonb;
import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.client.UndertowClient;
import io.undertow.connector.ByteBufferPool;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.Protocols;
import io.undertow.util.StringReadChannelListener;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.util.concurrent.CompletableFuture;

@Internal
public final class UndertowClients {

  private static final UndertowClient CLIENT = UndertowClient.getInstance();
  private static final Jsonb JSONB = Jsonb.instance();
  private static final ByteBufferPool BUFFERS = new DefaultByteBufferPool(true, 16 * 1024, -1, 4);
  private static final OptionMap OPTIONS = OptionMap.builder()
      .set(Options.WORKER_IO_THREADS, 2)
      .set(Options.TCP_NODELAY, true)
      .set(Options.KEEP_ALIVE, true)
      .getMap();

  private static volatile XnioWorker worker;
  private static volatile UndertowXnioSsl ssl;

  private UndertowClients() {
  }

  public static byte[] exchange(
      final String baseUrl,
      final String method,
      final String path,
      final String contentType,
      final byte[] body) {
    final URI uri = URI.create(baseUrl);
    final ClientConnection connection = connect(uri);
    try {
      return send(connection, uri, method, path, contentType, body);
    } finally {
      close(connection);
    }
  }

  public static byte[] bytes(final Object body) {
    return switch (body) {
      case null -> null;
      case final byte[] bytes -> bytes;
      case final String string -> Strings.utf8(string);
      default -> JSONB.toJsonBytes(body);
    };
  }

  public static String text(final byte[] body) {
    return body == null || body.length == 0 ? null : Strings.utf8(body);
  }

  public static <T> T json(final byte[] body, final Class<T> type) {
    if (body == null || body.length == 0) {
      return null;
    }
    return JSONB.type(type).fromJson(body);
  }

  public static String query(final Object value) {
    if (value == null) {
      return "";
    }
    return URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8);
  }

  public static String contentTypeOf(final String declared, final Object body) {
    if (declared != null && !declared.isEmpty()) {
      return declared;
    }
    return switch (body) {
      case null -> null;
      case final byte[] _ -> "application/octet-stream";
      case final String _ -> "text/plain; charset=utf-8";
      default -> "application/json";
    };
  }

  private static ClientConnection connect(final URI uri) {
    try {
      if ("https".equalsIgnoreCase(uri.getScheme())) {
        return CLIENT.connect(uri, worker(), ssl(), BUFFERS, OPTIONS).get();
      }
      return CLIENT.connect(uri, worker(), BUFFERS, OPTIONS).get();
    } catch (final IOException e) {
      throw new IllegalStateException("Failed to connect to " + uri, e);
    }
  }

  private static byte[] send(
      final ClientConnection connection,
      final URI uri,
      final String method,
      final String path,
      final String contentType,
      final byte[] body) {
    final CompletableFuture<byte[]> result = new CompletableFuture<>();
    final ClientRequest request = request(uri, method, path, contentType, body);
    connection.sendRequest(request, new ClientCallback<>() {
      @Override
      public void completed(final ClientExchange exchange) {
        if (body != null && body.length > 0) {
          write(exchange.getRequestChannel(), body, result);
        }
        exchange.setResponseListener(new ClientCallback<>() {
          @Override
          public void completed(final ClientExchange responseExchange) {
            final ClientResponse response = responseExchange.getResponse();
            new StringReadChannelListener(connection.getBufferPool()) {
              @Override
              protected void stringDone(final String value) {
                final int status = response.getResponseCode();
                if (status >= 400) {
                  result.completeExceptionally(new IllegalStateException(
                      status + " " + response.getStatus() + (value == null || value.isEmpty() ? "" : ": " + value)));
                  return;
                }
                result.complete(value == null ? new byte[0] : Strings.utf8(value));
              }

              @Override
              protected void error(final IOException e) {
                result.completeExceptionally(e);
              }
            }.setup(responseExchange.getResponseChannel());
          }

          @Override
          public void failed(final IOException e) {
            result.completeExceptionally(e);
          }
        });
      }

      @Override
      public void failed(final IOException e) {
        result.completeExceptionally(e);
      }
    });
    try {
      return result.join();
    } catch (final RuntimeException e) {
      final Throwable cause = e.getCause() == null ? e : e.getCause();
      if (cause instanceof RuntimeException runtime) {
        throw runtime;
      }
      throw new IllegalStateException(cause);
    }
  }

  private static ClientRequest request(
      final URI uri,
      final String method,
      final String path,
      final String contentType,
      final byte[] body) {
    final ClientRequest request = new ClientRequest()
        .setMethod(method(method))
        .setPath(path == null || path.isEmpty() ? "/" : path)
        .setProtocol(Protocols.HTTP_1_1);
    request.getRequestHeaders().put(Headers.HOST, host(uri));
    if (contentType != null && !contentType.isEmpty()) {
      request.getRequestHeaders().put(Headers.CONTENT_TYPE, contentType);
    }
    if (body != null && body.length > 0) {
      request.getRequestHeaders().put(Headers.CONTENT_LENGTH, body.length);
    }
    return request;
  }

  private static HttpString method(final String method) {
    return switch (method) {
      case "DELETE" -> Methods.DELETE;
      case "HEAD" -> Methods.HEAD;
      case "OPTIONS" -> Methods.OPTIONS;
      case "PATCH" -> Methods.PATCH;
      case "POST" -> Methods.POST;
      case "PUT" -> Methods.PUT;
      default -> Methods.GET;
    };
  }

  private static String host(final URI uri) {
    final int port = uri.getPort();
    if (port < 0) {
      return uri.getHost();
    }
    return uri.getHost() + ":" + port;
  }

  private static XnioWorker worker() {
    final XnioWorker current = worker;
    if (current != null) {
      return current;
    }
    synchronized (UndertowClients.class) {
      if (worker == null) {
        try {
          worker = Xnio.getInstance().createWorker(OPTIONS);
        } catch (final IOException e) {
          throw new IllegalStateException("Failed to start Undertow client worker", e);
        }
      }
      return worker;
    }
  }

  private static UndertowXnioSsl ssl() {
    final UndertowXnioSsl current = ssl;
    if (current != null) {
      return current;
    }
    synchronized (UndertowClients.class) {
      if (ssl == null) {
        try {
          ssl = new UndertowXnioSsl(Xnio.getInstance(), OptionMap.EMPTY, SSLContext.getDefault());
        } catch (final Exception e) {
          throw new IllegalStateException("Failed to initialize Undertow client SSL", e);
        }
      }
      return ssl;
    }
  }

  private static void write(
      final StreamSinkChannel channel,
      final byte[] body,
      final CompletableFuture<byte[]> result) {
    final ByteBuffer buffer = ByteBuffer.wrap(body);
    try {
      while (buffer.hasRemaining()) {
        if (channel.write(buffer) == 0) {
          channel.getWriteSetter().set(ignored -> write(channel, buffer, result));
          channel.resumeWrites();
          return;
        }
      }
      channel.shutdownWrites();
    } catch (final IOException e) {
      result.completeExceptionally(e);
    }
  }

  private static void write(
      final StreamSinkChannel channel,
      final ByteBuffer buffer,
      final CompletableFuture<byte[]> result) {
    try {
      while (buffer.hasRemaining()) {
        if (channel.write(buffer) == 0) {
          return;
        }
      }
      channel.shutdownWrites();
      channel.getWriteSetter().set(null);
      channel.suspendWrites();
    } catch (final IOException e) {
      result.completeExceptionally(e);
    }
  }

  private static void close(final Channel channel) {
    if (channel == null) {
      return;
    }
    IoUtils.safeClose(channel);
  }
}
