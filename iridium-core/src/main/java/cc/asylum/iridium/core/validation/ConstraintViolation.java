package cc.asylum.iridium.core.validation;

public record ConstraintViolation(
    String field,
    String message,
    Object invalidValue) {
}
