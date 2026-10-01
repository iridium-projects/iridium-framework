package cc.asylum.iridium.codegen.write;

import cc.asylum.forgery.type.ClassBuilder;
import cc.asylum.iridium.codegen.Generated;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class SourceWriterTest {

  @Test
  void generatedClassAndNested() {
    final ClassBuilder type = new ClassBuilder("Widget");
    SourceWriter.generatedClass(type, builder -> builder.method("run", method -> method.public_()));
    final NestedTypes nested = SourceWriter.nested(type);
    assertEquals("Inner", ((cc.asylum.forgery.model.ClassRef) nested.nest("Inner", ClassBuilder::public_)).canonicalName());
    final String source = type.build().toString();
    assertTrue(source.contains(Generated.class.getCanonicalName()) || source.contains("Generated"));
  }

  @Test
  void writeJava() throws Exception {
    final Filer filer = mock(Filer.class);
    final JavaFileObject file = mock(JavaFileObject.class);
    final StringWriter writer = new StringWriter();
    when(filer.createSourceFile(eq("app.Widget"), any(Element[].class))).thenAnswer(invocation -> file);
    when(file.openWriter()).thenAnswer(invocation -> writer);
    final Element origin = mock(Element.class);
    SourceWriter.writeJava(filer, "app", "Widget", builder -> {
    }, origin);
    verify(filer).createSourceFile(eq("app.Widget"), any(Element[].class));
    assertTrue(writer.toString().contains("class Widget"));
  }

  @Test
  void writeJavaWrapsIoFailure() throws Exception {
    final Filer filer = mock(Filer.class);
    when(filer.createSourceFile(any(), any(Element[].class))).thenThrow(new IOException("disk"));
    final RuntimeException failure = assertThrows(RuntimeException.class, () -> SourceWriter.writeJava(filer, "app", "Widget", builder -> {
    }));
    assertEquals("Failed to generate app.Widget", failure.getMessage());
    assertInstanceOf(IOException.class, failure.getCause());
  }

  @Test
  void writeService() throws Exception {
    final Filer filer = mock(Filer.class);
    final FileObject file = mock(FileObject.class);
    final StringWriter writer = new StringWriter();
    when(filer.createResource(eq(StandardLocation.CLASS_OUTPUT), eq(""), eq("META-INF/services/java.lang.Runnable"), any(Element[].class))).thenAnswer(invocation -> file);
    when(file.openWriter()).thenAnswer(invocation -> writer);
    SourceWriter.writeService(filer, Runnable.class, "app.Task");
    SourceWriter.writeService(filer, "java.lang.Runnable", "app.Task");
    assertTrue(writer.toString().contains("app.Task"));
  }

  @Test
  void writeServiceWrapsIoFailure() throws Exception {
    final Filer filer = mock(Filer.class);
    final FileObject file = mock(FileObject.class);
    final Writer writer = mock(Writer.class);
    when(filer.createResource(any(), any(), any(), any(Element[].class))).thenAnswer(invocation -> file);
    when(file.openWriter()).thenAnswer(invocation -> writer);
    org.mockito.Mockito.doThrow(new IOException("full")).when(writer).write(any(String.class));
    final RuntimeException failure = assertThrows(RuntimeException.class, () -> SourceWriter.writeService(filer, "java.lang.Runnable", "app.Task"));
    assertEquals("Failed to write service file for java.lang.Runnable", failure.getMessage());
  }
}
