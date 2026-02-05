package com.ticketblitz.user.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserRegistrationRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Validation: Should pass with valid data")
    void validation_ValidData() {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setUsername("validUser123");
        req.setEmail("valid@test.com");
        req.setPassword("ValidPass1!");

        Set<ConstraintViolation<UserRegistrationRequest>> violations = validator.validate(req);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Validation: Should fail when username is blank")
    void validation_BlankUsername() {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setUsername("");
        req.setEmail("valid@test.com");
        req.setPassword("ValidPass1!");

        Set<ConstraintViolation<UserRegistrationRequest>> violations = validator.validate(req);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Validation: Should fail when password is too short")
    void validation_ShortPassword() {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setUsername("validUser");
        req.setEmail("valid@test.com");
        req.setPassword("Short1!");

        Set<ConstraintViolation<UserRegistrationRequest>> violations = validator.validate(req);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Validation: Should fail when email is invalid")
    void validation_InvalidEmail() {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setUsername("validUser");
        req.setEmail("notanemail");
        req.setPassword("ValidPass1!");

        Set<ConstraintViolation<UserRegistrationRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }
}
