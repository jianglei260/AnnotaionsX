package cn.scewin.ormplus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldGenerate {
    String name();

    Class type();

    boolean ignore() default false;

    boolean primaryKey() default false;

    boolean autoGenerate() default false;
}
