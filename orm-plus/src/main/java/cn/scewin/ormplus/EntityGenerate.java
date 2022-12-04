package cn.scewin.ormplus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EntityGenerate {
    String packageName();

    String entityName();

    String createSql() default "";

    Class[] entities() default {};

    String[] commonField() default {};

    String[] excludes() default {};

    FieldGenerate[] fields() default {};

}
