package dev.yyuh.iridium.test;

import de.yyuh.iridium.core.validation.annotation.AssertTrue;
import de.yyuh.iridium.core.validation.annotation.Email;
import de.yyuh.iridium.core.validation.annotation.EscapeHtml;
import de.yyuh.iridium.core.validation.annotation.Max;
import de.yyuh.iridium.core.validation.annotation.Min;
import de.yyuh.iridium.core.validation.annotation.NotBlank;
import de.yyuh.iridium.core.validation.annotation.Pattern;
import de.yyuh.iridium.core.validation.annotation.Positive;
import de.yyuh.iridium.core.validation.annotation.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 50) String name,
        @Email String email,
        @Min(18) @Max(120) int age,
        @Positive double rating,
        @Pattern(regexp = "^[a-z0-9_]+$") String username,
        @EscapeHtml String bio,
        @AssertTrue boolean acceptedTerms
) {
}
