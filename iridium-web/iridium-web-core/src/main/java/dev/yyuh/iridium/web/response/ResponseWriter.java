package dev.yyuh.iridium.web.response;

import de.yyuh.iridium.core.component.Component;
import dev.yyuh.iridium.json.Json;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public final class ResponseWriter {

    private final Json json;

    public ResponseWriter(final Json json) {
        this.json = json;
    }

    public byte[] writeBody(final Response<?> response) {
        final Object body = response.body();

        if (body == null) {
            return new byte[0];
        }

        if (body instanceof byte[] bytes) {
            return bytes;
        }

        if (body instanceof String string) {
            return string.getBytes(StandardCharsets.UTF_8);
        }

        return json.serializeBytes(body);
    }

    public String contentType(final Response<?> response) {
        for (final Map.Entry<String, String> header : response.headers().entrySet()) {
            if (header.getKey().equalsIgnoreCase("Content-Type")) {
                return header.getValue();
            }
        }
        return "application/json";
    }
}
