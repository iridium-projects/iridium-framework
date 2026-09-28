package cc.asylum.iridium.web.controller.parameter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@RequestBinding(BindingSource.PATH)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface PathVariable {

  String value() default "";
}
