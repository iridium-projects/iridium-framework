package cc.asylum.iridium.codegen.write;

import cc.asylum.forgery.expr.Expr;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class RegistrarTest {

  @Test
  void buildsAndWrites() throws Exception {
    final Registrar registrar = Registrar.of("AppRegistrar", Runnable.class, Object.class, "pool");
    assertTrue(registrar.empty());
    assertNotNull(registrar.pool());
    registrar.put("widget", Expr.nil()).line(Expr.lit(1)).nest(types -> types.nest("Inner", builder -> {
    }));
    assertFalse(registrar.empty());

    final Filer filer = mock(Filer.class);
    final JavaFileObject java = mock(JavaFileObject.class);
    final FileObject service = mock(FileObject.class);
    when(filer.createSourceFile(eq("app.AppRegistrar"), any(Element[].class))).thenAnswer(invocation -> java);
    when(java.openWriter()).thenAnswer(invocation -> new StringWriter());
    when(filer.createResource(eq(StandardLocation.CLASS_OUTPUT), eq(""), eq("META-INF/services/java.lang.Runnable"), any(Element[].class))).thenAnswer(invocation -> service);
    when(service.openWriter()).thenAnswer(invocation -> new StringWriter());
    registrar.write(filer, "app", mock(Element.class));
  }
}
