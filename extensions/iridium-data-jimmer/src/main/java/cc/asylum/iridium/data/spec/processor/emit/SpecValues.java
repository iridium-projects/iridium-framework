package cc.asylum.iridium.data.spec.processor.emit;

import cc.asylum.iridium.core.util.Strings;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SpecValues {

  private SpecValues() {
  }

  public static String text(final String raw) {
    return Strings.blank(raw) ? null : raw.trim();
  }

  public static List<String> parts(final List<String> raw) {
    if (raw == null || raw.isEmpty()) {
      return List.of();
    }
    final List<String> values = new ArrayList<>();
    for (final String item : raw) {
      if (item == null) {
        continue;
      }
      values.addAll(Strings.split(item, ','));
    }
    return values;
  }

  public static Boolean truthy(final String raw) {
    if (Strings.blank(raw)) {
      return null;
    }
    final Boolean parsed = Strings.truthy(raw);
    if (parsed == null) {
       throw new IllegalArgumentException(raw);
    }
    return parsed;
  }

  public static Integer integer(final String raw) {
     return parse(raw, Integer::valueOf);
  }

  public static Long longValue(final String raw) {
     return parse(raw, Long::valueOf);
  }

  public static Double doubleValue(final String raw) {
     return parse(raw, Double::valueOf);
  }

  public static Float floatValue(final String raw) {
     return parse(raw, Float::valueOf);
  }

  public static Short shortValue(final String raw) {
     return parse(raw, Short::valueOf);
  }

  public static Byte byteValue(final String raw) {
     return parse(raw, Byte::valueOf);
  }

  public static Boolean bool(final String raw) {
     return truthy(raw);
  }

  public static BigDecimal decimal(final String raw) {
     return parse(raw, BigDecimal::new);
  }

  public static BigInteger integerBig(final String raw) {
     return parse(raw, BigInteger::new);
  }

  public static UUID uuid(final String raw) {
     return parse(raw, UUID::fromString);
  }

  public static LocalDate localDate(final String raw) {
     return parse(raw, LocalDate::parse);
  }

  public static LocalDateTime localDateTime(final String raw) {
     return parse(raw, LocalDateTime::parse);
  }

  public static LocalTime localTime(final String raw) {
     return parse(raw, LocalTime::parse);
  }

  public static Instant instant(final String raw) {
     return parse(raw, Instant::parse);
  }

  public static OffsetDateTime offsetDateTime(final String raw) {
     return parse(raw, OffsetDateTime::parse);
  }

  public static ZonedDateTime zonedDateTime(final String raw) {
     return parse(raw, ZonedDateTime::parse);
  }

  public static <E extends Enum<E>> E enumeration(final String raw, final Class<E> type) {
    if (Strings.blank(raw)) {
      return null;
    }
    try {
      return Enum.valueOf(type, raw.trim());
    } catch (final IllegalArgumentException exception) {
       throw new IllegalArgumentException(raw);
    }
  }

  public static List<Integer> integers(final List<String> raw) {
     return list(raw, SpecValues::integer);
  }

  public static List<Long> longs(final List<String> raw) {
     return list(raw, SpecValues::longValue);
  }

  public static List<Double> doubles(final List<String> raw) {
     return list(raw, SpecValues::doubleValue);
  }

  public static List<Float> floats(final List<String> raw) {
     return list(raw, SpecValues::floatValue);
  }

  public static List<Short> shorts(final List<String> raw) {
     return list(raw, SpecValues::shortValue);
  }

  public static List<Byte> bytes(final List<String> raw) {
     return list(raw, SpecValues::byteValue);
  }

  public static List<Boolean> bools(final List<String> raw) {
     return list(raw, SpecValues::bool);
  }

  public static List<BigDecimal> decimals(final List<String> raw) {
     return list(raw, SpecValues::decimal);
  }

  public static List<BigInteger> integerBigs(final List<String> raw) {
     return list(raw, SpecValues::integerBig);
  }

  public static List<UUID> uuids(final List<String> raw) {
     return list(raw, SpecValues::uuid);
  }

  public static List<String> texts(final List<String> raw) {
    final List<String> parts = parts(raw);
    return parts.isEmpty() ? null : parts;
  }

  public static <E extends Enum<E>> List<E> enumerations(final List<String> raw, final Class<E> type) {
    final List<String> parts = parts(raw);
    if (parts.isEmpty()) {
      return null;
    }
    final List<E> values = new ArrayList<>();
    for (final String part : parts) {
      values.add(enumeration(part, type));
    }
    return values;
  }

  private static <T> T parse(final String raw, final java.util.function.Function<String, T> parser) {
    if (Strings.blank(raw)) {
      return null;
    }
    try {
      return parser.apply(raw.trim());
    } catch (final RuntimeException exception) {
       throw new IllegalArgumentException(raw);
    }
  }

  private static <T> List<T> list(
      final List<String> raw,
      final java.util.function.Function<String, T> parser
  ) {
    final List<String> parts = parts(raw);
    if (parts.isEmpty()) {
      return null;
    }
    final List<T> values = new ArrayList<>();
    for (final String part : parts) {
      final T parsed = parser.apply(part);
      if (parsed != null) {
        values.add(parsed);
      }
    }
    return values.isEmpty() ? null : values;
  }
}
