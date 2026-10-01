package cc.asylum.iridium.core.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StringsTest {

  @Test
  void firstNonBlankSkipsEmptyValues() {
    assertNull(Strings.firstNonBlank((String[]) null));
    assertNull(Strings.firstNonBlank(null, "  ", ""));
    assertEquals("kept", Strings.firstNonBlank(null, " ", "kept", "later"));
  }

  @Test
  void capitalizesAndDecapitalizes() {
    assertNull(Strings.capitalize(null));
    assertEquals("", Strings.capitalize(""));
    assertEquals("Name", Strings.capitalize("name"));
    assertEquals("URL", Strings.capitalize("URL"));
    assertNull(Strings.decapitalize(null));
    assertEquals("", Strings.decapitalize(""));
    assertEquals("name", Strings.decapitalize("Name"));
  }

  @Test
  void quoteEscapesSpecialCharacters() {
    assertEquals("null", Strings.quote(null));
    assertEquals("\"a\\\\b\\\"c\\n\\r\"", Strings.quote("a\\b\"c\n\r"));
  }

  @Test
  void blankAndSplit() {
    assertTrue(Strings.blank(null));
    assertTrue(Strings.blank(" \t"));
    assertFalse(Strings.blank("x"));
    assertEquals(List.of(), Strings.split(null, ','));
    assertEquals(List.of(), Strings.split("", ','));
    assertEquals(List.of("a", "b", "c"), Strings.split(" a , , b,c ", ','));
    assertEquals(List.of("a", "b"), Strings.split("a,b,", ','));
  }

  @Test
  void simpleNameAndTruthy() {
    assertNull(Strings.simpleName(null));
    assertEquals("", Strings.simpleName(""));
    assertEquals("Names", Strings.simpleName("cc.asylum.iridium.core.util.Names"));
    assertEquals("Names", Strings.simpleName("Names"));
    assertNull(Strings.truthy(null));
    assertEquals(Boolean.TRUE, Strings.truthy(" YES "));
    assertEquals(Boolean.TRUE, Strings.truthy("true"));
    assertEquals(Boolean.TRUE, Strings.truthy("on"));
    assertEquals(Boolean.TRUE, Strings.truthy("1"));
    assertEquals(Boolean.FALSE, Strings.truthy("No"));
    assertEquals(Boolean.FALSE, Strings.truthy("false"));
    assertEquals(Boolean.FALSE, Strings.truthy("off"));
    assertEquals(Boolean.FALSE, Strings.truthy("0"));
    assertNull(Strings.truthy("maybe"));
  }

  @Test
  void utf8AndTrim() {
    assertNull(Strings.utf8((byte[]) null));
    assertNull(Strings.utf8(new byte[0]));
    assertEquals("hi", Strings.utf8("hi".getBytes(StandardCharsets.UTF_8)));
    assertEquals(0, Strings.utf8((String) null).length);
    assertEquals("hi", new String(Strings.utf8("hi"), StandardCharsets.UTF_8));
    assertEquals("", Strings.trim(null, '/'));
    assertEquals("ab", Strings.trim("//ab//", '/'));
    assertEquals("", Strings.trim("///", '/'));
    assertEquals("ab", Strings.trim("ab", '/'));
  }
}
