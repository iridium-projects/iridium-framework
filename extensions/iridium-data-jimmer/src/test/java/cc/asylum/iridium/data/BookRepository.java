package cc.asylum.iridium.data;

import cc.asylum.iridium.data.repository.SpecRepository;
import org.babyfish.jimmer.sql.JSqlClient;

final class BookRepository extends SpecRepository<Book, Long> {

  BookRepository(final JSqlClient sql) {
    super(sql, Book.class, BookTable.$);
  }
}
