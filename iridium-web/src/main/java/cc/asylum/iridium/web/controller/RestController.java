package cc.asylum.iridium.web.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark rest controllers. Makes controller class a dependency
 * injection target.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface RestController {

  /**
   * Route prefix all controller methods append to
   */
  String value() default "";
}
