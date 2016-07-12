package com.dreamdigitizers.androidsqliteorm.annotations;

import com.dreamdigitizers.androidsqliteorm.FetchType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface OneToOne {
    boolean optional() default false;
    Class<?> foreignTableClass() default void.class;
    String foreignColumnFieldName() default "";
    FetchType fetchType() default FetchType.LAZY;
}
