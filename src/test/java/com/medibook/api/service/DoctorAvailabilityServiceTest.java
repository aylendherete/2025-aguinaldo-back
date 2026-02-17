package com.medibook.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.medibook.api.dto.Availability.*;
import com.medibook.api.entity.DoctorProfile;
import com.medibook.api.entity.User;
import com.medibook.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorAvailabilityServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private BadgeEvaluationTriggerService badgeEvaluationTriggerService;

    @InjectMocks
    private DoctorAvailabilityService doctorAvailabilityService;

    private User doctorUser;
    private DoctorProfile doctorProfile;
    private DoctorAvailabilityRequestDTO testRequest;

    @BeforeEach
    void setUp() {
        doctorUser = new User();
        doctorUser.setId(UUID.randomUUID());
        doctorUser.setEmail("doctor@test.com");

        doctorProfile = new DoctorProfile();
        doctorProfile.setId(doctorUser.getId());
        doctorProfile.setSpecialty("Cardiology");
        doctorProfile.setSlotDurationMin(30);
        
        doctorUser.setDoctorProfile(doctorProfile);

        List<DayAvailabilityDTO> weeklyAvailability = List.of(
            new DayAvailabilityDTO("MONDAY", true, List.of(
                new TimeRangeDTO("09:00", "12:00")
            ))
        );
        
        testRequest = new DoctorAvailabilityRequestDTO(weeklyAvailability);
    }

    @Test
    void saveAvailability_DoctorNotFound() {
        UUID doctorId = UUID.randomUUID();
        
        when(userRepository.findById(doctorId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> doctorAvailabilityService.saveAvailability(doctorId, testRequest));
        
        assertEquals("Doctor not found", exception.getMessage());
        verify(userRepository).findById(doctorId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void saveAvailability_NoDoctorProfile() {
        UUID doctorId = doctorUser.getId();
        doctorUser.setDoctorProfile(null);
        
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctorUser));

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> doctorAvailabilityService.saveAvailability(doctorId, testRequest));
        
        assertEquals("Doctor profile not found", exception.getMessage());
        verify(userRepository).findById(doctorId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getAvailability_DoctorNotFound() {
        UUID doctorId = UUID.randomUUID();
        
        when(userRepository.findById(doctorId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> doctorAvailabilityService.getAvailability(doctorId));
        
        assertEquals("Doctor not found", exception.getMessage());
        verify(userRepository).findById(doctorId);
    }

    @Test
    void getAvailability_NoScheduleReturnsEmpty() {
        UUID doctorId = doctorUser.getId();
        doctorProfile.setAvailabilitySchedule(null);
        
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctorUser));

        DoctorAvailabilityResponseDTO result = doctorAvailabilityService.getAvailability(doctorId);

        assertNotNull(result);
        assertEquals(30, result.getSlotDurationMin());
        assertNotNull(result.getWeeklyAvailability());
        assertTrue(result.getWeeklyAvailability().isEmpty());
        
        verify(userRepository).findById(doctorId);
    }

    @Test 
    void getAvailability_NoDoctorProfile() {
        UUID doctorId = doctorUser.getId();
        doctorUser.setDoctorProfile(null);
        
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctorUser));

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> doctorAvailabilityService.getAvailability(doctorId));
        
        assertEquals("Doctor profile not found", exception.getMessage());
        verify(userRepository).findById(doctorId);
    }

    @Test
    void saveAvailability_Success() throws Exception {
        UUID doctorId = doctorUser.getId();
        String expectedJson = "[{\"day\":\"MONDAY\",\"enabled\":true,\"ranges\":[{\"start\":\"09:00\",\"end\":\"12:00\"}]}]";
        
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctorUser));
        when(objectMapper.writeValueAsString(testRequest.getWeeklyAvailability())).thenReturn(expectedJson);
        when(userRepository.save(doctorUser)).thenReturn(doctorUser);

        doctorAvailabilityService.saveAvailability(doctorId, testRequest);

        assertEquals(30, doctorProfile.getSlotDurationMin());
        assertEquals(expectedJson, doctorProfile.getAvailabilitySchedule());
        verify(userRepository).findById(doctorId);
        verify(objectMapper).writeValueAsString(testRequest.getWeeklyAvailability());
        verify(userRepository).save(doctorUser);
    }

    @Test
    void saveAvailability_AllWeekDisabled_Success() throws Exception {
        UUID doctorId = doctorUser.getId();
        List<DayAvailabilityDTO> allDisabled = List.of(
            new DayAvailabilityDTO("MONDAY", false, List.of()),
            new DayAvailabilityDTO("TUESDAY", false, List.of()),
            new DayAvailabilityDTO("WEDNESDAY", false, List.of()),
            new DayAvailabilityDTO("THURSDAY", false, List.of()),
            new DayAvailabilityDTO("FRIDAY", false, List.of()),
            new DayAvailabilityDTO("SATURDAY", false, List.of()),
            new DayAvailabilityDTO("SUNDAY", false, List.of())
        );
        DoctorAvailabilityRequestDTO request = new DoctorAvailabilityRequestDTO(allDisabled);
        String expectedJson = "[{\"day\":\"MONDAY\",\"enabled\":false,\"ranges\":[]},{\"day\":\"TUESDAY\",\"enabled\":false,\"ranges\":[]},{\"day\":\"WEDNESDAY\",\"enabled\":false,\"ranges\":[]},{\"day\":\"THURSDAY\",\"enabled\":false,\"ranges\":[]},{\"day\":\"FRIDAY\",\"enabled\":false,\"ranges\":[]},{\"day\":\"SATURDAY\",\"enabled\":false,\"ranges\":[]},{\"day\":\"SUNDAY\",\"enabled\":false,\"ranges\":[]}]";

        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctorUser));
        when(objectMapper.writeValueAsString(allDisabled)).thenReturn(expectedJson);
        when(userRepository.save(doctorUser)).thenReturn(doctorUser);

        doctorAvailabilityService.saveAvailability(doctorId, request);

        assertEquals(expectedJson, doctorProfile.getAvailabilitySchedule());
        verify(userRepository).findById(doctorId);
        verify(objectMapper).writeValueAsString(allDisabled);
        verify(userRepository).save(doctorUser);
    }

    @Test
    void saveAvailability_JsonProcessingException() throws Exception {
        UUID doctorId = doctorUser.getId();
        
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctorUser));
        when(objectMapper.writeValueAsString(testRequest.getWeeklyAvailability()))
            .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("JSON error") {});

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> doctorAvailabilityService.saveAvailability(doctorId, testRequest));
        
        assertEquals("Error serializing availability schedule", exception.getMessage());
        verify(userRepository).findById(doctorId);
        verify(objectMapper).writeValueAsString(testRequest.getWeeklyAvailability());
        verify(userRepository, never()).save(any());
    }

    @Test
    void getAvailability_WithValidSchedule() throws Exception {
        UUID doctorId = doctorUser.getId();
        String scheduleJson = "[{\"day\":\"MONDAY\",\"enabled\":true,\"ranges\":[{\"start\":\"09:00\",\"end\":\"12:00\"}]}]";
        doctorProfile.setAvailabilitySchedule(scheduleJson);
        CollectionType listType = new ObjectMapper().getTypeFactory().constructCollectionType(List.class, DayAvailabilityDTO.class);
        
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctorUser));
        when(objectMapper.readValue(scheduleJson, listType)).thenReturn(testRequest.getWeeklyAvailability());
        when(objectMapper.getTypeFactory()).thenReturn(new ObjectMapper().getTypeFactory());

        DoctorAvailabilityResponseDTO result = doctorAvailabilityService.getAvailability(doctorId);

        assertNotNull(result);
        assertEquals(30, result.getSlotDurationMin());
        assertNotNull(result.getWeeklyAvailability());
        assertEquals(1, result.getWeeklyAvailability().size());
        verify(userRepository).findById(doctorId);
    }

    @Test
    void getAvailability_InvalidJsonReturnsEmpty() throws Exception {
        UUID doctorId = doctorUser.getId();
        String invalidJson = "invalid json";
        doctorProfile.setAvailabilitySchedule(invalidJson);
        CollectionType listType = new ObjectMapper().getTypeFactory().constructCollectionType(List.class, DayAvailabilityDTO.class);
        
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctorUser));
        when(objectMapper.readValue(invalidJson, listType))
            .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Invalid JSON") {});
        when(objectMapper.getTypeFactory()).thenReturn(new ObjectMapper().getTypeFactory());

        DoctorAvailabilityResponseDTO result = doctorAvailabilityService.getAvailability(doctorId);

        assertNotNull(result);
        assertEquals(30, result.getSlotDurationMin());
        assertNotNull(result.getWeeklyAvailability());
        assertTrue(result.getWeeklyAvailability().isEmpty());
        verify(userRepository).findById(doctorId);
    }

    @Test
    void getAvailableSlots_NoAvailability() {
        UUID doctorId = doctorUser.getId();
        LocalDate fromDate = LocalDate.of(2025, 9, 23); // Monday
        LocalDate toDate = LocalDate.of(2025, 9, 27); // Friday
        
        doctorProfile.setAvailabilitySchedule(null);
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctorUser));

        List<AvailableSlotDTO> result = doctorAvailabilityService.getAvailableSlots(doctorId, fromDate, toDate);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository).findById(doctorId);
    }

    @Test
    void getAvailableSlots_WithValidAvailability() throws Exception {
        UUID doctorId = doctorUser.getId();
        LocalDate fromDate = LocalDate.of(2025, 9, 29);
        LocalDate toDate = LocalDate.of(2025, 9, 29);
        String scheduleJson = "[{\"day\":\"MONDAY\",\"enabled\":true,\"ranges\":[{\"start\":\"09:00\",\"end\":\"11:00\"}]}]";
        CollectionType listType = new ObjectMapper().getTypeFactory().constructCollectionType(List.class, DayAvailabilityDTO.class);
        
        doctorProfile.setAvailabilitySchedule(scheduleJson);
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctorUser));
        when(objectMapper.readValue(scheduleJson, listType)).thenReturn(testRequest.getWeeklyAvailability());
        when(objectMapper.getTypeFactory()).thenReturn(new ObjectMapper().getTypeFactory());

        List<AvailableSlotDTO> result = doctorAvailabilityService.getAvailableSlots(doctorId, fromDate, toDate);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(6, result.size());
        
        AvailableSlotDTO firstSlot = result.get(0);
        assertEquals(fromDate, firstSlot.getDate());
        assertEquals("09:00", firstSlot.getStartTime().toString());
        assertEquals("09:30", firstSlot.getEndTime().toString());
        
        verify(userRepository).findById(doctorId);
    }

    @Test
    void getAvailableSlots_DisabledDay() throws Exception {
        UUID doctorId = doctorUser.getId();
        LocalDate fromDate = LocalDate.of(2025, 9, 30);
        LocalDate toDate = LocalDate.of(2025, 9, 30);
        String scheduleJson = "[{\"day\":\"MONDAY\",\"enabled\":true,\"ranges\":[{\"start\":\"09:00\",\"end\":\"11:00\"}]}]";
        CollectionType listType = new ObjectMapper().getTypeFactory().constructCollectionType(List.class, DayAvailabilityDTO.class);
        
        doctorProfile.setAvailabilitySchedule(scheduleJson);
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctorUser));
        when(objectMapper.readValue(scheduleJson, listType)).thenReturn(testRequest.getWeeklyAvailability());
        when(objectMapper.getTypeFactory()).thenReturn(new ObjectMapper().getTypeFactory());

        List<AvailableSlotDTO> result = doctorAvailabilityService.getAvailableSlots(doctorId, fromDate, toDate);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository).findById(doctorId);
    }

    @Test
    void getAvailableSlots_DayDisabledInConfiguration() throws Exception {
        UUID doctorId = doctorUser.getId();
        LocalDate fromDate = LocalDate.of(2025, 9, 29); // Monday
        LocalDate toDate = LocalDate.of(2025, 9, 29);
        
        // Crear configuración con día deshabilitado
        List<DayAvailabilityDTO> weeklyAvailability = List.of(
            new DayAvailabilityDTO("MONDAY", false, List.of(
                new TimeRangeDTO("09:00", "12:00")
            ))
        );
        
        String scheduleJson = "[{\"day\":\"MONDAY\",\"enabled\":false,\"ranges\":[{\"start\":\"09:00\",\"end\":\"12:00\"}]}]";
        CollectionType listType = new ObjectMapper().getTypeFactory().constructCollectionType(List.class, DayAvailabilityDTO.class);
        
        doctorProfile.setAvailabilitySchedule(scheduleJson);
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctorUser));
        when(objectMapper.readValue(scheduleJson, listType)).thenReturn(weeklyAvailability);
        when(objectMapper.getTypeFactory()).thenReturn(new ObjectMapper().getTypeFactory());

        List<AvailableSlotDTO> result = doctorAvailabilityService.getAvailableSlots(doctorId, fromDate, toDate);

        assertNotNull(result);
        assertTrue(result.isEmpty()); // No debe generar slots porque el día está deshabilitado
        verify(userRepository).findById(doctorId);
    }

    @Test
    void getAvailableSlots_DayWithNullRanges() throws Exception {
        UUID doctorId = doctorUser.getId();
        LocalDate fromDate = LocalDate.of(2025, 9, 29); // Monday
        LocalDate toDate = LocalDate.of(2025, 9, 29);
        
        // Crear configuración con ranges null
        List<DayAvailabilityDTO> weeklyAvailability = List.of(
            new DayAvailabilityDTO("MONDAY", true, null) // ranges es null
        );
        
        String scheduleJson = "[{\"day\":\"MONDAY\",\"enabled\":true,\"ranges\":null}]";
        CollectionType listType = new ObjectMapper().getTypeFactory().constructCollectionType(List.class, DayAvailabilityDTO.class);
        
        doctorProfile.setAvailabilitySchedule(scheduleJson);
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctorUser));
        when(objectMapper.readValue(scheduleJson, listType)).thenReturn(weeklyAvailability);
        when(objectMapper.getTypeFactory()).thenReturn(new ObjectMapper().getTypeFactory());

        List<AvailableSlotDTO> result = doctorAvailabilityService.getAvailableSlots(doctorId, fromDate, toDate);

        assertNotNull(result);
        assertTrue(result.isEmpty()); // No debe generar slots porque ranges es null
        verify(userRepository).findById(doctorId);
    }

    @Test
    void getAvailableSlots_EmptyWeeklyAvailability() throws Exception {
        UUID doctorId = doctorUser.getId();
        LocalDate fromDate = LocalDate.of(2025, 9, 29); // Monday
        LocalDate toDate = LocalDate.of(2025, 9, 29);
        
        // Crear configuración con lista vacía (pero no null)
        List<DayAvailabilityDTO> emptyWeeklyAvailability = List.of(); // Lista vacía
        
        String scheduleJson = "[]"; // JSON de lista vacía
        CollectionType listType = new ObjectMapper().getTypeFactory().constructCollectionType(List.class, DayAvailabilityDTO.class);
        
        doctorProfile.setAvailabilitySchedule(scheduleJson);
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctorUser));
        when(objectMapper.readValue(scheduleJson, listType)).thenReturn(emptyWeeklyAvailability);
        when(objectMapper.getTypeFactory()).thenReturn(new ObjectMapper().getTypeFactory());

        List<AvailableSlotDTO> result = doctorAvailabilityService.getAvailableSlots(doctorId, fromDate, toDate);

        assertNotNull(result);
        assertTrue(result.isEmpty()); // No debe generar slots porque la lista está vacía
        verify(userRepository).findById(doctorId);
    }

    @Test
    void getAvailableSlots_DayNameMismatch() throws Exception {
        UUID doctorId = doctorUser.getId();
        LocalDate fromDate = LocalDate.of(2025, 9, 30); // Tuesday 
        LocalDate toDate = LocalDate.of(2025, 9, 30);
        
        // Configuración solo para MONDAY, pero se buscan slots para TUESDAY
        List<DayAvailabilityDTO> weeklyAvailability = List.of(
            new DayAvailabilityDTO("MONDAY", true, List.of(
                new TimeRangeDTO("09:00", "12:00")
            ))
        );
        
        String scheduleJson = "[{\"day\":\"MONDAY\",\"enabled\":true,\"ranges\":[{\"start\":\"09:00\",\"end\":\"12:00\"}]}]";
        CollectionType listType = new ObjectMapper().getTypeFactory().constructCollectionType(List.class, DayAvailabilityDTO.class);
        
        doctorProfile.setAvailabilitySchedule(scheduleJson);
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctorUser));
        when(objectMapper.readValue(scheduleJson, listType)).thenReturn(weeklyAvailability);
        when(objectMapper.getTypeFactory()).thenReturn(new ObjectMapper().getTypeFactory());

        List<AvailableSlotDTO> result = doctorAvailabilityService.getAvailableSlots(doctorId, fromDate, toDate);

        assertNotNull(result);
        assertTrue(result.isEmpty()); // No debe generar slots porque no hay configuración para TUESDAY
        verify(userRepository).findById(doctorId);
    }

    @Test
    void getAvailableSlots_WeeklyAvailabilityIsNullDirectly() {
        UUID doctorId = doctorUser.getId();
        LocalDate fromDate = LocalDate.of(2025, 9, 29); // Monday
        LocalDate toDate = LocalDate.of(2025, 9, 29);
        
        // Crear un caso donde getWeeklyAvailability() sea null directamente
        DoctorAvailabilityResponseDTO mockAvailability = new DoctorAvailabilityResponseDTO();
        mockAvailability.setSlotDurationMin(30);
        mockAvailability.setWeeklyAvailability(null); // Explicitly null
        
        // Mock getAvailability to return the mock response directly
        DoctorAvailabilityService spyService = spy(doctorAvailabilityService);
        doReturn(mockAvailability).when(spyService).getAvailability(doctorId);

        List<AvailableSlotDTO> result = spyService.getAvailableSlots(doctorId, fromDate, toDate);

        assertNotNull(result);
        assertTrue(result.isEmpty()); // No debe generar slots porque weeklyAvailability es null
        verify(spyService).getAvailability(doctorId);
    }
}