package com.medibook.api.mapper;

import com.medibook.api.dto.Auth.RegisterRequestDTO;
import com.medibook.api.dto.Auth.RegisterResponseDTO;
import com.medibook.api.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void whenMapPatientRequestToUser_thenSuccess() {
        RegisterRequestDTO dto = new RegisterRequestDTO(
            "test@example.com",
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

        User user = mapper.toUser(dto, "PATIENT", "hashed_password");

        assertEquals(dto.email(), user.getEmail());
        assertEquals(dto.dni(), user.getDni());
        assertEquals("hashed_password", user.getPasswordHash());
        assertEquals(dto.name(), user.getName());
        assertEquals(dto.surname(), user.getSurname());
        assertEquals(dto.phone(), user.getPhone());
        assertEquals(dto.birthdate(), user.getBirthdate());
        assertEquals(dto.gender(), user.getGender());
        assertEquals("PATIENT", user.getRole());
        assertEquals("ACTIVE", user.getStatus());
        assertNull(user.getDoctorProfile());
    }

    @Test
    void whenMapDoctorRequestToUser_thenSuccess() {
        RegisterRequestDTO dto = new RegisterRequestDTO(
            "doctor@example.com",
            87654321L,
            "password123",
            "John",
            "Doe",
            "+1234567890",
            LocalDate.of(1990, 1, 1),
            "MALE",
            "ML123",
            "Cardiology",
            30, null, null
        );

        User user = mapper.toUser(dto, "DOCTOR", "hashed_password");

        assertEquals(dto.email(), user.getEmail());
        assertEquals(dto.dni(), user.getDni());
        assertEquals("hashed_password", user.getPasswordHash());
        assertEquals(dto.name(), user.getName());
        assertEquals(dto.surname(), user.getSurname());
        assertEquals("DOCTOR", user.getRole());
        assertNotNull(user.getDoctorProfile());
        assertEquals(dto.medicalLicense(), user.getDoctorProfile().getMedicalLicense());
        assertEquals(dto.specialty(), user.getDoctorProfile().getSpecialty());
        assertEquals(dto.slotDurationMin(), user.getDoctorProfile().getSlotDurationMin());
    }

    @Test
    void whenMapUserToResponse_thenSuccess() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setName("John");
        user.setSurname("Doe");
        user.setRole("PATIENT");

        RegisterResponseDTO response = mapper.toRegisterResponse(user);

        assertEquals(user.getId(), response.id());
        assertEquals(user.getEmail(), response.email());
        assertEquals(user.getName(), response.name());
        assertEquals(user.getSurname(), response.surname());
        assertEquals(user.getRole(), response.role());
    }
}
