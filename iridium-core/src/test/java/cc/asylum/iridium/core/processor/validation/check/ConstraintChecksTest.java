package cc.asylum.iridium.core.processor.validation.check;

import org.junit.jupiter.api.Test;

import javax.annotation.processing.Messager;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

final class ConstraintChecksTest {

  @Test
  void allReturnsEveryCheck() {
    assertEquals(5, ConstraintChecks.all(mock(Types.class), mock(Elements.class), mock(Messager.class)).size());
  }
}
