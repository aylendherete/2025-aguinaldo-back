package com.medibook.api.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.medibook.api.dto.Auth.RegisterRequestDTO;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestDTOTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void whenAllRequiredFieldsPresent_thenNoViolations() {
        RegisterRequestDTO dto = new RegisterRequestDTO(
            "test@example.com",
            12345678L,
            "Password123",  // Cambiado para cumplir con el patrón
            "John",
            "Doe",
            "+1234567890",
            LocalDate.of(1990, 1, 1),
            "MALE",
            null,
            null,
            null, null, null
        );

        var violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void whenEmailIsInvalid_thenViolation() {
        RegisterRequestDTO dto = new RegisterRequestDTO(
            "invalid-email",
            12345678L,
            "password123",
            "John",
            "Doe",
            "+1234567890",
            LocalDate.of(1990, 1, 1),
            "MALE",
            null,
            null,
            null, null, null
        );

        var violations = validator.validate(dto);
        assertEquals(2, violations.size());
        // Verificar que contiene los mensajes de error esperados
        var messages = violations.stream().map(violation -> violation.getMessage()).collect(java.util.stream.Collectors.toSet());
        assertTrue(messages.contains("Invalid email format"));
        assertTrue(messages.contains("Password must contain at least one lowercase letter, one uppercase letter, and one digit"));
    }

    @Test
    void whenPasswordTooShort_thenViolation() {
        RegisterRequestDTO dto = new RegisterRequestDTO(
            "test@example.com",
            12345678L,
            "short",
            "John",
            "Doe",
            "+1234567890",
            LocalDate.of(1990, 1, 1),
            "MALE",
            null,
            null,
            null, null, null
        );

        var violations = validator.validate(dto);
        assertEquals(2, violations.size());
        // Verificar que contiene los mensajes de error esperados
        var messages = violations.stream().map(violation -> violation.getMessage()).collect(java.util.stream.Collectors.toSet());
        assertTrue(messages.contains("Password must be between 8 and 128 characters"));
        assertTrue(messages.contains("Password must contain at least one lowercase letter, one uppercase letter, and one digit"));
    }

    @Test 
    void whenOptionalFieldsMissing_thenNoViolations() {
        RegisterRequestDTO dto = new RegisterRequestDTO(
            "test@example.com",
            12345678L,
            "Password123",  // Cambiado para cumplir con el patrón
            "John",
            "Doe",
            "+1234567890",  // Phone es obligatorio ahora
            LocalDate.of(1990, 1, 1),  // Birthdate es obligatorio ahora
            "MALE",  // Gender es obligatorio ahora
            null,  // medicalLicense sigue siendo opcional
            null,  // specialty sigue siendo opcional
            null   // slotDurationMin sigue siendo opcional
, null, null
        );

        var violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void whenSlotDurationIsInvalid_thenViolation() {
        RegisterRequestDTO dto = new RegisterRequestDTO(
            "test@example.com",
            12345678L,
            "Password123",  // Corregido para cumplir patrón
            "John",
            "Doe",
            "+1234567890",  // Agregado phone obligatorio
            LocalDate.of(1990, 1, 1),  // Agregado birthdate obligatorio
            "MALE",  // Agregado gender obligatorio
            "ML123",
            "Cardiology",
            -1, null, null
        );

        var violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertEquals("Slot duration must be at least 15 minutes", violations.iterator().next().getMessage());
    }

    @Test
    void whenDniIsNull_thenViolation() {
        RegisterRequestDTO dto = new RegisterRequestDTO(
            "test@example.com",
            null,
            "Password123",  // Corregido para cumplir patrón
            "John",
            "Doe",
            "+1234567890",  // Agregado phone obligatorio
            LocalDate.of(1990, 1, 1),  // Agregado birthdate obligatorio
            "MALE",  // Agregado gender obligatorio
            null,
            null,
            null, null, null
        );

        var violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertEquals("DNI is required", violations.iterator().next().getMessage());
    }

    @Test
    void whenDniTooShort_thenViolation() {
        RegisterRequestDTO dto = new RegisterRequestDTO(
            "test@example.com",
            123456L, // 6 digits, too short
            "Password123",  // Corregido para cumplir patrón
            "John",
            "Doe",
            "+1234567890",  // Agregado phone obligatorio
            LocalDate.of(1990, 1, 1),  // Agregado birthdate obligatorio
            "MALE",  // Agregado gender obligatorio
            null,
            null,
            null, null, null
        );

        var violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertEquals("DNI must be at least 7 digits", violations.iterator().next().getMessage());
    }

    @Test
    void whenDniTooLong_thenViolation() {
        RegisterRequestDTO dto = new RegisterRequestDTO(
            "test@example.com",
            1234567890L, // 10 digits, too long
            "Password123",  // Corregido para cumplir patrón
            "John",
            "Doe",
            "+1234567890",  // Agregado phone obligatorio
            LocalDate.of(1990, 1, 1),  // Agregado birthdate obligatorio
            "MALE",  // Agregado gender obligatorio
            null,
            null,
            null, null, null
        );

        var violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertEquals("DNI must be at most 9 digits", violations.iterator().next().getMessage());
    }
}