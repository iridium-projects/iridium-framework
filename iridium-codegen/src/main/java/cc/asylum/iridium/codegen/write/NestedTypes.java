package cc.asylum.iridium.codegen.write;

import cc.asylum.forgery.model.TypeRef;
import cc.asylum.forgery.type.ClassBuilder;

import java.util.function.Consumer;

public interface NestedTypes {

  TypeRef nest(final String name, final Consumer<ClassBuilder> configure);
}
