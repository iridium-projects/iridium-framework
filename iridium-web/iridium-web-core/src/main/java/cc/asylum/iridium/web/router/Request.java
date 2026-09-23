package cc.asylum.iridium.web.router;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record Request(
        String method,
        String path,
        Map<String, List<String>> headers,
        Map<String, List<String>> queryParameters,
        Map<String, String> pathVariables,
        byte[] body
) {

    public Optional<String> header(final String name) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .flatMap(List::stream)
                .findFirst();
    }

    public List<String> headers(final String name) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(List.of());
    }

    public Optional<String> query(final String name) {
        return Optional.ofNullable(queryParameters.get(name))
                .flatMap(values -> values.stream().findFirst());
    }

    public Optional<String> pathVariable(final String name) {
        return pathVariables == null
                ? Optional.empty()
                : Optional.ofNullable(pathVariables.get(name));
    }

    public Request withPathVariables(final Map<String, String> variables) {
        return new Request(method, path, headers, queryParameters, variables, body);
    }
}
