package cc.asylum.iridium.data;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.GenerationType;
import org.babyfish.jimmer.sql.Id;

import java.math.BigDecimal;

@Entity
public interface Book {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  long id();

  String name();

  BigDecimal price();
}
