package com.medibook.api.dto.validation;

import com.medibook.api.dto.Auth.RegisterRequestDTO;
import com.medibook.api.dto.Auth.SignInRequestDTO;
import com.medibook.api.dto.ProfileUpdateRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DTOValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // REGISTER REQUEST DTO VALIDATION TESTS
    @Test
    void registerRequestDTO_ValidPatient_NoViolations() {
        RegisterRequestDTO validRequest = new RegisterRequestDTO(
                "patient@test.com",
                12345678L,
                "Password123",  // Corregido para cumplir patrón
                "John",
                "Doe",
                "+1234567890",  // Corregido para cumplir patrón de teléfono
                LocalDate.of(1990, 1, 1),
                "MALE",
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(validRequest);
        assertTrue(violations.isEmpty());
    }

    @Test
    void registerRequestDTO_InvalidEmail_HasViolations() {
        RegisterRequestDTO invalidRequest = new RegisterRequestDTO(
                "invalid-email", // Invalid email format
                12345678L,
                "password123",
                "John",
                "Doe",
                "1234567890",
                LocalDate.of(1990, 1, 1),
                "MALE",
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(invalidRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void registerRequestDTO_NullEmail_HasViolations() {
        RegisterRequestDTO nullEmailRequest = new RegisterRequestDTO(
                null, // Null email
                12345678L,
                "password123",
                "John",
                "Doe",
                "1234567890",
                LocalDate.of(1990, 1, 1),
                "MALE",
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(nullEmailRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void registerRequestDTO_EmptyEmail_HasViolations() {
        RegisterRequestDTO emptyEmailRequest = new RegisterRequestDTO(
                "", // Empty email
                12345678L,
                "password123",
                "John",
                "Doe",
                "1234567890",
                LocalDate.of(1990, 1, 1),
                "MALE",
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(emptyEmailRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void registerRequestDTO_ShortPassword_HasViolations() {
        RegisterRequestDTO shortPasswordRequest = new RegisterRequestDTO(
                "patient@test.com",
                12345678L,
                "short", // Less than 8 characters
                "John",
                "Doe",
                "1234567890",
                LocalDate.of(1990, 1, 1),
                "MALE",
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(shortPasswordRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void registerRequestDTO_NullPassword_HasViolations() {
        RegisterRequestDTO nullPasswordRequest = new RegisterRequestDTO(
                "patient@test.com",
                12345678L,
                null, // Null password
                "John",
                "Doe",
                "1234567890",
                LocalDate.of(1990, 1, 1),
                "MALE",
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(nullPasswordRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void registerRequestDTO_NullName_HasViolations() {
        RegisterRequestDTO nullNameRequest = new RegisterRequestDTO(
                "patient@test.com",
                12345678L,
                "password123",
                null, // Null name
                "Doe",
                "1234567890",
                LocalDate.of(1990, 1, 1),
                "MALE",
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(nullNameRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void registerRequestDTO_EmptyName_HasViolations() {
        RegisterRequestDTO emptyNameRequest = new RegisterRequestDTO(
                "patient@test.com",
                12345678L,
                "password123",
                "", // Empty name
                "Doe",
                "1234567890",
                LocalDate.of(1990, 1, 1),
                "MALE",
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(emptyNameRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void registerRequestDTO_NullSurname_HasViolations() {
        RegisterRequestDTO nullSurnameRequest = new RegisterRequestDTO(
                "patient@test.com",
                12345678L,
                "password123",
                "John",
                null, // Null surname
                "1234567890",
                LocalDate.of(1990, 1, 1),
                "MALE",
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(nullSurnameRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("surname")));
    }

    @Test
    void registerRequestDTO_NullDni_HasViolations() {
        RegisterRequestDTO nullDniRequest = new RegisterRequestDTO(
                "patient@test.com",
                null, // Null DNI
                "password123",
                "John",
                "Doe",
                "1234567890",
                LocalDate.of(1990, 1, 1),
                "MALE",
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(nullDniRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("dni")));
    }

    @Test
    void registerRequestDTO_InvalidDni_HasViolations() {
        RegisterRequestDTO invalidDniRequest = new RegisterRequestDTO(
                "patient@test.com",
                123L, // DNI too short
                "password123",
                "John",
                "Doe",
                "1234567890",
                LocalDate.of(1990, 1, 1),
                "MALE",
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(invalidDniRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("dni")));
    }

    @Test
    void registerRequestDTO_FutureBirthdate_HasViolations() {
        RegisterRequestDTO futureBirthdateRequest = new RegisterRequestDTO(
                "patient@test.com",
                12345678L,
                "password123",
                "John",
                "Doe",
                "1234567890",
                LocalDate.now().plusDays(1), // Future birthdate
                "MALE",
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(futureBirthdateRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("birthdate")));
    }

    @Test
    void registerRequestDTO_InvalidGender_HasViolations() {
        RegisterRequestDTO invalidGenderRequest = new RegisterRequestDTO(
                "patient@test.com",
                12345678L,
                "password123",
                "John",
                "Doe",
                "1234567890",
                LocalDate.of(1990, 1, 1),
                "INVALID_GENDER", // Invalid gender
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(invalidGenderRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("gender")));
    }

    // SIGNIN REQUEST DTO VALIDATION TESTS
    @Test
    void signInRequestDTO_ValidCredentials_NoViolations() {
        SignInRequestDTO validRequest = new SignInRequestDTO(
                "user@test.com",
                "password123"
        );

        Set<ConstraintViolation<SignInRequestDTO>> violations = validator.validate(validRequest);
        assertTrue(violations.isEmpty());
    }

    @Test
    void signInRequestDTO_InvalidEmail_HasViolations() {
        SignInRequestDTO invalidRequest = new SignInRequestDTO(
                "invalid-email", // Invalid email format
                "password123"
        );

        Set<ConstraintViolation<SignInRequestDTO>> violations = validator.validate(invalidRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void signInRequestDTO_NullEmail_HasViolations() {
        SignInRequestDTO nullEmailRequest = new SignInRequestDTO(
                null, // Null email
                "password123"
        );

        Set<ConstraintViolation<SignInRequestDTO>> violations = validator.validate(nullEmailRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void signInRequestDTO_NullPassword_HasViolations() {
        SignInRequestDTO nullPasswordRequest = new SignInRequestDTO(
                "user@test.com",
                null // Null password
        );

        Set<ConstraintViolation<SignInRequestDTO>> violations = validator.validate(nullPasswordRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void signInRequestDTO_EmptyPassword_HasViolations() {
        SignInRequestDTO emptyPasswordRequest = new SignInRequestDTO(
                "user@test.com",
                "" // Empty password
        );

        Set<ConstraintViolation<SignInRequestDTO>> violations = validator.validate(emptyPasswordRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    // PROFILE UPDATE REQUEST DTO VALIDATION TESTS
    @Test
    void profileUpdateRequestDTO_ValidData_NoViolations() {
        ProfileUpdateRequestDTO validRequest = new ProfileUpdateRequestDTO(
                "user@test.com",
                "John",
                "Doe",
                "1234567890",
                LocalDate.of(1990, 1, 1),
                "MALE",
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<ProfileUpdateRequestDTO>> violations = validator.validate(validRequest);
        assertTrue(violations.isEmpty());
    }

    @Test
    void profileUpdateRequestDTO_InvalidEmail_HasViolations() {
        ProfileUpdateRequestDTO invalidRequest = new ProfileUpdateRequestDTO(
                "invalid-email", // Invalid email format
                "John",
                "Doe",
                "1234567890",
                LocalDate.of(1990, 1, 1),
                "MALE",
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<ProfileUpdateRequestDTO>> violations = validator.validate(invalidRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void profileUpdateRequestDTO_EmptyName_HasViolations() {
        ProfileUpdateRequestDTO emptyNameRequest = new ProfileUpdateRequestDTO(
                "user@test.com",
                "", // Empty name
                "Doe",
                "1234567890",
                LocalDate.of(1990, 1, 1),
                "MALE",
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<ProfileUpdateRequestDTO>> violations = validator.validate(emptyNameRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void profileUpdateRequestDTO_EmptySurname_HasViolations() {
        ProfileUpdateRequestDTO emptySurnameRequest = new ProfileUpdateRequestDTO(
                "user@test.com",
                "John",
                "", // Empty surname
                "1234567890",
                LocalDate.of(1990, 1, 1),
                "MALE",
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<ProfileUpdateRequestDTO>> violations = validator.validate(emptySurnameRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("surname")));
    }

    // EDGE CASES AND SECURITY TESTS
    @Test
    void registerRequestDTO_XSSAttempt_HandledSafely() {
        RegisterRequestDTO xssRequest = new RegisterRequestDTO(
                "user@test.com",
                12345678L,
                "Password123",  // Corregido para cumplir patrón
                "<script>alert('xss')</script>", // XSS attempt in name
                "Doe",
                "+1234567890",  // Corregido para cumplir patrón de teléfono
                LocalDate.of(1990, 1, 1),
                "MALE",
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(xssRequest);
        // Debería tener violaciones por el nombre con caracteres no permitidos
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Name can only contain letters and spaces")));
    }

    @Test
    void registerRequestDTO_SQLInjectionAttempt_HandledSafely() {
        RegisterRequestDTO sqlInjectionRequest = new RegisterRequestDTO(
                "'; DROP TABLE users; --@test.com",
                12345678L,
                "password123",
                "'; DROP TABLE users; --",
                "Smith",
                "1234567890",
                LocalDate.of(1990, 1, 1),
                "MALE",
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(sqlInjectionRequest);
        // Should have violations due to invalid email format
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void registerRequestDTO_ExcessiveLength_HandledAppropriately() {
        String longString = "a".repeat(1000); // Very long string
        RegisterRequestDTO excessiveLengthRequest = new RegisterRequestDTO(
                "user@test.com",
                12345678L,
                "Password123",  // Corregido para cumplir patrón
                longString,
                "Doe",
                "+1234567890",  // Corregido para cumplir patrón de teléfono
                LocalDate.of(1990, 1, 1),
                "MALE",
                null,
                null,
                null, longString, longString
        );

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(excessiveLengthRequest);
        // Debería tener violaciones por el nombre demasiado largo (máximo 50 caracteres)
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Name must be between 2 and 50 characters")));
    }

    @Test
    void registerRequestDTO_MinimumValidValues_NoViolations() {
        RegisterRequestDTO minRequest = new RegisterRequestDTO(
                "a@b.co", // Minimum valid email
                1000000L, // Minimum valid DNI
                "Password1", // Mínimo password que cumple patrón (mayúscula, minúscula, número)
                "Jo", // Mínimo 2 caracteres para name
                "Do", // Mínimo 2 caracteres para surname
                "+12345678", // Phone obligatorio, mínimo 8 dígitos
                LocalDate.of(1900, 1, 1), // Birthdate obligatorio
                "MALE", // Gender obligatorio, debe ser MALE o FEMALE
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(minRequest);
        assertTrue(violations.isEmpty());
    }

    @Test
    void registerRequestDTO_LeapYearBirthdate_NoViolations() {
        RegisterRequestDTO leapYearRequest = new RegisterRequestDTO(
                "user@test.com",
                12345678L,
                "Password123",  // Corregido para cumplir patrón
                "John",
                "Doe",
                "+1234567890",  // Corregido para cumplir patrón de teléfono
                LocalDate.of(2000, 2, 29), // Leap year date
                "MALE",
                null,
                null,
                null, null, null
        );

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(leapYearRequest);
        assertTrue(violations.isEmpty());
    }
}