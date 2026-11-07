package com.aistudymentor.auth.dto.validation;

import com.aistudymentor.auth.dto.RegisterRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;

public class AgeAndGuardianEmailValidator implements ConstraintValidator<ValidAgeAndGuardianEmail, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        // 1. Password byte length check (BCrypt hard limit is 72 bytes)
        if (request.getPassword() != null) {
            byte[] passwordBytes = request.getPassword().getBytes(StandardCharsets.UTF_8);
            if (passwordBytes.length > 72) {
                context.buildConstraintViolationWithTemplate("Password exceeds maximum length of 72 bytes")
                        .addPropertyNode("password")
                        .addConstraintViolation();
                valid = false;
            }
        }

        // 2. Age and Guardian Email validation (Decision D9: U13 calculation)
        if (request.getDob() != null) {
            int age = Period.between(request.getDob(), LocalDate.now()).getYears();
            if (age < 13) {
                if (!StringUtils.hasText(request.getGuardianEmail())) {
                    context.buildConstraintViolationWithTemplate("Guardian email is required for users under 13 years old")
                            .addPropertyNode("guardianEmail")
                            .addConstraintViolation();
                    valid = false;
                } else if (request.getEmail() != null && request.getGuardianEmail().trim().equalsIgnoreCase(request.getEmail().trim())) {
                    context.buildConstraintViolationWithTemplate("Guardian email must be different from student email")
                            .addPropertyNode("guardianEmail")
                            .addConstraintViolation();
                    valid = false;
                }
            }
        }

        return valid;
    }
}
