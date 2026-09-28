package cc.asylum.iridium.web.router;

import cc.asylum.iridium.core.util.Lists;
import cc.asylum.iridium.core.util.Paths;

import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;

public final class Request {

  private final String method;
  private final String path;
  private final String[] segments;

  private final Map<String, List<String>> headerMap;
  private final Map<String, List<String>> headers;

  private final Map<String, ? extends Collection<String>> queryParameters;
  private final Map<String, String> pathVariables;

  private final byte[] body;
  private final Supplier<InputStream> input;

  public Request(
      final String method,
      final String path,
      final Map<String, List<String>> headers,
      final Map<String, ? extends Collection<String>> queryParameters,
      final Map<String, String> pathVariables,
      final byte[] body) {
    this(
        method,
        path,
        null,
        null,
        headers,
        queryParameters,
        pathVariables,
        body,
        null);
  }

  public Request(
      final String method,
      final String path,
      final String[] segments,
      final Map<String, List<String>> headerMap,
      final Map<String, ? extends Collection<String>> queryParameters,
      final Supplier<InputStream> input) {
    this(
        method,
        path,
        segments,
        headerMap,
        null,
        queryParameters,
        Map.of(),
        null,
        input);
  }

  private Request(
      final String method,
      final String path,
      final String[] segments,
      final Map<String, List<String>> headerMap,
      final Map<String, List<String>> headers,
      final Map<String, ? extends Collection<String>> queryParameters,
      final Map<String, String> pathVariables,
      final byte[] body,
      final Supplier<InputStream> input) {
    this.method = method;
    this.path = path;
    this.segments = segments;
    this.headerMap = headerMap;
    this.headers = headers;
    this.queryParameters = queryParameters;
    this.pathVariables = pathVariables;
    this.body = body;
    this.input = input;
  }

  public String method() {
    return method;
  }

  public String path() {
    return path;
  }

  public String[] segments() {
    return segments;
  }

  public Map<String, List<String>> headerMap() {
    return headerMap;
  }

  public Map<String, List<String>> headers() {
    return headers;
  }

  public Map<String, ? extends Collection<String>> queryParameters() {
    return queryParameters;
  }

  public Map<String, String> pathVariables() {
    return pathVariables;
  }

  public byte[] body() {
    return body;
  }

  public InputStream input() {
    return input == null ? null : input.get();
  }

  public String header(final String name) {
    return Lists.first(headers(name));
  }

  public List<String> headers(final String name) {
    if (name == null) {
      return List.of();
    }

    final Map<String, List<String>> source = headerMap != null ? headerMap : headers;
    if (source == null) {
      return List.of();
    }

    final List<String> values = source.get(name);
    if (values != null) {
      return values;
    }

    final String normalized = name.toLowerCase(Locale.ROOT);
    for (final Entry<String, List<String>> entry : source.entrySet()) {
      final String key = entry.getKey();
      if (key != null && key.toLowerCase(Locale.ROOT).equals(normalized)) {
        return entry.getValue() == null ? List.of() : entry.getValue();
      }
    }

    return List.of();
  }

  public String query(final String name) {
    if (queryParameters == null || name == null) {
      return null;
    }

    return Lists.first(queryParameters.get(name));
  }

  public String pathVariable(final String name) {
    return pathVariables == null ? null : pathVariables.get(name);
  }

  public Request withPathVariables(final Map<String, String> variables) {
    return new Request(
        method,
        path,
        segments,
        headerMap,
        headers,
        queryParameters,
        variables,
        body,
        input);
  }

  public Request withBody(final byte[] bytes) {
    return new Request(
        method,
        path,
        segments,
        headerMap,
        headers,
        queryParameters,
        pathVariables,
        bytes,
        input);
  }

  public static String[] split(final String path) {
    return Paths.segments(path);
  }

  public static String normalizeMethod(final String method) {
    return method == null
        ? ""
        : method.toUpperCase(Locale.ROOT);
  }
}
