package cc.asylum.iridium.web.http;

import java.util.Map;

@FunctionalInterface
public interface RequestInterceptor {

  void apply(final RequestTemplate template);

  interface RequestTemplate {

    String method();

    String url();

    RequestTemplate url(final String url);

    String path();

    RequestTemplate path(final String path);

    Map<String, String> headers();

    RequestTemplate header(
      final String name,
      final String value);

    byte[] body();

    RequestTemplate body(byte[] body);
  }
}
