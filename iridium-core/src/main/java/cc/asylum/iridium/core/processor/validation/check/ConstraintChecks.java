package cc.asylum.iridium.core.processor.validation.check;

import cc.asylum.iridium.core.annotation.Internal;

import javax.annotation.processing.Messager;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

@Internal
public final class ConstraintChecks {

  private ConstraintChecks() {
  }

  public static List<ConstraintCheck> all(final Types types, final Elements elements, final Messager messager) {
    return List.of(
      new PresenceChecks(types, elements),
      new NumericChecks(types, elements),
      new TextChecks(),
      new TemporalChecks(types, elements, messager),
      new FlagChecks()
    );
  }
}
