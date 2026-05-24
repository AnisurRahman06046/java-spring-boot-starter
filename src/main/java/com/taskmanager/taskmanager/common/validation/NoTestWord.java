package com.taskmanager.taskmanager.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;


@Documented
@Constraint(validatedBy=NoTestWordValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

public @interface NoTestWord {
    String message() default  "Title must not contain the word 'test'";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
