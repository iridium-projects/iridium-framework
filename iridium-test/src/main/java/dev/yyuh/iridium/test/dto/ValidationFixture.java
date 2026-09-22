package dev.yyuh.iridium.test.dto;

import de.yyuh.iridium.core.validation.annotation.AssertFalse;
import de.yyuh.iridium.core.validation.annotation.AssertTrue;
import de.yyuh.iridium.core.validation.annotation.Digits;
import de.yyuh.iridium.core.validation.annotation.Email;
import de.yyuh.iridium.core.validation.annotation.EscapeHtml;
import de.yyuh.iridium.core.validation.annotation.Future;
import de.yyuh.iridium.core.validation.annotation.Max;
import de.yyuh.iridium.core.validation.annotation.Min;
import de.yyuh.iridium.core.validation.annotation.Negative;
import de.yyuh.iridium.core.validation.annotation.NegativeOrZero;
import de.yyuh.iridium.core.validation.annotation.NotBlank;
import de.yyuh.iridium.core.validation.annotation.NotEmpty;
import de.yyuh.iridium.core.validation.annotation.NotNull;
import de.yyuh.iridium.core.validation.annotation.Past;
import de.yyuh.iridium.core.validation.annotation.Pattern;
import de.yyuh.iridium.core.validation.annotation.Positive;
import de.yyuh.iridium.core.validation.annotation.PositiveOrZero;
import de.yyuh.iridium.core.validation.annotation.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ValidationFixture(
        @NotNull String notNullField,
        @NotBlank String notBlankField,
        @NotEmpty String notEmptyField,
        @Size(min = 2, max = 5) String sizeField,
        @Min(10) int minField,
        @Max(20) int maxField,
        @Positive int positiveField,
        @Negative int negativeField,
        @PositiveOrZero int positiveOrZeroField,
        @NegativeOrZero int negativeOrZeroField,
        @Digits(integer = 3, fraction = 2) BigDecimal digitsField,
        @Pattern(regexp = "^[a-z]+$") String patternField,
        @Email String emailField,
        @Past LocalDate pastField,
        @Future LocalDate futureField,
        @AssertTrue boolean assertTrueField,
        @AssertFalse boolean assertFalseField,
        @EscapeHtml String escapeHtmlField
) {
}
