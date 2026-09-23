package cc.asylum.iridium.web.controller.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@HttpMapping(method = "HEAD")
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface HEAD {

  String value() default "";
}
