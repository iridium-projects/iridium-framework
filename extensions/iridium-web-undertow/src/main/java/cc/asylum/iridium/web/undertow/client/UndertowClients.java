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
import io.undertow.connector.PooledByteBuffer;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.Protocols;
import org.xnio.ChannelListeners;
import org.xnio.IoFuture;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Internal
public final class UndertowClients {

  private static final int BUFFER_SIZE = 16 * 1024;
  private static final int MAX_BODY = 1_048_576;
  private static final long TIMEOUT_SECONDS = 60L;

  private static final UndertowClient CLIENT = UndertowClient.getInstance();

  private static final Jsonb JSONB = Jsonb.instance();
  private static final ByteBufferPool BUFFERS = new DefaultByteBufferPool(
      true,
      BUFFER_SIZE,
      -1,
      4);

  private static final OptionMap OPTIONS = OptionMap.builder()
      .set(Options.WORKER_IO_THREADS, 2)
      .set(Options.TCP_NODELAY, true)
      .set(Options.KEEP_ALIVE, true)
      .set(Options.READ_TIMEOUT, (int) TIMEOUT_SECONDS * 1000)
      .set(Options.WRITE_TIMEOUT, (int) TIMEOUT_SECONDS * 1000)
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
      case final byte[] raw -> raw;
      default -> JSONB.toJsonBytes(body);
    };
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
    if (!Strings.blank(declared)) {
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
      final IoFuture<ClientConnection> connecting = "https".equalsIgnoreCase(uri.getScheme())
          ? CLIENT.connect(uri, worker(), ssl(), BUFFERS, OPTIONS)
          : CLIENT.connect(uri, worker(), BUFFERS, OPTIONS);

      if (connecting.awaitInterruptibly(TIMEOUT_SECONDS, TimeUnit.SECONDS) != IoFuture.Status.DONE) {
        connecting.cancel();
        throw new IllegalStateException("Timed out connecting to " + uri);
      }

      return connecting.get();
    } catch (final Exception e) {
      throw failure("Failed to connect to " + uri, e);
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
    final ByteBuffer payload = body == null || body.length == 0 ? null : ByteBuffer.wrap(body);

    connection.sendRequest(request(uri, method, path, contentType, body), new ClientCallback<>() {
      @Override
      public void completed(final ClientExchange exchange) {
        if (payload != null) {
          write(exchange.getRequestChannel(), payload, result);
        }

        exchange.setResponseListener(new ClientCallback<>() {
          @Override
          public void completed(final ClientExchange responseExchange) {
            read(responseExchange, result);
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
      return result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (final TimeoutException e) {
      throw new IllegalStateException("Request timed out", e);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Request interrupted", e);

    } catch (final Exception e) {
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
        .setPath(Strings.blank(path) ? "/" : path)
        .setProtocol(Protocols.HTTP_1_1);

    request.getRequestHeaders().put(Headers.HOST, host(uri));
    request.getRequestHeaders().put(Headers.CONNECTION, "close");
    if (!Strings.blank(contentType)) {
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

    return port < 0 ? uri.getHost() : uri.getHost() + ":" + port;
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

  private static void read(final ClientExchange exchange, final CompletableFuture<byte[]> result) {
    final ClientResponse response = exchange.getResponse();
    final StreamSourceChannel channel = exchange.getResponseChannel();
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    channel.getReadSetter().set(source -> {
      final PooledByteBuffer pooled = exchange.getConnection().getBufferPool().allocate();
      final ByteBuffer buffer = pooled.getBuffer();
      try {
        while (true) {
          buffer.clear();
          final int read = source.read(buffer);
          if (read == 0) {
            return;
          }

          if (read == -1) {
            finish(response, out.toByteArray(), result);
            return;
          }

          if (out.size() > MAX_BODY - read) {
            result.completeExceptionally(new IllegalStateException("Response exceeds " + MAX_BODY + " bytes"));
            return;
          }

          buffer.flip();
          final byte[] chunk = new byte[read];
          buffer.get(chunk);
          out.write(chunk);
        }
      } catch (final IOException e) {
        result.completeExceptionally(e);
      } finally {
        pooled.close();
      }
    });
    channel.resumeReads();
  }

  private static void finish(
      final ClientResponse response,
      final byte[] body,
      final CompletableFuture<byte[]> result) {
    final int status = response.getResponseCode();

    if (status >= 400) {
      final String detail = Strings.utf8(body);
      final var message = new StringBuilder()
          .append(status)
          .append(" ")
          .append(response.getStatus())
          .append(detail == null ? "" : ": " + detail);

      result.completeExceptionally(new IllegalStateException(message.toString()));
      return;
    }

    result.complete(body);
  }

  private static void write(
      final StreamSinkChannel channel,
      final ByteBuffer buffer,
      final CompletableFuture<byte[]> result) {
    try {
      while (buffer.hasRemaining()) {
        if (channel.write(buffer) == 0) {
          channel.getWriteSetter().set(ignored -> write(channel, buffer, result));
          channel.resumeWrites();
          return;
        }
      }

      channel.shutdownWrites();
      if (!channel.flush()) {
        channel.getWriteSetter().set(ChannelListeners.flushingChannelListener(
            ignored -> channel.getWriteSetter().set(null),
            ChannelListeners.closingChannelExceptionHandler()));

        channel.resumeWrites();
        return;
      }

      channel.getWriteSetter().set(null);
      channel.suspendWrites();
    } catch (final IOException e) {
      result.completeExceptionally(e);
    }
  }

  private static void close(final Channel channel) {
    if (channel != null) {
      IoUtils.safeClose(channel);
    }
  }

  private static IllegalStateException failure(final String message, final Exception cause) {
    final Throwable unwrapped = cause.getCause() == null ? cause : cause.getCause();

    if (unwrapped instanceof RuntimeException runtime) {
      return new IllegalStateException(message, runtime);
    }

    return new IllegalStateException(message, unwrapped);
  }
}
