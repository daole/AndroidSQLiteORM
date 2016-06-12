package com.dreamdigitizers.androidsqliteorm.annotations;

import com.dreamdigitizers.androidsqliteorm.FetchType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface OneToMany {
    boolean isOptional() default false;
    Class<?> detailTableClass() default void.class;
    String detailColumnFieldName() default "";
    FetchType fetchType() default FetchType.LAZY;
}
