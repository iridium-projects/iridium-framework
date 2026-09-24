package cc.asylum.iridium.codegen.processor;

import cc.asylum.iridium.codegen.ProcessorHarness;
import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.validation.ConstraintViolation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URLClassLoader;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationConstraintTest {

  private static ProcessorHarness.Result compiled;
  private static URLClassLoader loader;

  @BeforeAll
  static void compileFixtures() throws Exception {
    final Map<String, String> sources = new LinkedHashMap<>();
    sources.put("test.NameForm", """
            package test;
            import cc.asylum.iridium.core.validation.annotation.NotBlank;
            import cc.asylum.iridium.core.validation.annotation.NotNull;
            public record NameForm(@NotNull @NotBlank String name) {}
            """);
    sources.put("test.SizeForm", """
            package test;
            import cc.asylum.iridium.core.validation.annotation.Size;
            public record SizeForm(@Size(min = 2, max = 4) String code) {}
            """);
    sources.put("test.BoundForm", """
            package test;
            import cc.asylum.iridium.core.validation.annotation.Max;
            import cc.asylum.iridium.core.validation.annotation.Min;
            public record BoundForm(@Min(1) @Max(10) int count) {}
            """);
    sources.put("test.SignForm", """
            package test;
            import cc.asylum.iridium.core.validation.annotation.Negative;
            import cc.asylum.iridium.core.validation.annotation.NegativeOrZero;
            import cc.asylum.iridium.core.validation.annotation.Positive;
            import cc.asylum.iridium.core.validation.annotation.PositiveOrZero;
            public record SignForm(
                @Positive Integer positive,
                @Negative Integer negative,
                @PositiveOrZero Integer positiveOrZero,
                @NegativeOrZero Integer negativeOrZero) {}
            """);
    sources.put("test.DigitsForm", """
            package test;
            import cc.asylum.iridium.core.validation.annotation.Digits;
            public record DigitsForm(@Digits(integer = 2, fraction = 1) java.math.BigDecimal amount) {}
            """);
    sources.put("test.PatternForm", """
            package test;
            import cc.asylum.iridium.core.validation.annotation.Pattern;
            public record PatternForm(@Pattern(regexp = "[a-z]+") String token) {}
            """);
    sources.put("test.EmailForm", """
            package test;
            import cc.asylum.iridium.core.validation.annotation.Email;
            public record EmailForm(@Email String email) {}
            """);
    sources.put("test.WhenForm", """
            package test;
            import cc.asylum.iridium.core.validation.annotation.Future;
            import cc.asylum.iridium.core.validation.annotation.Past;
            public record WhenForm(@Past java.time.LocalDate born, @Future java.time.LocalDate due) {}
            """);
    sources.put("test.FlagForm", """
            package test;
            import cc.asylum.iridium.core.validation.annotation.AssertFalse;
            import cc.asylum.iridium.core.validation.annotation.AssertTrue;
            public record FlagForm(@AssertTrue boolean accepted, @AssertFalse Boolean rejected) {}
            """);
    sources.put("test.EmptyForm", """
            package test;
            import cc.asylum.iridium.core.validation.annotation.NotEmpty;
            public record EmptyForm(@NotEmpty java.util.List<String> tags) {}
            """);
    sources.put("test.HtmlForm", """
            package test;
            import cc.asylum.iridium.core.validation.annotation.EscapeHtml;
            public record HtmlForm(@EscapeHtml String text) {}
            """);
    sources.put("test.OptionalForm", """
            package test;
            import cc.asylum.iridium.core.validation.annotation.Nullable;
            public record OptionalForm(@Nullable String note) {}
            """);
    compiled = ProcessorHarness.compile(sources, new ValidationProcessor());
    assertTrue(compiled.success(), () -> String.join("\n", compiled.errors()));
    loader = ProcessorHarness.classLoader(compiled);
  }

  @Test
  void checksNullBlankAndSizeBoundaries() throws Exception {
    assertTrue(validate("NameForm", "Ada").isOk());
    assertMessage(validate("NameForm", "   "), "name", "must not be blank");
    final Result<?, List<ConstraintViolation>> missing = validate("NameForm", new Object[] {null});
    assertTrue(missing.unwrapErr().stream().anyMatch(violation -> violation.message().equals("must not be null")));
    assertTrue(missing.unwrapErr().stream().anyMatch(violation -> violation.message().equals("must not be blank")));

    assertTrue(validate("SizeForm", "ab").isOk());
    assertTrue(validate("SizeForm", "abcd").isOk());
    assertTrue(validate("SizeForm", new Object[] {null}).isOk());
    assertMessage(validate("SizeForm", "a"), "code", "size must be between 2 and 4");
    assertMessage(validate("SizeForm", "abcde"), "code", "size must be between 2 and 4");
  }

  @Test
  void checksNumericBoundsSignsAndDigits() throws Exception {
    assertTrue(validate("BoundForm", 1).isOk());
    assertTrue(validate("BoundForm", 10).isOk());
    assertMessage(validate("BoundForm", 0), "count", "must be >= 1");
    assertMessage(validate("BoundForm", 11), "count", "must be <= 10");

    assertTrue(validate("SignForm", 1, -1, 0, 0).isOk());
    assertTrue(validate("SignForm", 1, -1, 0, -2).isOk());
    assertMessage(validate("SignForm", 0, -1, 0, 0), "positive", "must be positive");
    assertMessage(validate("SignForm", 1, 0, 0, 0), "negative", "must be negative");
    assertMessage(validate("SignForm", 1, -1, -1, 0), "positiveOrZero", "must be positive or zero");
    assertMessage(validate("SignForm", 1, -1, 0, 1), "negativeOrZero", "must be negative or zero");
    assertTrue(validate("SignForm", new Object[] {null, null, null, null}).isOk());

    assertTrue(validate("DigitsForm", new BigDecimal("12.3")).isOk());
    assertTrue(validate("DigitsForm", new Object[] {null}).isOk());
    assertTrue(validate("DigitsForm", new BigDecimal("123.4")).isErr());
    assertTrue(validate("DigitsForm", new BigDecimal("1.23")).isErr());
  }

  @Test
  void checksPatternEmailDatesFlagsAndEmptiness() throws Exception {
    assertTrue(validate("PatternForm", "abc").isOk());
    assertTrue(validate("PatternForm", new Object[] {null}).isOk());
    assertMessage(validate("PatternForm", "A"), "token", "must match [a-z]+");

    assertTrue(validate("EmailForm", "a@b.c").isOk());
    assertTrue(validate("EmailForm", new Object[] {null}).isOk());
    assertMessage(validate("EmailForm", "not-an-email"), "email", "must be a valid email address");
    assertMessage(validate("EmailForm", "a@b"), "email", "must be a valid email address");

    assertTrue(validate("WhenForm", LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)).isOk());
    assertTrue(validate("WhenForm", null, null).isOk());
    assertMessage(validate("WhenForm", LocalDate.now().plusDays(1), LocalDate.now().plusDays(1)), "born", "must be in the past");
    assertMessage(validate("WhenForm", LocalDate.now().minusDays(1), LocalDate.now().minusDays(1)), "due", "must be in the future");

    assertTrue(validate("FlagForm", true, false).isOk());
    assertTrue(validate("FlagForm", true, null).isErr());
    assertMessage(validate("FlagForm", false, false), "accepted", "must be true");
    assertMessage(validate("FlagForm", true, true), "rejected", "must be false");

    assertTrue(validate("EmptyForm", List.of("x")).isOk());
    assertMessage(validate("EmptyForm", List.of()), "tags", "must not be empty");
    assertMessage(validate("EmptyForm", new Object[] {null}), "tags", "must not be empty");
  }

  @Test
  void escapesHtmlAndSkipsNullableOnlyTypes() throws Exception {
    final Class<?> form = loader.loadClass("test.HtmlForm");
    final Object value = form.getDeclaredConstructor(String.class).newInstance("<a>");
    final Object validator = loader.loadClass("test.gen.HtmlFormValidator").getDeclaredConstructor().newInstance();
    final Object escaped = validator.getClass().getMethod("escape", form).invoke(validator, value);
    assertEquals("&lt;a&gt;", escaped.getClass().getMethod("text").invoke(escaped));
    assertFalse(compiled.generatedSources().resolve("test/gen/OptionalFormValidator.java").toFile().exists());
  }

  private static Result<?, List<ConstraintViolation>> validate(final String type, final Object... args) throws Exception {
    final Class<?> form = loader.loadClass("test." + type);
    java.lang.reflect.Constructor<?> constructor = null;
    for (final java.lang.reflect.Constructor<?> candidate : form.getDeclaredConstructors()) {
      if (candidate.getParameterCount() == args.length) {
        constructor = candidate;
        break;
      }
    }
    if (constructor == null) {
      throw new NoSuchMethodException(type);
    }
    final Object value = constructor.newInstance(args);
    final Object validator = loader.loadClass("test.gen." + type + "Validator").getDeclaredConstructor().newInstance();
    final Method method = validator.getClass().getMethod("validate", form);
    @SuppressWarnings("unchecked")
    final Result<?, List<ConstraintViolation>> result =
        (Result<?, List<ConstraintViolation>>) method.invoke(validator, value);
    return result;
  }

  private static void assertMessage(
      final Result<?, List<ConstraintViolation>> result,
      final String field,
      final String message
  ) {
    assertTrue(result.isErr(), () -> String.valueOf(result));
    assertTrue(result.unwrapErr().stream().anyMatch(violation ->
        field.equals(violation.field()) && message.equals(violation.message())), () -> result.unwrapErr().toString());
  }
}
