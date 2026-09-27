package com.securehook.validation;

import com.securehook.model.EventType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SingleEventTypeValidator implements ConstraintValidator<ValidEventType, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // Let @NotBlank handle empty validation if needed
        }
        return EventType.isValid(value);
    }
}
