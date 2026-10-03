package cc.asylum.iridium.core.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ConstraintViolationTest {

  @Test
  void holdsFieldMessageAndValue() {
    final ConstraintViolation violation = new ConstraintViolation("name", "blank", "");
    assertEquals("name", violation.field());
    assertEquals("blank", violation.message());
    assertEquals("", violation.invalidValue());
  }
}
