package cc.asylum.iridium.core.validation;

import cc.asylum.iridium.core.result.Result;

import java.util.List;

public final class SampleValidationRegistrar implements ValidationRegistrar {

  @Override
  public void register(final ValidatorRegistry registry) {
    registry.register(Marked.class, new MarkedValidator());
  }

  static final class MarkedValidator implements Validator<Marked>, Escaper<Marked> {

    @Override
    public Result<Marked, List<ConstraintViolation>> validate(final Marked value) {
      if ("ok".equals(value.value())) {
        return Result.ok(value);
      }
      return Result.err(List.of(new ConstraintViolation("name", "invalid", value.value())));
    }

    @Override
    public Marked escape(final Marked value) {
      return new Marked("escaped");
    }
  }
}
