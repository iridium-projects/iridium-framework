package cc.asylum.iridium.test.dto;

import cc.asylum.iridium.core.validation.annotation.AssertTrue;
import cc.asylum.iridium.core.validation.annotation.Email;
import cc.asylum.iridium.core.validation.annotation.EscapeHtml;
import cc.asylum.iridium.core.validation.annotation.Max;
import cc.asylum.iridium.core.validation.annotation.Min;
import cc.asylum.iridium.core.validation.annotation.NotBlank;
import cc.asylum.iridium.core.validation.annotation.Pattern;
import cc.asylum.iridium.core.validation.annotation.Positive;
import cc.asylum.iridium.core.validation.annotation.Size;

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
