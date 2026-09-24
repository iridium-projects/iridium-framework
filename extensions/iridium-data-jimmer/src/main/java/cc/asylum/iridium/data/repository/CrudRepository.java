package cc.asylum.iridium.data.repository;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.BatchSaveResult;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;

import java.util.ArrayList;
import java.util.List;

public abstract class CrudRepository<E, ID> extends Repository<E, ID> {

  protected CrudRepository(
      final JSqlClient sql,
      final Class<E> entityType,
      final TableProxy<E> table
  ) {
    super(sql, entityType, table);
  }

  public E findById(final ID id) {
    return sql.findById(entityType, id);
  }

  public E findOneById(final ID id) {
    return sql.findOneById(entityType, id);
  }

  public List<E> findByIds(final Iterable<ID> ids) {
    return sql.findByIds(entityType, ids);
  }

  public List<E> findAll() {
    return sql.createQuery(table).select(table).execute();
  }

  public long count() {
    final Long total = sql.createQuery(table).select(table.count()).fetchOne();
    return total == null ? 0L : total;
  }

  public E save(final E entity) {
    return sql.saveCommand(entity)
        .setMode(SaveMode.NON_IDEMPOTENT_UPSERT)
        .execute()
        .getModifiedEntity();
  }

  public List<E> saveAll(final Iterable<E> entities) {
    final List<E> saved = new ArrayList<>();

    for (final BatchSaveResult.Item<E> item : sql.saveEntitiesCommand(entities)
        .setMode(SaveMode.NON_IDEMPOTENT_UPSERT)
        .execute()
        .getItems()) {

      saved.add(item.getModifiedEntity());
    }
    return List.copyOf(saved);
  }

  public int deleteById(final ID id) {
    return sql.deleteById(entityType, id).getTotalAffectedRowCount();
  }

  public int deleteByIds(final Iterable<ID> ids) {
    return sql.deleteByIds(entityType, ids).getTotalAffectedRowCount();
  }

  public boolean existsById(final ID id) {
    return findById(id) != null;
  }
}
