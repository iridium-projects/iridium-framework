package cc.asylum.iridium.data.spec;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpecValuesTest {

  @Test
  void blanksBecomeNullAndTextIsTrimmed() {
    assertNull(SpecValues.text(null));
    assertNull(SpecValues.text("  "));
    assertEquals("ada", SpecValues.text(" ada "));
  }

  @Test
  void splitsCommaSeparatedPartsAndDropsBlanks() {
    assertEquals(List.of(), SpecValues.parts(null));
    assertEquals(List.of(), SpecValues.parts(List.of()));
    assertEquals(List.of("a", "b", "c"), SpecValues.parts(java.util.Arrays.asList(" a, ,b ", null, "c,")));
    assertNull(SpecValues.texts(List.of(" , ")));
    assertEquals(List.of("a"), SpecValues.texts(List.of("a")));
  }

  @Test
  void parsesTruthyAliasesAndRejectsUnknownValues() {
    assertNull(SpecValues.truthy(null));
    assertNull(SpecValues.truthy(" "));
    assertEquals(true, SpecValues.bool(" YES "));
    assertEquals(false, SpecValues.bool("Off"));
    assertEquals(true, SpecValues.truthy("1"));
    assertEquals(false, SpecValues.truthy("0"));
    assertThrows(IllegalArgumentException.class, () -> SpecValues.truthy("maybe"));
  }

  @Test
  void parsesScalarsAndRejectsGarbage() {
    assertNull(SpecValues.integer(" "));
    assertEquals(7, SpecValues.integer(" 7 "));
    assertEquals(8L, SpecValues.longValue("8"));
    assertEquals(1.5d, SpecValues.doubleValue("1.5"));
    assertEquals(1.5f, SpecValues.floatValue("1.5"));
    assertEquals((short) 3, SpecValues.shortValue("3"));
    assertEquals((byte) 4, SpecValues.byteValue("4"));
    assertEquals(new BigDecimal("10.50"), SpecValues.decimal(" 10.50 "));
    assertEquals(new BigInteger("99"), SpecValues.integerBig("99"));
    final UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    assertEquals(id, SpecValues.uuid(id.toString()));
    assertEquals(LocalDate.parse("2020-01-02"), SpecValues.localDate("2020-01-02"));
    assertEquals(LocalTime.parse("01:02:03"), SpecValues.localTime("01:02:03"));
    assertEquals(LocalDateTime.parse("2020-01-02T03:04:05"), SpecValues.localDateTime("2020-01-02T03:04:05"));
    assertEquals(Instant.parse("2020-01-02T03:04:05Z"), SpecValues.instant("2020-01-02T03:04:05Z"));
    assertEquals(OffsetDateTime.parse("2020-01-02T03:04:05Z"), SpecValues.offsetDateTime("2020-01-02T03:04:05Z"));
    assertEquals(
        ZonedDateTime.of(2020, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC),
        SpecValues.zonedDateTime("2020-01-02T03:04:05Z"));
    assertEquals(Color.RED, SpecValues.enumeration("RED", Color.class));
    assertNull(SpecValues.enumeration(" ", Color.class));

    assertThrows(IllegalArgumentException.class, () -> SpecValues.integer("nope"));
    assertThrows(IllegalArgumentException.class, () -> SpecValues.uuid("nope"));
    assertThrows(IllegalArgumentException.class, () -> SpecValues.enumeration("red", Color.class));
    assertThrows(IllegalArgumentException.class, () -> SpecValues.decimal("nope"));
  }

  @Test
  void parsesListsAndDropsEmptyResults() {
    assertNull(SpecValues.integers(null));
    assertNull(SpecValues.integers(List.of(" , ")));
    assertEquals(List.of(1, 2), SpecValues.integers(List.of("1, 2")));
    assertEquals(List.of(3L), SpecValues.longs(List.of("3")));
    assertEquals(List.of(1.5d), SpecValues.doubles(List.of("1.5")));
    assertEquals(List.of(1.5f), SpecValues.floats(List.of("1.5")));
    assertEquals(List.of((short) 2), SpecValues.shorts(List.of("2")));
    assertEquals(List.of((byte) 3), SpecValues.bytes(List.of("3")));
    assertEquals(List.of(true, false), SpecValues.bools(List.of("yes,no")));
    assertEquals(List.of(new BigDecimal("1.0")), SpecValues.decimals(List.of("1.0")));
    assertEquals(List.of(new BigInteger("4")), SpecValues.integerBigs(List.of("4")));
    final UUID id = UUID.fromString("00000000-0000-0000-0000-000000000002");
    assertEquals(List.of(id), SpecValues.uuids(List.of(id.toString())));
    assertEquals(List.of(Color.RED, Color.BLUE), SpecValues.enumerations(List.of("RED,BLUE"), Color.class));
    assertNull(SpecValues.enumerations(List.of(), Color.class));
    assertThrows(IllegalArgumentException.class, () -> SpecValues.integers(List.of("1,nope")));
  }

  private enum Color {
    RED,
    BLUE
  }
}
