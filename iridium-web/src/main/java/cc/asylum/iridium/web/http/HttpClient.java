package cc.asylum.iridium.web.http;

import cc.asylum.iridium.codegen.Register;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Register(suffix = "Undertow")
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface HttpClient {

  String value();

  String url();
}
