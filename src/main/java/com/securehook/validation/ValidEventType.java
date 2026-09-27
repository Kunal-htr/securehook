package com.securehook.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = {EventTypeValidator.class, SingleEventTypeValidator.class})
@Documented
public @interface ValidEventType {
    String message() default "contains one or more invalid event types. Allowed values are: order.created, order.placed, payment.failed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
