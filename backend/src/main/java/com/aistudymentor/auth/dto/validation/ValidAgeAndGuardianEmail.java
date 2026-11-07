package com.aistudymentor.auth.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = AgeAndGuardianEmailValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAgeAndGuardianEmail {
    String message() default "Invalid registration details for age or guardian email";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
