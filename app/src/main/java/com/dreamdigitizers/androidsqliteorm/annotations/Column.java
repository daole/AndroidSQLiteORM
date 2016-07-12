package com.dreamdigitizers.androidsqliteorm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Column {
    String name() default "";
    String defaultValue() default "";
    boolean nullable() default true;
    boolean unique() default false;
}
