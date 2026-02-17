package com.medibook.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibook.api.dto.Turn.TurnCreateRequestDTO;
import com.medibook.api.entity.User;
import com.medibook.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TurnAssignedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String patientToken;
    private String doctorToken;
    private String adminToken;
    private User patient;
    private User otherPatient;
    private User doctor;
    private User admin;

    @BeforeEach
    void setUp() throws Exception {
        patient = createTestPatient();
        otherPatient = createAnotherTestPatient();
        doctor = createTestDoctor();
        admin = createTestAdmin();

        patientToken = getAuthToken(patient.getEmail(), "password123");
        doctorToken = getAuthToken(doctor.getEmail(), "password123");
        adminToken = getAuthToken(admin.getEmail(), "password123");
    }

    // Test 1: createTurn_AsPatient_Success - problema de estado (PENDING vs SCHEDULED)
    @Test
    void createTurn_AsPatient_Success() throws Exception {
        TurnCreateRequestDTO createRequest = new TurnCreateRequestDTO();
        createRequest.setDoctorId(doctor.getId());
        createRequest.setPatientId(patient.getId());
        createRequest.setScheduledAt(OffsetDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/turns")
                .header("Authorization", "Bearer " + patientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.doctorId").value(doctor.getId().toString()))
                .andExpect(jsonPath("$.patientId").value(patient.getId().toString()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));  // Ahora es SCHEDULED
    }

    // Test 2: createTurn_AsDoctor_Forbidden - problema de autorización (era 201 Created en lugar de 403 Forbidden)
    @Test
    void createTurn_AsDoctor_Forbidden() throws Exception {
        TurnCreateRequestDTO createRequest = new TurnCreateRequestDTO();
        createRequest.setDoctorId(doctor.getId());
        createRequest.setPatientId(patient.getId());
        createRequest.setScheduledAt(OffsetDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/turns")
                .header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());  // Era 201 Created
    }

    @Test
    void createTurn_AsAdmin_Forbidden() throws Exception {
        TurnCreateRequestDTO createRequest = new TurnCreateRequestDTO();
        createRequest.setDoctorId(doctor.getId());
        createRequest.setPatientId(patient.getId());
        createRequest.setScheduledAt(OffsetDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/turns")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only patients can create turns"));
    }

    @Test
    void createTurn_PatientCreatingForAnotherPatient_Forbidden() throws Exception {
        TurnCreateRequestDTO createRequest = new TurnCreateRequestDTO();
        createRequest.setDoctorId(doctor.getId());
        createRequest.setPatientId(otherPatient.getId());
        createRequest.setScheduledAt(OffsetDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/turns")
                .header("Authorization", "Bearer " + patientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden())
            .andExpect(content().string("Patients can only create turns for themselves"));
    }

    // Test 3: createTurn_WithPastDate_BadRequest - problema de validación (era 201 Created en lugar de 400 Bad Request)
    @Test
    void createTurn_WithPastDate_BadRequest() throws Exception {
        TurnCreateRequestDTO createRequest = new TurnCreateRequestDTO();
        createRequest.setDoctorId(doctor.getId());
        createRequest.setPatientId(patient.getId());
        createRequest.setScheduledAt(OffsetDateTime.now().minusDays(1));

        mockMvc.perform(post("/api/turns")
                .header("Authorization", "Bearer " + patientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());  // Era 201 Created
    }

    // HELPER METHODS
    private User createTestPatient() {
        User patient = new User();
        patient.setEmail("patient@example.com");
        patient.setDni(12345678L);
        patient.setPasswordHash(passwordEncoder.encode("password123"));
        patient.setName("John");
        patient.setSurname("Doe");
        patient.setPhone("1234567890");
        patient.setBirthdate(LocalDate.of(1990, 1, 1));
        patient.setGender("MALE");
        patient.setRole("PATIENT");
        patient.setStatus("ACTIVE");
        patient.setEmailVerified(true);
        return userRepository.save(patient);
    }

    private User createTestDoctor() {
        User doctor = new User();
        doctor.setEmail("doctor@example.com");
        doctor.setDni(87654321L);
        doctor.setPasswordHash(passwordEncoder.encode("password123"));
        doctor.setName("Jane");
        doctor.setSurname("Smith");
        doctor.setPhone("0987654321");
        doctor.setBirthdate(LocalDate.of(1985, 5, 15));
        doctor.setGender("FEMALE");
        doctor.setRole("DOCTOR");
        doctor.setStatus("ACTIVE");
        doctor.setEmailVerified(true);
        return userRepository.save(doctor);
    }

    private User createAnotherTestPatient() {
        User otherPatient = new User();
        otherPatient.setEmail("patient2@example.com");
        otherPatient.setDni(22345678L);
        otherPatient.setPasswordHash(passwordEncoder.encode("password123"));
        otherPatient.setName("Ana");
        otherPatient.setSurname("Perez");
        otherPatient.setPhone("1234500000");
        otherPatient.setBirthdate(LocalDate.of(1992, 2, 2));
        otherPatient.setGender("FEMALE");
        otherPatient.setRole("PATIENT");
        otherPatient.setStatus("ACTIVE");
        otherPatient.setEmailVerified(true);
        return userRepository.save(otherPatient);
    }

    private User createTestAdmin() {
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setDni(11223344L);
        admin.setPasswordHash(passwordEncoder.encode("password123"));
        admin.setName("Admin");
        admin.setSurname("User");
        admin.setPhone("1111111111");
        admin.setBirthdate(LocalDate.of(1980, 1, 1));
        admin.setGender("OTHER");
        admin.setRole("ADMIN");
        admin.setStatus("ACTIVE");
        admin.setEmailVerified(true);
        return userRepository.save(admin);
    }

    private String getAuthToken(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var jsonNode = objectMapper.readTree(response);
        if (jsonNode.get("accessToken") == null) {
            throw new RuntimeException("AccessToken not found in response: " + response);
        }
        
        return jsonNode.get("accessToken").asText();
    }
}