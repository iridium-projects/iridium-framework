package cc.asylum.iridium.web.response;

import java.util.HashMap;
import java.util.Map;

public final class Response<T> {

  private final int status;
  private final Map<String, String> headers;
  private final T body;

  private Response(
      final int status,
      final Map<String, String> headers,
      final T body) {
    this.status = status;
    this.headers = Map.copyOf(headers);
    this.body = body;
  }

  public int status() {
    return status;
  }

  public Map<String, String> headers() {
    return headers;
  }

  public T body() {
    return body;
  }

  public static BodyBuilder ok() {
    return status(200);
  }

  public static <T> Response<T> ok(final T body) {
    return status(200).body(body);
  }

  public static BodyBuilder status(final int status) {
    return new DefaultBodyBuilder(status);
  }

  public static BodyBuilder notFound() {
    return status(404);
  }

  public static BodyBuilder badRequest() {
    return status(400);
  }

  public static BodyBuilder noContent() {
    return status(204);
  }

  public static BodyBuilder created(final String location) {
    return new DefaultBodyBuilder(201).header("Location", location);
  }

  public interface BodyBuilder {

    BodyBuilder header(final String name, final String value);

    <T> Response<T> body(final T body);

    <T> Response<T> build();
  }

  private static final class DefaultBodyBuilder implements BodyBuilder {

    private final int status;
    private final Map<String, String> headers = new HashMap<>();

    private DefaultBodyBuilder(final int status) {
      this.status = status;
    }

    @Override
    public BodyBuilder header(final String name, final String value) {
      if (name == null || value == null) {
        throw new IllegalArgumentException("Header name and value must not be null");
      }

      headers.put(name, value);
      return this;
    }

    @Override
    public <T> Response<T> body(final T body) {
      return new Response<>(status, headers, body);
    }

    @Override
    public <T> Response<T> build() {
      return new Response<>(status, headers, null);
    }
  }
}
