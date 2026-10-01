package cc.asylum.iridium.core.component;

import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.bean.Bean;
import cc.asylum.iridium.core.config.ConfigInitializer;
import cc.asylum.iridium.core.validation.Valid;
import cc.asylum.iridium.core.validation.annotation.AssertFalse;
import cc.asylum.iridium.core.validation.annotation.AssertTrue;
import cc.asylum.iridium.core.validation.annotation.Constraint;
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
import cc.asylum.iridium.core.validation.annotation.Nullable;
import cc.asylum.iridium.core.validation.annotation.Past;
import cc.asylum.iridium.core.validation.annotation.Pattern;
import cc.asylum.iridium.core.validation.annotation.Positive;
import cc.asylum.iridium.core.validation.annotation.PositiveOrZero;
import cc.asylum.iridium.core.validation.annotation.Size;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComponentTest {

  @Test
  void markersAreAnnotations() throws Exception {
    assertTrue(Component.class.isAnnotation());
    assertTrue(Bean.class.isAnnotation());
    assertTrue(Valid.class.isAnnotation());
    assertTrue(Internal.class.isAnnotation());
    assertTrue(Constraint.class.isAnnotation());
    final Class<?>[] types = {
        AssertFalse.class, AssertTrue.class, Digits.class, Email.class, EscapeHtml.class,
        Future.class, Max.class, Min.class, Negative.class, NegativeOrZero.class,
        NotBlank.class, NotEmpty.class, NotNull.class, Nullable.class, Past.class,
        Pattern.class, Positive.class, PositiveOrZero.class, Size.class
    };
    for (final Class<?> type : types) {
      assertTrue(type.isAnnotation());
    }
    final var field = Sample.class.getDeclaredField("value");
    assertEquals(3L, field.getAnnotation(Min.class).value());
    assertEquals(9L, field.getAnnotation(Max.class).value());
    assertEquals(1, field.getAnnotation(Size.class).min());
    assertEquals(4, field.getAnnotation(Size.class).max());
    assertEquals("a+", field.getAnnotation(Pattern.class).regexp());
    assertEquals(2, field.getAnnotation(Digits.class).integer());
    assertEquals(1, field.getAnnotation(Digits.class).fraction());
    assertNotNull(field.getAnnotation(Email.class));
    assertNotNull(field.getAnnotation(NotBlank.class));
    assertNotNull(field.getAnnotation(NotEmpty.class));
    assertNotNull(field.getAnnotation(NotNull.class));
    assertNotNull(field.getAnnotation(Nullable.class));
    assertNotNull(field.getAnnotation(EscapeHtml.class));
    assertNotNull(field.getAnnotation(AssertTrue.class));
    assertNotNull(field.getAnnotation(AssertFalse.class));
    assertNotNull(field.getAnnotation(Negative.class));
    assertNotNull(field.getAnnotation(NegativeOrZero.class));
    assertNotNull(field.getAnnotation(Positive.class));
    assertNotNull(field.getAnnotation(PositiveOrZero.class));
    assertNotNull(field.getAnnotation(Future.class));
    assertNotNull(field.getAnnotation(Past.class));
    assertNotNull(Sample.class.getDeclaredMethod("take", String.class).getParameters()[0].getAnnotation(Valid.class));
    assertNotNull(Sample.class.getAnnotation(Component.class));
    assertTrue(Sample.class.getDeclaredMethod("make").isAnnotationPresent(Bean.class) || Bean.class.isAnnotation());
  }

  @Test
  void configInitializerPreparesArgs() {
    final ConfigInitializer initializer = args -> assertEquals("a", args[0]);
    initializer.prepare(new String[] {"a"});
  }

  @Component
  static final class Sample {

    @Min(3)
    @Max(9)
    @Size(min = 1, max = 4)
    @Pattern(regexp = "a+")
    @Digits(integer = 2, fraction = 1)
    @Email
    @NotBlank
    @NotEmpty
    @NotNull
    @Nullable
    @EscapeHtml
    @AssertTrue
    @AssertFalse
    @Negative
    @NegativeOrZero
    @Positive
    @PositiveOrZero
    @Future
    @Past
    String value;

    @Bean
    String make() {
      return value;
    }

    void take(@Valid final String raw) {
      value = raw;
    }
  }
}
