package com.securehook.validation;

import com.securehook.model.EventType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

public class EventTypeValidator implements ConstraintValidator<ValidEventType, Set<String>> {

    @Override
    public boolean isValid(Set<String> values, ConstraintValidatorContext context) {
        if (values == null || values.isEmpty()) {
            return true; // @NotEmpty handles empty validation
        }
        for (String value : values) {
            if (!EventType.isValid(value)) {
                return false;
            }
        }
        return true;
    }
}
