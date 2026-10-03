package cc.asylum.iridium.core.annotation.processor;

import cc.asylum.iridium.codegen.Generated;
import cc.asylum.iridium.core.processor.Mirrors;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class InternalUsageProcessorTest {

  @Test
  void warnsOnExternalInternalUsage() {
    final ProcessingEnvironment environment = environment("com.example");
    final Trees trees = mock(Trees.class);
    final InternalUsageProcessor processor = processor(environment, trees);
    assertEquals(SourceVersion.latestSupported(), processor.getSupportedSourceVersion());
    assertEquals(Set.of("*"), processor.getSupportedAnnotationTypes());

    final Element root = mock(Element.class);
    when(root.getAnnotation(Generated.class)).thenAnswer(invocation -> null);
    final TreePath path = parsed("package com.example; class Use { cc.asylum.iridium.core.annotation.Internal internal; }");
    when(trees.getPath(root)).thenAnswer(invocation -> path);
    final TypeElement internal = mock(TypeElement.class);
    when(internal.getQualifiedName()).thenAnswer(invocation -> Mirrors.name("cc.asylum.iridium.core.annotation.Internal"));
    when(internal.getAnnotation(cc.asylum.iridium.core.annotation.Internal.class)).thenAnswer(invocation -> internalMarker());
    when(trees.getElement(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> internal);

    final RoundEnvironment round = mock(RoundEnvironment.class);
    when(round.processingOver()).thenAnswer(invocation -> false);
    when(round.getRootElements()).thenAnswer(invocation -> Set.of(root));
    assertFalse(processor.process(Set.of(), round));
    verify(environment.getMessager()).printMessage(
      org.mockito.ArgumentMatchers.eq(Diagnostic.Kind.WARNING),
      org.mockito.ArgumentMatchers.contains("internal type"),
      org.mockito.ArgumentMatchers.eq(root));
  }

  @Test
  void skipsAllowedGeneratedMissingAndFinishedRounds() {
    final ProcessingEnvironment environment = environment("cc.asylum.iridium.other");
    final Trees trees = mock(Trees.class);
    final InternalUsageProcessor processor = processor(environment, trees);
    final Element allowed = mock(Element.class);
    when(trees.getPath(allowed)).thenAnswer(invocation -> parsed("package cc.asylum.iridium.other; class Local {}"));
    final Element generated = mock(Element.class);
    when(generated.getAnnotation(Generated.class)).thenAnswer(invocation -> generated());
    final Element missing = mock(Element.class);
    when(missing.getAnnotation(Generated.class)).thenAnswer(invocation -> null);
    when(trees.getPath(missing)).thenAnswer(invocation -> null);
    final RoundEnvironment round = mock(RoundEnvironment.class);
    when(round.processingOver()).thenAnswer(invocation -> false);
    when(round.getRootElements()).thenAnswer(invocation -> Set.of(allowed, generated, missing));
    assertFalse(processor.process(Set.of(), round));

    when(round.processingOver()).thenAnswer(invocation -> true);
    assertFalse(processor.process(Set.of(), round));

    final InternalUsageProcessor unresolved = new InternalUsageProcessor();
    unresolved.init(mock(ProcessingEnvironment.class));
    assertFalse(unresolved.process(Set.of(), round));
  }

  @Test
  void unwrapsDelegatesUntilTreesResolve() {
    final ProcessingEnvironment inner = environment("com.example");
    final ProcessingEnvironment middle = new Delegating(inner);
    final ProcessingEnvironment outer = new Delegating(middle);
    final Broken broken = new Broken(outer);
    final InternalUsageProcessor processor = new InternalUsageProcessor();
    processor.init(broken);
    final RoundEnvironment round = mock(RoundEnvironment.class);
    when(round.processingOver()).thenAnswer(invocation -> true);
    assertFalse(processor.process(Set.of(), round));

    final InternalUsageProcessor stuck = new InternalUsageProcessor();
    final InternalUsageProcessor nullDelegate = new InternalUsageProcessor();
    nullDelegate.init(new Delegating(null));
    assertFalse(nullDelegate.process(Set.of(), round));
    final InternalUsageProcessor sealed = new InternalUsageProcessor();
    sealed.init(new Sealed("delegate"));
    assertFalse(sealed.process(Set.of(), round));
    final InternalUsageProcessor plain = new InternalUsageProcessor();
    plain.init(new Plain());
    assertFalse(plain.process(Set.of(), round));
  }

  private static TreePath parsed(final String source) {
    final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    final JavaFileObject file = new SimpleJavaFileObject(URI.create("string:///Use.java"), JavaFileObject.Kind.SOURCE) {
      @Override
      public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
        return source;
      }
    };
    final JavacTask task = (JavacTask) compiler.getTask(null, null, null, java.util.List.of(), null, java.util.List.of(file));
    try {
      final CompilationUnitTree unit = task.parse().iterator().next();
      return new TreePath(unit);
    } catch (final java.io.IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static InternalUsageProcessor processor(final ProcessingEnvironment environment, final Trees trees) {
    final InternalUsageProcessor processor = new InternalUsageProcessor();
    processor.init(environment);
    try {
      final Field field = InternalUsageProcessor.class.getDeclaredField("trees");
      field.setAccessible(true);
      field.set(processor, trees);
    } catch (final ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
    return processor;
  }

  private static ProcessingEnvironment environment(final String pkg) {
    final ProcessingEnvironment environment = mock(ProcessingEnvironment.class);
    final Elements elements = mock(Elements.class);
    final Messager messager = mock(Messager.class);
    when(environment.getElementUtils()).thenAnswer(invocation -> elements);
    when(environment.getMessager()).thenAnswer(invocation -> messager);
    when(environment.getLocale()).thenAnswer(invocation -> Locale.ROOT);
    when(environment.getOptions()).thenAnswer(invocation -> Map.of());
    when(environment.getSourceVersion()).thenAnswer(invocation -> SourceVersion.latestSupported());
    when(elements.getPackageOf(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
      final PackageElement pack = mock(PackageElement.class);
      when(pack.getQualifiedName()).thenAnswer(ignored -> Mirrors.name(pkg));
      return pack;
    });
    return environment;
  }

  private static cc.asylum.iridium.core.annotation.Internal internalMarker() {
    return new cc.asylum.iridium.core.annotation.Internal() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return cc.asylum.iridium.core.annotation.Internal.class;
      }
    };
  }

  private static Generated generated() {
    return new Generated() {
      @Override
      public String author() {
        return "";
      }

      @Override
      public String date() {
        return "";
      }

      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Generated.class;
      }
    };
  }

  private static class Delegating implements ProcessingEnvironment {
    private final ProcessingEnvironment delegate;

    private Delegating(final ProcessingEnvironment delegate) {
      this.delegate = delegate;
    }

    @Override
    public Map<String, String> getOptions() {
      return Map.of();
    }

    @Override
    public Messager getMessager() {
      return mock(Messager.class);
    }

    @Override
    public javax.annotation.processing.Filer getFiler() {
      return mock(javax.annotation.processing.Filer.class);
    }

    @Override
    public Elements getElementUtils() {
      return mock(Elements.class);
    }

    @Override
    public javax.lang.model.util.Types getTypeUtils() {
      return mock(javax.lang.model.util.Types.class);
    }

    @Override
    public SourceVersion getSourceVersion() {
      return SourceVersion.latestSupported();
    }

    @Override
    public Locale getLocale() {
      return Locale.ROOT;
    }
  }

  private static final class Broken extends Delegating {
    private Broken(final ProcessingEnvironment delegate) {
      super(delegate);
    }
  }

  private static final class Sealed implements ProcessingEnvironment {
    @SuppressWarnings("unused")
    private final String delegate;

    private Sealed(final String delegate) {
      this.delegate = delegate;
    }

    @Override
    public Map<String, String> getOptions() {
      return Map.of();
    }

    @Override
    public Messager getMessager() {
      return mock(Messager.class);
    }

    @Override
    public javax.annotation.processing.Filer getFiler() {
      return mock(javax.annotation.processing.Filer.class);
    }

    @Override
    public Elements getElementUtils() {
      return mock(Elements.class);
    }

    @Override
    public javax.lang.model.util.Types getTypeUtils() {
      return mock(javax.lang.model.util.Types.class);
    }

    @Override
    public SourceVersion getSourceVersion() {
      return SourceVersion.latestSupported();
    }

    @Override
    public Locale getLocale() {
      return Locale.ROOT;
    }
  }

  private static final class Plain implements ProcessingEnvironment {
    @Override
    public Map<String, String> getOptions() {
      return Map.of();
    }

    @Override
    public Messager getMessager() {
      return mock(Messager.class);
    }

    @Override
    public javax.annotation.processing.Filer getFiler() {
      return mock(javax.annotation.processing.Filer.class);
    }

    @Override
    public Elements getElementUtils() {
      return mock(Elements.class);
    }

    @Override
    public javax.lang.model.util.Types getTypeUtils() {
      return mock(javax.lang.model.util.Types.class);
    }

    @Override
    public SourceVersion getSourceVersion() {
      return SourceVersion.latestSupported();
    }

    @Override
    public Locale getLocale() {
      return Locale.ROOT;
    }
  }
}
