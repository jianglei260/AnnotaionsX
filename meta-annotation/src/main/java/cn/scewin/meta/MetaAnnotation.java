package cn.scewin.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MetaAnnotation {
    Class[] children() default {};

    Class entityClass() default void.class;

    boolean buildEntityClass() default true;

    boolean buildConstants() default true;

    boolean buildRefs() default false;

    String entitySuffix() default "Info";

}