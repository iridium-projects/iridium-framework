package cc.asylum.iridium.web.processor.binding.convert;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ParsedConversionTest {

  @Test
  void holdsOwnerAndMethod() {
    final ParsedConversion conversion = new ParsedConversion(Long.class, "parseLong");
    assertEquals(Long.class, conversion.owner());
    assertEquals("parseLong", conversion.method());
  }
}
