package dev.yyuh.iridium.test;

import de.yyuh.iridium.core.validation.Validation;

public class Main {
    public static void main(String[] args) {
        final var valid = new CreateUserRequest(
                "Alice", "alice@example.com", 30, 4.5, "alice_42", "<b>hello</b>", true);

        final var invalid = new CreateUserRequest(
                "", "not-an-email", 12, -1.0, "UPPER CASE", null, false);

        Validation.validate(valid).ifOk(v -> System.out.println("valid: ok"))
                .ifErr(errors -> System.out.println("valid: " + errors));

        Validation.validate(invalid).ifOk(v -> System.out.println("invalid: ok"))
                .ifErr(errors -> errors.forEach(System.out::println));

        final var escaped = Validation.escape(valid);
        System.out.println("escaped bio: " + escaped.bio());
    }
}
