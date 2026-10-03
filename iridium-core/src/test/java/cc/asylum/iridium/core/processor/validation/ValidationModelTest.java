package cc.asylum.iridium.core.processor.validation;

import cc.asylum.iridium.core.validation.annotation.AssertFalse;
import cc.asylum.iridium.core.validation.annotation.Size;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.annotation.processing.Filer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ValidationModelTest {

  @Test
  void annotationsListsConstraintTypes() {
    final var names = ValidationModel.annotations();
    assertTrue(names.contains(AssertFalse.class.getCanonicalName()));
    assertTrue(names.contains(Size.class.getCanonicalName()));
    assertEquals(18, names.size());
  }

  @Test
  void finishIsANoopWhenNothingWasValidated() {
    final ValidationModel model = new ValidationModel();
    model.finish(Mockito.mock(Filer.class));
    model.finish(Mockito.mock(Filer.class));
  }
}
