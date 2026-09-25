package cc.asylum.iridium.core.validation;

import io.avaje.jsonb.Json;

@Json
public record ConstraintViolation(
    String field,
    String message,
    Object invalidValue) {
}
