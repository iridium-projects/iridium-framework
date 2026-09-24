package cc.asylum.iridium.data.spec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Repeatable(Joins.class)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
public @interface Join {

  String path();

  String alias();

  JoinKind type() default JoinKind.INNER;
}
