package cc.asylum.iridium.core.processor.validation.check;

import cc.asylum.forgery.stmt.Stmt;
import cc.asylum.iridium.core.annotation.Internal;

import java.util.List;
import java.util.Map;

@Internal
public interface ConstraintCheck {

  List<Stmt> emit(final ValidatedField field, final Map<String, String> patterns);
}
