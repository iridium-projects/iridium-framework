package cc.asylum.iridium.test.dto;

import cc.asylum.iridium.core.validation.annotation.AssertFalse;
import cc.asylum.iridium.core.validation.annotation.AssertTrue;
import cc.asylum.iridium.core.validation.annotation.Digits;
import cc.asylum.iridium.core.validation.annotation.Email;
import cc.asylum.iridium.core.validation.annotation.EscapeHtml;
import cc.asylum.iridium.core.validation.annotation.Future;
import cc.asylum.iridium.core.validation.annotation.Max;
import cc.asylum.iridium.core.validation.annotation.Min;
import cc.asylum.iridium.core.validation.annotation.Negative;
import cc.asylum.iridium.core.validation.annotation.NegativeOrZero;
import cc.asylum.iridium.core.validation.annotation.NotBlank;
import cc.asylum.iridium.core.validation.annotation.NotEmpty;
import cc.asylum.iridium.core.validation.annotation.NotNull;
import cc.asylum.iridium.core.validation.annotation.Past;
import cc.asylum.iridium.core.validation.annotation.Pattern;
import cc.asylum.iridium.core.validation.annotation.Positive;
import cc.asylum.iridium.core.validation.annotation.PositiveOrZero;
import cc.asylum.iridium.core.validation.annotation.Size;

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
