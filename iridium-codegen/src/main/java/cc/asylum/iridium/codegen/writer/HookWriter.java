package cc.asylum.iridium.codegen.writer;

import com.io7m.jodist.ClassName;
import com.io7m.jodist.MethodSpec;
import com.io7m.jodist.TypeName;
import com.io7m.jodist.TypeSpec;
import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.hook.ShutdownHook;

import javax.lang.model.element.Modifier;

@Internal
public final class HookWriter {

  private HookWriter() {
  }

  public static TypeSpec shutdownHook(final ClassName enclosingType, final String methodName, final int priority) {
    return TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(ClassName.get(ShutdownHook.class))
        .addMethod(MethodSpec.methodBuilder("run")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addStatement("new $T().$N()", enclosingType, methodName)
            .build())
        .addMethod(MethodSpec.methodBuilder("priority")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(TypeName.INT)
            .addStatement("return $L", priority)
            .build())
        .build();
  }
}
