package cc.asylum.iridium.config.processor.binding;

import cc.asylum.forgery.expr.Expr;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigPieceTest {

  @Test
  void exposesComponents() {
    final Expr code = Expr.nil();
    final ConfigPiece piece = new ConfigPiece(code, true, "name0");
    assertEquals(code, piece.code());
    assertTrue(piece.result());
    assertEquals("name0", piece.name());
    assertFalse(new ConfigPiece(code, false, "plain").result());
  }
}
