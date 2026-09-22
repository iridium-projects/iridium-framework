package dev.yyuh.iridium.web.controller.parameter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface CookieValue {

  String value() default "";

  boolean required() default true;

  String defaultValue() default "";
}
