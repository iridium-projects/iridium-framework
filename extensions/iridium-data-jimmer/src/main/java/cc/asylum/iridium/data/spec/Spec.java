package cc.asylum.iridium.data.spec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Repeatable(And.class)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
public @interface Spec {

  String path();

  String[] params() default {};

  String[] headers() default {};

  String constVal() default "";

  String defaultVal() default "";

  Class<? extends Op> spec();

  boolean not() default false;
}
