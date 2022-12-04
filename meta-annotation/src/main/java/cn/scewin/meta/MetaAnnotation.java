package cn.scewin.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MetaAnnotation {
    Class[] children() default {};

    boolean buildEntityClass() default true;

    boolean buildConstants() default true;

    String entitySuffix() default "Info";

}