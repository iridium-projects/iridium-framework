package cc.asylum.iridium.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configuration annotation used to configure the web application. Required by
 * {@link Iridium}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebApplication {

  /**
   * Host IP the application binds it's web server to
   */
  String host() default "0.0.0.0";

  /**
   * Port the application binds it's web server to
   */
  int port() default 8080;
}
