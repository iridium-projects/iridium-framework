package cc.asylum.iridium.data.spec;

import cc.asylum.iridium.codegen.ProcessorHarness;
import cc.asylum.iridium.codegen.processor.WebProcessor;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecBindingTest {

  @Test
  void generatesCompileTimeSpecBinding() throws Exception {
    final var result = ProcessorHarness.compile(Map.of(
        "test.Store", """
            package test;
            import org.babyfish.jimmer.sql.Entity;
            @Entity
            public interface Store {
              String name();
            }
            """,
        "test.Book", """
            package test;
            import org.babyfish.jimmer.sql.Entity;
            @Entity
            public interface Book {
              String name();
              java.math.BigDecimal price();
              Store store();
            }
            """,
        "test.BookTable", """
            package test;
            import org.babyfish.jimmer.sql.ast.PropExpression;
            public abstract class BookTable extends org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable<Book> {
              public BookTable() { super(Book.class); }
              public PropExpression.Str name() { return null; }
              public PropExpression.Num<java.math.BigDecimal> price() { return null; }
              public StoreTable store() { return null; }
              public StoreTable store(org.babyfish.jimmer.sql.JoinType joinType) { return null; }
              public static class StoreTable {
                public PropExpression.Str name() { return null; }
              }
            }
            """,
        "test.BooksController", """
            package test;
            import cc.asylum.iridium.data.spec.And;
            import cc.asylum.iridium.data.spec.Join;
            import cc.asylum.iridium.data.spec.JoinKind;
            import cc.asylum.iridium.data.spec.Op;
            import cc.asylum.iridium.data.spec.Spec;
            import cc.asylum.iridium.web.controller.RestController;
            import cc.asylum.iridium.web.controller.mapping.GET;
            import cc.asylum.iridium.web.response.Response;
            import org.babyfish.jimmer.sql.ast.query.specification.JSpecification;
            import java.util.List;
            @RestController
            public final class BooksController {
              @GET("/repeat")
              public Response<String> repeat(
                  @Spec(path = "name", spec = Op.StartingWith.class)
                  @Spec(path = "price", params = {"minPrice", "maxPrice"}, spec = Op.Between.class)
                  JSpecification<Book, ?> spec) {
                return Response.ok("ok");
              }
              @GET("/books")
              public Response<List<String>> books(
                  @And({
                    @Spec(path = "name", spec = Op.Like.class),
                    @Spec(path = "price", params = "minPrice", spec = Op.GreaterThanOrEqual.class),
                    @Spec(path = "s.name", params = "store", spec = Op.Equal.class)
                  })
                  @Join(path = "store", alias = "s", type = JoinKind.LEFT)
                  JSpecification<Book, ?> spec) {
                return Response.ok(List.of());
              }
            }
            """
    ), new WebProcessor());

    assertTrue(result.success(), () -> String.join("\n", result.errors()));
    final String registrar = ProcessorHarness.generatedSource(result, "test", "gen", "WebRegistrarGenerated.java");
    assertTrue(registrar.contains("likeIf"), registrar);
    assertTrue(registrar.contains("betweenIf"), registrar);
    assertTrue(registrar.contains("LikeMode.START"), registrar);
    assertTrue(registrar.contains("geIf"), registrar);
    assertTrue(registrar.contains("JoinType.LEFT"), registrar);
    assertTrue(registrar.contains("SpecValues.decimal"), registrar);
    assertFalse(registrar.contains("getDeclaredMethod"), registrar);
    assertFalse(registrar.contains("Class.forName"), registrar);
  }
}
