package cc.asylum.iridium.config.processor.walk;

import cc.asylum.forgery.expr.Expr;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

final class ConfigKeyTest {

  @Test
  void joinsLiteralIndexedAndExpressionKeys() {
    final ConfigKey empty = ConfigKey.literal("");
    final ConfigKey child = empty.child("app");
    assertNull(child.expression());
    assertNotNull(child.code());
    final ConfigKey nested = child.child("name");
    assertNotNull(nested.code());
    final ConfigKey indexed = nested.indexed("i");
    assertNotNull(indexed.code());
    final ConfigKey suffix = indexed.child("field");
    assertNotNull(suffix.code());
    final ConfigKey deeper = suffix.child("inner");
    assertNotNull(deeper.code());
    final ConfigKey reindexed = indexed.indexed("j");
    assertNotNull(reindexed.code());
    final ConfigKey expressed = ConfigKey.literal("root").indexed("i").child("name").indexed("k");
    assertNotNull(expressed.expression());
    assertNotNull(expressed.child("tail").code());
    assertNotNull(ConfigKey.literal("only").code());
  }
}
