package cc.asylum.iridium.data.repository;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.query.specification.JSpecification;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;

import java.util.List;

public abstract class SpecRepository<E, ID> extends CrudRepository<E, ID> {

  protected SpecRepository(
      final JSqlClient sql,
      final Class<E> entityType,
      final TableProxy<E> table
  ) {
    super(sql, entityType, table);
  }

  public List<E> findAll(final JSpecification<E, ?> specification) {
    return select(specification).execute();
  }

  public List<E> findAll(final JSpecification<E, ?> specification, final int limit, final long offset) {
    if (limit <= 0) {
      return findAll(specification);
    }

    return select(specification).limit(limit, Math.max(offset, 0L)).execute();
  }

  public E findOne(final JSpecification<E, ?> specification) {
    final List<E> rows = findAll(specification, 1, 0L);

    return rows.isEmpty() ? null : rows.get(0);
  }

  public long count(final JSpecification<E, ?> specification) {
    final MutableRootQuery<TableProxy<E>> query = sql.createQuery(table);

    if (specification != null) {
      query.where(specification);
    }

    final Long total = query.select(table.count()).fetchOne();
    return total == null ? 0L : total;
  }

  public Page<E> page(
    final JSpecification<E, ?> specification,
    final int page,
    final int size
  ) {
    if (page < 0 || size <= 0) {
      throw new IllegalArgumentException("page must be >= 0 and size must be > 0");
    }

    return new Page<>(
        findAll(specification, size, (long) page * size),
        page,
        size,
        count(specification)
    );
  }

  private ConfigurableRootQuery<TableProxy<E>, E> select(final JSpecification<E, ?> specification) {
    final MutableRootQuery<TableProxy<E>> query = sql.createQuery(table);

    if (specification != null) {
      query.where(specification);
    }

    return query.select(table);
  }
}
