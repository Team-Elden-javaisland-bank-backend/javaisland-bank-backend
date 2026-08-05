package com.javaisland.bank_backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AdultValidator.class)
public @interface Adult {
    String message() default "You must be of legal age to register.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
