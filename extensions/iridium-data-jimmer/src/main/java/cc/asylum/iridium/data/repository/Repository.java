package cc.asylum.iridium.data.repository;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;

import java.util.function.Supplier;

public abstract class Repository<E, ID> {

  protected final JSqlClient sql;
  protected final Class<E> entityType;
  protected final TableProxy<E> table;

  protected Repository(
      final JSqlClient sql,
      final Class<E> entityType,
      final TableProxy<E> table
  ) {
    this.sql = sql;
    this.entityType = entityType;
    this.table = table;
  }

  public JSqlClient sql() {
    return sql;
  }

  public Class<E> entityType() {
    return entityType;
  }

  public TableProxy<E> table() {
    return table;
  }

  public <T> T transaction(final Supplier<T> block) {
    return sql.transaction(block);
  }

  public void transaction(final Runnable block) {
    sql.transaction(() -> {
      block.run();
      return null;
    });
  }
}
