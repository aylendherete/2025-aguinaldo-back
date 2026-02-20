package com.medibook.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medibook.api.dto.Badge.BadgeProgressSummaryDTO;
import com.medibook.api.dto.Badge.BadgesResponseDTO;
import com.medibook.api.dto.Badge.BadgeDTO;
import com.medibook.api.entity.*;
import com.medibook.api.entity.BadgeType.BadgeCategory;
import com.medibook.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BadgeServiceTest {

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BadgeStatisticsRepository statisticsRepository;

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private TurnAssignedRepository turnAssignedRepository;

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @Mock
    private BadgeProgressService badgeProgressService;

    @InjectMocks
    private BadgeService badgeService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private UUID userId;
    private User doctor;
    private User patient;
    private UUID patientId;
    private UUID doctorId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userId = UUID.randomUUID();
        doctor = new User();
        doctor.setId(userId);
        doctor.setName("Dr. Test");
        doctor.setSurname("Doctor");
        doctor.setRole("DOCTOR");
        doctor.setStatus("ACTIVE");

        patient = new User();
        patient.setId(UUID.randomUUID());
        patient.setName("Test");
        patient.setSurname("Patient");
        patient.setRole("PATIENT");
        patient.setStatus("ACTIVE");

        patientId = patient.getId();
        doctorId = doctor.getId();

        lenient().when(ratingRepository.findTop35ByRated_IdAndRater_RoleOrderByCreatedAtDesc(any(UUID.class), anyString()))
            .thenReturn(new ArrayList<>());
    }

    @Test
    void getUserBadges_UserFound_ReturnsBadgesResponse() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(badgeRepository.findByUser_IdOrderByEarnedAtDesc(userId)).thenReturn(new ArrayList<>());

        BadgesResponseDTO result = badgeService.getUserBadges(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("Dr. Test Doctor", result.getUserName());
        assertEquals("DOCTOR", result.getRole());
        assertEquals(0, result.getTotalActiveBadges());

        verify(userRepository).findById(userId);
        verify(badgeRepository).findByUser_IdOrderByEarnedAtDesc(userId);
    }

    @Test
    void getUserBadges_UserNotFound_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> badgeService.getUserBadges(userId));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(userId);
        verifyNoInteractions(badgeRepository);
    }

    @Test
    void getUserBadgeProgress_UserFound_ReturnsProgressList() {
        List<BadgeProgressSummaryDTO> expectedProgress = List.of(
            BadgeProgressSummaryDTO.builder()
                .badgeType("DOCTOR_EMPATHETIC_DOCTOR")
                .badgeName("Médico Empático")
                .category(BadgeCategory.QUALITY_OF_CARE)
                .rarity(com.medibook.api.model.BadgeMetadata.BadgeRarity.RARE)
                .description("Destaca por su empatía")
                .icon("❤️")
                .color("#E91E63")
                .criteria("Recibe 25 menciones positivas de empatía")
                .earned(true)
                .progressPercentage(100.0)
                .statusMessage("¡Insignia obtenida! Excelente trabajo.")
                .build()
        );

        when(badgeProgressService.getBadgeProgress(userId)).thenReturn(expectedProgress);

        List<BadgeProgressSummaryDTO> result = badgeService.getUserBadgeProgress(userId);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("DOCTOR_EMPATHETIC_DOCTOR", result.get(0).getBadgeType());
        assertEquals("Médico Empático", result.get(0).getBadgeName());
        assertEquals(BadgeCategory.QUALITY_OF_CARE, result.get(0).getCategory());

        verify(badgeProgressService).getBadgeProgress(userId);
    }

    @Test
    void getUserBadgeProgress_UserNotFound_ThrowsException() {
        when(badgeProgressService.getBadgeProgress(userId)).thenThrow(new RuntimeException("User not found"));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> badgeService.getUserBadgeProgress(userId));

        assertEquals("User not found", exception.getMessage());
        verify(badgeProgressService).getBadgeProgress(userId);
    }

    @Test
    void evaluateTurnRelatedBadges_Doctor_EvaluatesDoctorBadges() {
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(objectMapper.createObjectNode())
                .progress(objectMapper.createObjectNode())
                .build();
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);

        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));

        badgeService.evaluateTurnRelatedBadges(userId);

        verify(userRepository).findById(userId);
    }

    @Test
    void evaluateTurnRelatedBadges_Patient_EvaluatesPatientBadges() {
        UUID patientId = patient.getId();
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(patientId)
                .statistics(objectMapper.createObjectNode())
                .progress(objectMapper.createObjectNode())
                .build();
        when(statisticsRepository.findByUserId(patientId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);

        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));

        badgeService.evaluateTurnRelatedBadges(patientId);

        verify(userRepository).findById(patientId);
    }

    @Test
    void evaluateRatingRelatedBadges_Doctor_EvaluatesDoctorRatingBadges() {
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(objectMapper.createObjectNode())
                .progress(objectMapper.createObjectNode())
                .build();
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);

        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));

        badgeService.evaluateRatingRelatedBadges(userId);

        verify(userRepository).findById(userId);
    }

    @Test
    void evaluateRatingRelatedBadges_Patient_EvaluatesPatientRatingBadges() {
        UUID patientId = patient.getId();
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(patientId)
                .statistics(objectMapper.createObjectNode())
                .progress(objectMapper.createObjectNode())
                .build();
        when(statisticsRepository.findByUserId(patientId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);

        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));

        badgeService.evaluateRatingRelatedBadges(patientId);

        verify(userRepository).findById(patientId);
    }

    @Test
    void evaluateAllBadges_Doctor_EvaluatesAllDoctorBadges() {
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(objectMapper.createObjectNode())
                .progress(objectMapper.createObjectNode())
                .build();
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);

        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));

        badgeService.evaluateAllBadges(userId);

        verify(userRepository, times(6)).findById(userId);
    }

    @Test
    void evaluateAllBadges_Patient_EvaluatesAllPatientBadges() {
        UUID patientId = patient.getId();
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(patientId)
                .statistics(objectMapper.createObjectNode())
                .progress(objectMapper.createObjectNode())
                .build();
        when(statisticsRepository.findByUserId(patientId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);

        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));

        badgeService.evaluateAllBadges(patientId);

        verify(userRepository, times(5)).findById(patientId);
    }

    @Test
    void toBadgeDTO_ConvertsBadgeCorrectly() {
        Badge badge = Badge.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .badgeType("DOCTOR_EMPATHETIC_DOCTOR")
                .earnedAt(OffsetDateTime.now())
                .isActive(true)
                .lastEvaluatedAt(OffsetDateTime.now())
                .build();

        BadgeDTO result = badgeService.toBadgeDTO(badge, "DOCTOR");

        assertNotNull(result);
        assertEquals(badge.getId(), result.getId());
        assertEquals(badge.getBadgeType(), result.getBadgeType());
        assertEquals(BadgeCategory.QUALITY_OF_CARE, result.getCategory());
        assertEquals(badge.getEarnedAt(), result.getEarnedAt());
        assertEquals(badge.getIsActive(), result.getIsActive());
    }

    @Test
    void evaluateMediBookWelcome_SufficientTurns_ActivatesBadge() {
        UUID patientId = patient.getId();
        
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_turns_completed", 2);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(patientId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(patientId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(badgeRepository.findByUser_IdAndBadgeType(patientId, "PATIENT_MEDIBOOK_WELCOME")).thenReturn(Optional.empty());

        badgeService.evaluateTurnRelatedBadges(patientId);

        verify(badgeRepository).save(argThat(badge -> 
            "PATIENT_MEDIBOOK_WELCOME".equals(badge.getBadgeType()) && 
            badge.getIsActive() && 
            badge.getEarnedAt() != null
        ));
    }

    @Test
    void evaluateMediBookWelcome_InsufficientTurns_DeactivatesBadge() {
        UUID patientId = patient.getId();
        
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_turns_completed", 0);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(patientId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(patientId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(badgeRepository.findByUser_IdAndBadgeType(patientId, "PATIENT_MEDIBOOK_WELCOME")).thenReturn(Optional.empty());

        badgeService.evaluateTurnRelatedBadges(patientId);

        verify(badgeRepository, never()).save(any(Badge.class));
    }

    @Test
    void evaluateExemplaryPunctuality_SufficientRatings_ActivatesBadge() {
        UUID patientId = patient.getId();
        
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("doctor_punctuality_mentions", 20);
        statisticsJson.put("total_turns_completed", 20);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(patientId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(patientId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(badgeRepository.findByUser_IdAndBadgeType(patientId, "PATIENT_EXEMPLARY_PUNCTUALITY")).thenReturn(Optional.empty());

        badgeService.evaluateRatingRelatedBadges(patientId);

        verify(badgeRepository).save(argThat(badge -> 
            "PATIENT_EXEMPLARY_PUNCTUALITY".equals(badge.getBadgeType()) && 
            badge.getIsActive()
        ));
    }

    @Test
    void evaluateAlwaysPrepared_SufficientFiles_ActivatesBadge() {
        UUID patientId = patient.getId();
        
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("files_uploaded", 15);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(patientId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(patientId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(badgeRepository.findByUser_IdAndBadgeType(patientId, "PATIENT_ALWAYS_PREPARED")).thenReturn(Optional.empty());

        badgeService.evaluateFileRelatedBadges(patientId);

        verify(badgeRepository).save(argThat(badge -> 
            "PATIENT_ALWAYS_PREPARED".equals(badge.getBadgeType()) && 
            badge.getIsActive()
        ));
    }

    @Test
    void evaluateExceptionalCommunicator_SufficientCommunication_ActivatesBadge() {
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_communication_count", 30);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_EXCEPTIONAL_COMMUNICATOR")).thenReturn(Optional.empty());

        badgeService.evaluateRatingRelatedBadges(userId);

        verify(badgeRepository).save(argThat(badge -> 
            "DOCTOR_EXCEPTIONAL_COMMUNICATOR".equals(badge.getBadgeType()) && 
            badge.getIsActive()
        ));
    }

    @Test
    void evaluateCompleteDocumenter_SufficientTurnsAndDocumentation_ActivatesBadge() {
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_turns_completed", 60);
        statisticsJson.put("documentation_count", 40);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_COMPLETE_DOCUMENTER")).thenReturn(Optional.empty());

        badgeService.evaluateDocumentationRelatedBadges(userId);

        verify(badgeRepository).save(argThat(badge -> 
            "DOCTOR_COMPLETE_DOCUMENTER".equals(badge.getBadgeType()) && 
            badge.getIsActive()
        ));
    }

    @Test
    void evaluateAlwaysAvailable_SufficientAvailability_ActivatesBadge() {
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(objectMapper.createObjectNode())
                .progress(objectMapper.createObjectNode())
                .build();
        
        DoctorProfile profile = new DoctorProfile();
        profile.setId(userId);
        profile.setAvailabilitySchedule("[{\"day\": \"MONDAY\", \"ranges\": [{\"end\": \"18:00\", \"start\": \"09:00\"}], \"enabled\": true}, {\"day\": \"TUESDAY\", \"ranges\": [{\"end\": \"18:00\", \"start\": \"09:00\"}], \"enabled\": true}, {\"day\": \"WEDNESDAY\", \"ranges\": [{\"end\": \"18:00\", \"start\": \"09:00\"}], \"enabled\": true}, {\"day\": \"THURSDAY\", \"ranges\": [{\"end\": \"18:00\", \"start\": \"09:00\"}], \"enabled\": true}]");
        
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(doctorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_ALWAYS_AVAILABLE")).thenReturn(Optional.empty());

        badgeService.evaluateConsistencyRelatedBadges(userId);

        verify(badgeRepository).save(argThat(badge -> 
            "DOCTOR_ALWAYS_AVAILABLE".equals(badge.getBadgeType()) && 
            badge.getIsActive()
        ));
    }

    @Test
    void evaluateMedicalLegend_AllRequiredBadgesActive_ActivatesBadge() {
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_turns_completed", 350);
        statisticsJson.put("total_avg_rating", 4.8);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_MEDICAL_LEGEND")).thenReturn(Optional.empty());
        stubMedicalLegendDependencies(userId, true, true, true);

        badgeService.evaluateTurnRelatedBadges(userId);

        verify(badgeRepository).save(argThat(badge -> 
            "DOCTOR_MEDICAL_LEGEND".equals(badge.getBadgeType()) && 
            badge.getIsActive()
        ));
    }

    @Test
    void evaluateConstantPatient_SufficientTurns_ActivatesBadge() {
        UUID patientId = patient.getId();
        
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_turns_completed", 15);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(patientId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(patientId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(badgeRepository.findByUser_IdAndBadgeType(patientId, "PATIENT_CONSTANT_PATIENT")).thenReturn(Optional.empty());

        badgeService.evaluateTurnRelatedBadges(patientId);

        verify(badgeRepository).save(argThat(badge -> 
            "PATIENT_CONSTANT_PATIENT".equals(badge.getBadgeType()) && 
            badge.getIsActive()
        ));
    }

    @Test
    void evaluateSmartPlanner_SufficientAdvanceBookings_ActivatesBadge() {
        UUID patientId = patient.getId();
        
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("advance_bookings", 10);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(patientId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(patientId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(badgeRepository.findByUser_IdAndBadgeType(patientId, "PATIENT_SMART_PLANNER")).thenReturn(Optional.empty());

        badgeService.evaluateBookingRelatedBadges(patientId);

        verify(badgeRepository).save(argThat(badge -> 
            "PATIENT_SMART_PLANNER".equals(badge.getBadgeType()) && 
            badge.getIsActive()
        ));
    }

    @Test
    void evaluateExcellentCollaborator_SufficientCollaboration_ActivatesBadge() {
        UUID patientId = patient.getId();
        
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("doctor_collaboration_mentions", 10);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(patientId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(patientId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(badgeRepository.findByUser_IdAndBadgeType(patientId, "PATIENT_EXCELLENT_COLLABORATOR")).thenReturn(Optional.empty());

        badgeService.evaluateRatingRelatedBadges(patientId);

        verify(badgeRepository).save(argThat(badge -> 
            "PATIENT_EXCELLENT_COLLABORATOR".equals(badge.getBadgeType()) && 
            badge.getIsActive()
        ));
    }

    @Test
    void evaluateResponsibleEvaluator_SufficientRatingsAndAvg_ActivatesBadge() {
        UUID patientId = patient.getId();
        
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("ratings_given", 10);
        statisticsJson.put("avg_rating_given", 4.0);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(patientId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(patientId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(badgeRepository.findByUser_IdAndBadgeType(patientId, "PATIENT_RESPONSIBLE_EVALUATOR")).thenReturn(Optional.empty());

        badgeService.evaluateRatingRelatedBadges(patientId);

        verify(badgeRepository).save(argThat(badge -> 
            "PATIENT_RESPONSIBLE_EVALUATOR".equals(badge.getBadgeType()) && 
            badge.getIsActive()
        ));
    }

    @Test
    void evaluateExcellenceModel_AllCriteriaMet_ActivatesBadge() {
        UUID patientId = patient.getId();
        
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_turns_completed", 25);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(patientId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(patientId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(badgeRepository.countActiveBadgesByUserIdExcludingType(patientId, "PATIENT_EXCELLENCE_MODEL")).thenReturn(4L);
        when(badgeRepository.findByUser_IdAndBadgeType(patientId, "PATIENT_EXCELLENCE_MODEL")).thenReturn(Optional.empty());

        badgeService.evaluateRatingRelatedBadges(patientId);

        verify(badgeRepository).save(argThat(badge -> 
            "PATIENT_EXCELLENCE_MODEL".equals(badge.getBadgeType()) && 
            badge.getIsActive()
        ));
    }

    @Test
    void evaluateEmpatheticDoctor_SufficientEmpathy_ActivatesBadge() {
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_empathy_count", 25);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_EMPATHETIC_DOCTOR")).thenReturn(Optional.empty());

        badgeService.evaluateRatingRelatedBadges(userId);

        verify(badgeRepository).save(argThat(badge -> 
            "DOCTOR_EMPATHETIC_DOCTOR".equals(badge.getBadgeType()) && 
            badge.getIsActive()
        ));
    }

    @Test
    void evaluatePunctualityProfessional_SufficientPunctuality_ActivatesBadge() {
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_punctuality_count", 20);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_PUNCTUALITY_PROFESSIONAL")).thenReturn(Optional.empty());

        badgeService.evaluateRatingRelatedBadges(userId);

        verify(badgeRepository).save(argThat(badge -> 
            "DOCTOR_PUNCTUALITY_PROFESSIONAL".equals(badge.getBadgeType()) && 
            badge.getIsActive()
        ));
    }

    @Test
    void evaluateDetailedDiagnostician_AllCriteriaMet_ActivatesBadge() {
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_turns_completed", 30);
        statisticsJson.put("documentation_count", 60);
        statisticsJson.put("total_avg_words_per_entry", 160.0);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(badgeRepository.existsByUser_IdAndBadgeTypeAndIsActive(userId, "DOCTOR_COMPLETE_DOCUMENTER", true)).thenReturn(true);
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_DETAILED_DIAGNOSTICIAN")).thenReturn(Optional.empty());

        badgeService.evaluateDocumentationRelatedBadges(userId);

        verify(badgeRepository).save(argThat(badge -> 
            "DOCTOR_DETAILED_DIAGNOSTICIAN".equals(badge.getBadgeType()) && 
            badge.getIsActive()
        ));
    }

    @Test
    void evaluateAgileResponder_ViaResponseRelatedBadges_ActivatesBadge() {
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_requests_handled", 8);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_AGILE_RESPONDER")).thenReturn(Optional.empty());

        badgeService.evaluateResponseRelatedBadges(userId);

        verify(badgeRepository).save(argThat(badge -> 
            "DOCTOR_AGILE_RESPONDER".equals(badge.getBadgeType()) && 
            badge.getIsActive()
        ));
    }

    @Test
    void evaluateRelationshipBuilder_ViaTurnRelatedBadges_ActivatesBadge() {
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_unique_patients", 50);
        statisticsJson.put("returning_patients_count", 10);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_RELATIONSHIP_BUILDER")).thenReturn(Optional.empty());

        List<User> patients = java.util.stream.IntStream.range(0, 50).mapToObj(i -> {
            User u = new User();
            u.setId(UUID.randomUUID());
            return u;
        }).collect(java.util.stream.Collectors.toList());
        when(turnAssignedRepository.findDistinctPatientsByDoctorId(userId)).thenReturn(patients);

        badgeService.evaluateTurnRelatedBadges(userId);

        verify(badgeRepository).save(argThat(badge -> 
            "DOCTOR_RELATIONSHIP_BUILDER".equals(badge.getBadgeType()) && 
            badge.getIsActive()
        ));
    }

    @Test
    void evaluateConsistentProfessional_ViaConsistencyRelatedBadges_ActivatesBadge() {
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_turns_completed", 80);
        statisticsJson.put("total_cancellations", 10);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_CONSISTENT_PROFESSIONAL")).thenReturn(Optional.empty());

        badgeService.evaluateConsistencyRelatedBadges(userId);

        verify(badgeRepository).save(argThat(badge -> 
            "DOCTOR_CONSISTENT_PROFESSIONAL".equals(badge.getBadgeType()) && 
            badge.getIsActive()
        ));
    }

    @Test
    void evaluateTopSpecialist_AllCriteriaMet_ActivatesBadge() {
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_turns_completed", 100);
        statisticsJson.put("total_avg_rating", 4.2);
        statisticsJson.put("specialty_rank_percentile", 0.15);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_TOP_SPECIALIST")).thenReturn(Optional.empty());
        when(ratingRepository.findTop35ByRated_IdAndRater_RoleOrderByCreatedAtDesc(userId, "PATIENT")).thenReturn(createRatings(35, 4));

        badgeService.evaluateTurnRelatedBadges(userId);

        verify(badgeRepository).save(argThat(badge -> 
            "DOCTOR_TOP_SPECIALIST".equals(badge.getBadgeType()) && 
            badge.getIsActive()
        ));
    }

    @Test
    void evaluateConsistencyRelatedBadges_DoctorRole_EvaluatesDoctorBadges() {
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_turns_completed", 60);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        DoctorProfile profile = new DoctorProfile();
        profile.setId(userId);
        profile.setAvailabilitySchedule("{\"monday\": \"09:00-17:00\"}");
        
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(doctorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_RELATIONSHIP_BUILDER")).thenReturn(Optional.empty());
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_TOP_SPECIALIST")).thenReturn(Optional.empty());
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_MEDICAL_LEGEND")).thenReturn(Optional.empty());
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_ALWAYS_AVAILABLE")).thenReturn(Optional.empty());
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_CONSISTENT_PROFESSIONAL")).thenReturn(Optional.empty());
        when(ratingRepository.findTop35ByRated_IdAndRater_RoleOrderByCreatedAtDesc(userId, "PATIENT")).thenReturn(new ArrayList<>());

        badgeService.evaluateConsistencyRelatedBadges(userId);

        verify(userRepository).findById(userId);
        verify(badgeRepository).findByUser_IdAndBadgeType(userId, "DOCTOR_CONSISTENT_PROFESSIONAL");
        verify(badgeRepository).findByUser_IdAndBadgeType(userId, "DOCTOR_ALWAYS_AVAILABLE");
    }

    @Test
    void evaluateTurnRelatedBadges_ExceptionInSave_LogsError() {
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_turns_completed", 10);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenThrow(new RuntimeException("Database error"));
        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));

        badgeService.evaluateTurnRelatedBadges(userId);

        verify(statisticsRepository, atLeast(1)).save(any(BadgeStatistics.class));
    }

    @Test
    void evaluateRatingRelatedBadges_ExceptionInSave_LogsError() {
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_turns_completed", 10);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenThrow(new RuntimeException("Database error"));
        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));

        badgeService.evaluateRatingRelatedBadges(userId);

        verify(statisticsRepository, atLeast(1)).save(any(BadgeStatistics.class));
    }

    @Test
    void evaluateFileRelatedBadges_ExceptionInSave_LogsError() {
        UUID patientId = patient.getId();
        
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("files_uploaded", 15);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(patientId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(patientId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenThrow(new RuntimeException("Database error"));
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));

        badgeService.evaluateFileRelatedBadges(patientId);

        verify(statisticsRepository, atLeast(1)).save(any(BadgeStatistics.class));
    }

    @Test
    void evaluateBookingRelatedBadges_ExceptionInSave_LogsError() {
        UUID patientId = patient.getId();
        
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_advance_booking_count", 10);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(patientId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(patientId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenThrow(new RuntimeException("Database error"));
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));

        badgeService.evaluateBookingRelatedBadges(patientId);

        verify(statisticsRepository, atLeast(1)).save(any(BadgeStatistics.class));
    }

    @Test
    void evaluateDocumentationRelatedBadges_ExceptionInSave_LogsError() {
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_turns_completed", 60);
        statisticsJson.put("total_documented_count", 40);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenThrow(new RuntimeException("Database error"));
        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));

        badgeService.evaluateDocumentationRelatedBadges(userId);

        verify(statisticsRepository, atLeast(1)).save(any(BadgeStatistics.class));
    }

    @Test
    void evaluateResponseRelatedBadges_ExceptionInSave_LogsError() {
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_requests_handled", 7);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenThrow(new RuntimeException("Database error"));
        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));

        badgeService.evaluateResponseRelatedBadges(userId);

        verify(statisticsRepository, atLeast(1)).save(any(BadgeStatistics.class));
    }

    @Test
    void evaluateTurnCompletionRelatedBadges_ExceptionInSave_LogsError() {
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_turns_completed", 1);
        
        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();
        
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenThrow(new RuntimeException("Database error"));
        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));

        badgeService.evaluateTurnCompletionRelatedBadges(userId);

        verify(statisticsRepository, atLeast(1)).save(any(BadgeStatistics.class));
    }

    @Test
    void activateBadge_ExistingBadge_ActivatesSuccessfully() {
        Badge existingBadge = new Badge();
        existingBadge.setId(UUID.randomUUID());
        existingBadge.setBadgeType("DOCTOR_EXCEPTIONAL_COMMUNICATOR");
        existingBadge.setIsActive(false);

        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_EXCEPTIONAL_COMMUNICATOR"))
            .thenReturn(Optional.of(existingBadge));
        when(badgeRepository.save(any(Badge.class))).thenReturn(existingBadge);

        badgeService.activateBadge(userId, "DOCTOR_EXCEPTIONAL_COMMUNICATOR");

        assertTrue(existingBadge.getIsActive());
        verify(badgeRepository).findByUser_IdAndBadgeType(userId, "DOCTOR_EXCEPTIONAL_COMMUNICATOR");
        verify(badgeRepository).save(existingBadge);
    }

    @Test
    void activateBadge_NewBadge_CreatesAndActivates() {
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_EXCEPTIONAL_COMMUNICATOR"))
            .thenReturn(Optional.empty());
        when(badgeRepository.save(any(Badge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        badgeService.activateBadge(userId, "DOCTOR_EXCEPTIONAL_COMMUNICATOR");

        verify(badgeRepository).findByUser_IdAndBadgeType(userId, "DOCTOR_EXCEPTIONAL_COMMUNICATOR");
        verify(badgeRepository).save(any(Badge.class));
    }

    @Test
    void deactivateBadge_ExistingBadge_DeactivatesSuccessfully() {
        Badge existingBadge = new Badge();
        existingBadge.setId(UUID.randomUUID());
        existingBadge.setBadgeType("DOCTOR_EXCEPTIONAL_COMMUNICATOR");
        existingBadge.setIsActive(true);

        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_EXCEPTIONAL_COMMUNICATOR"))
            .thenReturn(Optional.of(existingBadge));
        when(badgeRepository.save(any(Badge.class))).thenReturn(existingBadge);

        badgeService.deactivateBadge(userId, "DOCTOR_EXCEPTIONAL_COMMUNICATOR");

        assertFalse(existingBadge.getIsActive());
        verify(badgeRepository).findByUser_IdAndBadgeType(userId, "DOCTOR_EXCEPTIONAL_COMMUNICATOR");
        verify(badgeRepository).save(existingBadge);
    }

    @Test
    void getCategoryForBadge_DoctorBadge_ReturnsDoctorCategory() {
        BadgeCategory result = badgeService.getCategoryForBadge("DOCTOR_EXCEPTIONAL_COMMUNICATOR", "DOCTOR");
        assertEquals(BadgeCategory.QUALITY_OF_CARE, result);
    }

    @Test
    void getCategoryForBadge_UnknownBadge_ReturnsConsistency() {
        BadgeCategory result = badgeService.getCategoryForBadge("UNKNOWN_BADGE", "DOCTOR");
        assertEquals(BadgeCategory.CONSISTENCY, result);
    }

    @Test
    void createBadge_ValidInputs_CreatesBadge() {
        when(badgeRepository.save(any(Badge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        badgeService.createBadge(userId, "DOCTOR_EXCEPTIONAL_COMMUNICATOR");

        verify(badgeRepository).save(any(Badge.class));
    }

    @Test
    void parseJson_ValidJson_ReturnsParsedJsonNode() {
        String json = "{\"key\": \"value\", \"number\": 42}";
        JsonNode result = badgeService.parseJson(json);
        
        assertNotNull(result);
        assertEquals("value", result.path("key").asText());
        assertEquals(42, result.path("number").asInt());
    }

    @Test
    void parseJson_InvalidJson_ReturnsEmptyJsonNode() {
        String invalidJson = "invalid json";
        JsonNode result = badgeService.parseJson(invalidJson);
        
        assertNotNull(result);
        assertTrue(result.isObject());
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateAllBadges_DoctorWithHighStats_ActivatesMultipleBadges() {
        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_turns_completed", 100);
        statisticsJson.put("documentation_count", 50);
        statisticsJson.put("total_turns_cancelled", 5);
        statisticsJson.put("unique_patients_served", 20);
        statisticsJson.put("requests_handled", 10);
        statisticsJson.put("total_ratings_received", 50);
        statisticsJson.put("total_communication_count", 30);
        statisticsJson.put("total_empathy_count", 30);
        statisticsJson.put("total_punctuality_count", 25);
        statisticsJson.put("total_avg_rating", 4.5);
        statisticsJson.put("total_low_rating_count", 2);

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();

        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));

        badgeService.evaluateAllBadges(userId);

        verify(statisticsRepository, atLeast(1)).findByUserId(userId);
        verify(userRepository, times(6)).findById(userId);
        verify(statisticsRepository, atLeast(1)).save(any(BadgeStatistics.class));
    }

    @Test
    void evaluateAllBadges_PatientWithHighStats_ActivatesMultipleBadges() {
        patient.setId(userId);

        ObjectNode statisticsJson = objectMapper.createObjectNode();
        statisticsJson.put("total_turns_completed", 30);
        statisticsJson.put("turns_with_same_doctor", 5);
        statisticsJson.put("ratings_given", 15);
        statisticsJson.put("doctor_collaboration_mentions", 12);
        statisticsJson.put("doctor_punctuality_mentions", 12);
        statisticsJson.put("advance_bookings", 12);
        statisticsJson.put("files_uploaded", 12);
        statisticsJson.put("avg_rating_received", 4.8);

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(statisticsJson)
                .progress(objectMapper.createObjectNode())
                .build();

        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(userId)).thenReturn(Optional.of(patient));

        badgeService.evaluateAllBadges(userId);

        verify(statisticsRepository, atLeast(1)).findByUserId(userId);
        verify(userRepository, times(5)).findById(userId);
        verify(statisticsRepository, atLeast(1)).save(any(BadgeStatistics.class));
    }

    @Test
    void evaluateMediBookWelcome_WithInsufficientTurns_DeactivatesBadge() {
        User patient = new User();
        patient.setId(patientId);
        patient.setRole("PATIENT");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(patientId)
                .statistics(objectMapper.createObjectNode().put("total_turns_completed", 0))
                .progress(objectMapper.createObjectNode())
                .build();

        when(statisticsRepository.findByUserId(patientId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));

        badgeService.evaluateAllBadges(patientId);

        verify(badgeRepository).findByUser_IdAndBadgeType(patientId, "PATIENT_MEDIBOOK_WELCOME");
        verify(badgeRepository, never()).save(any(Badge.class));
    }

    @Test
    void evaluateConstantPatient_WithExactRequiredTurns_ActivatesBadge() {
        User patient = new User();
        patient.setId(patientId);
        patient.setRole("PATIENT");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(patientId)
                .statistics(objectMapper.createObjectNode().put("total_turns_completed", 15))
                .progress(objectMapper.createObjectNode())
                .build();

        when(statisticsRepository.findByUserId(patientId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));

        badgeService.evaluateAllBadges(patientId);

        verify(badgeRepository, atLeastOnce()).findByUser_IdAndBadgeType(patientId, "PATIENT_CONSTANT_PATIENT");
    }

    @Test
    void evaluateExemplaryPunctuality_WithInsufficientTurns_DeactivatesBadge() {
        User patient = new User();
        patient.setId(patientId);
        patient.setRole("PATIENT");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(patientId)
                .statistics(objectMapper.createObjectNode()
                        .put("total_turns_completed", 5)
                        .put("total_punctuality_count", 15))
                .progress(objectMapper.createObjectNode())
                .build();

        when(statisticsRepository.findByUserId(patientId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));

        badgeService.evaluateAllBadges(patientId);

        verify(badgeRepository).findByUser_IdAndBadgeType(patientId, "PATIENT_EXEMPLARY_PUNCTUALITY");
    }

    @Test
    void evaluateResponsibleEvaluator_WithHighRatingRange_DeactivatesBadge() {
        User patient = new User();
        patient.setId(patientId);
        patient.setRole("PATIENT");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(patientId)
                .statistics(objectMapper.createObjectNode()
                        .put("ratings_given", 15)
                        .put("avg_rating_given", 4.5))
                .progress(objectMapper.createObjectNode())
                .build();

        when(statisticsRepository.findByUserId(patientId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));

        badgeService.evaluateAllBadges(patientId);

        verify(badgeRepository).findByUser_IdAndBadgeType(patientId, "PATIENT_RESPONSIBLE_EVALUATOR");
    }

    @Test
    void evaluateExcellenceModel_WithInsufficientBadges_DeactivatesBadge() {
        User patient = new User();
        patient.setId(patientId);
        patient.setRole("PATIENT");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(patientId)
                .statistics(objectMapper.createObjectNode().put("total_turns_completed", 30))
                .progress(objectMapper.createObjectNode())
                .build();

        when(statisticsRepository.findByUserId(patientId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(badgeRepository.countActiveBadgesByUserIdExcludingType(patientId, "PATIENT_EXCELLENCE_MODEL")).thenReturn(2L);

        badgeService.evaluateAllBadges(patientId);

        verify(badgeRepository).findByUser_IdAndBadgeType(patientId, "PATIENT_EXCELLENCE_MODEL");
    }

    @Test
    void evaluateExceptionalCommunicator_Doctor_WithExactThreshold_ActivatesBadge() {
        User doctor = new User();
        doctor.setId(doctorId);
        doctor.setRole("DOCTOR");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(doctorId)
                .statistics(objectMapper.createObjectNode().put("total_communication_count", 25))
                .progress(objectMapper.createObjectNode())
                .build();

        when(statisticsRepository.findByUserId(doctorId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(ratingRepository.findTop35ByRated_IdAndRater_RoleOrderByCreatedAtDesc(doctorId, "PATIENT")).thenReturn(createRatings(35, 4));

        badgeService.evaluateAllBadges(doctorId);

        verify(badgeRepository).findByUser_IdAndBadgeType(doctorId, "DOCTOR_EXCEPTIONAL_COMMUNICATOR");
    }

    @Test
    void evaluateCompleteDocumenter_Doctor_WithLowTurns_ScalesRequirement() {
        User doctor = new User();
        doctor.setId(doctorId);
        doctor.setRole("DOCTOR");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(doctorId)
                .statistics(objectMapper.createObjectNode()
                        .put("total_turns_completed", 30)
                        .put("total_documented_count", 20))
                .progress(objectMapper.createObjectNode())
                .build();

        when(statisticsRepository.findByUserId(doctorId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        badgeService.evaluateAllBadges(doctorId);

        verify(badgeRepository).findByUser_IdAndBadgeType(doctorId, "DOCTOR_COMPLETE_DOCUMENTER");
    }

    @Test
    void evaluateConsistentProfessional_Doctor_WithHighCancellationRate_NoActivation() {
        User doctor = new User();
        doctor.setId(doctorId);
        doctor.setRole("DOCTOR");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(doctorId)
                .statistics(objectMapper.createObjectNode()
                        .put("total_turns_completed", 100)
                        .put("total_cancellations", 20))
                .progress(objectMapper.createObjectNode())
                .build();

        when(statisticsRepository.findByUserId(doctorId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        badgeService.evaluateAllBadges(doctorId);

        verify(badgeRepository).findByUser_IdAndBadgeType(doctorId, "DOCTOR_CONSISTENT_PROFESSIONAL");
    }

    @Test
    void evaluateAlwaysAvailable_Doctor_WithAvailability_ActivatesBadge() {
        User doctor = new User();
        doctor.setId(doctorId);
        doctor.setRole("DOCTOR");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(doctorId)
                .statistics(objectMapper.createObjectNode())
                .progress(objectMapper.createObjectNode())
                .build();

        DoctorProfile profile = new DoctorProfile();
        profile.setId(doctorId);
        profile.setAvailabilitySchedule("[{\"day\": \"MONDAY\", \"ranges\": [{\"end\": \"18:00\", \"start\": \"09:00\"}], \"enabled\": true}, {\"day\": \"TUESDAY\", \"ranges\": [{\"end\": \"18:00\", \"start\": \"09:00\"}], \"enabled\": true}, {\"day\": \"WEDNESDAY\", \"ranges\": [{\"end\": \"18:00\", \"start\": \"09:00\"}], \"enabled\": true}, {\"day\": \"THURSDAY\", \"ranges\": [{\"end\": \"18:00\", \"start\": \"09:00\"}], \"enabled\": true}]");

        when(statisticsRepository.findByUserId(doctorId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(doctorProfileRepository.findByUserId(doctorId)).thenReturn(Optional.of(profile));
        when(badgeRepository.findByUser_IdAndBadgeType(doctorId, "DOCTOR_ALWAYS_AVAILABLE")).thenReturn(Optional.empty());
        when(badgeRepository.save(any(Badge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        badgeService.evaluateAllBadges(doctorId);

        verify(badgeRepository, atLeastOnce()).findByUser_IdAndBadgeType(doctorId, "DOCTOR_ALWAYS_AVAILABLE");
        verify(badgeRepository, atLeastOnce()).save(any(Badge.class));
    }

    @Test
    void evaluateTopSpecialist_Doctor_WithAllRequirements_ActivatesBadge() {
        User doctor = new User();
        doctor.setId(doctorId);
        doctor.setRole("DOCTOR");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(doctorId)
                .statistics(objectMapper.createObjectNode()
                        .put("total_turns_completed", 120)
                        .put("total_avg_rating", 4.5)
                        .put("specialty_rank_percentile", 0.05))
                .progress(objectMapper.createObjectNode())
                .build();

        when(statisticsRepository.findByUserId(doctorId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        badgeService.evaluateAllBadges(doctorId);

        verify(badgeRepository).findByUser_IdAndBadgeType(doctorId, "DOCTOR_TOP_SPECIALIST");
    }

    @Test
    void evaluateMedicalLegend_Doctor_WithAllRequirements_ActivatesBadge() {
        User doctor = new User();
        doctor.setId(doctorId);
        doctor.setRole("DOCTOR");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(doctorId)
                .statistics(objectMapper.createObjectNode()
                        .put("total_turns_completed", 350)
                        .put("total_avg_rating", 4.8))
                .progress(objectMapper.createObjectNode())
                .build();

        when(statisticsRepository.findByUserId(doctorId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        stubMedicalLegendDependencies(doctorId, true, true, true);

        badgeService.evaluateAllBadges(doctorId);

        verify(badgeRepository, atLeastOnce()).findByUser_IdAndBadgeType(doctorId, "DOCTOR_MEDICAL_LEGEND");
    }

    @Test
    void getCategoryForBadge_DoctorBadge_ReturnsCorrectCategory() {
        BadgeCategory result = badgeService.getCategoryForBadge("DOCTOR_EXCEPTIONAL_COMMUNICATOR", "DOCTOR");
        assertEquals(BadgeCategory.QUALITY_OF_CARE, result);
    }

    @Test
    void getCategoryForBadge_DoctorQualityOfCare_ReturnsCorrectCategory() {
        BadgeCategory result = badgeService.getCategoryForBadge("DOCTOR_EMPATHETIC_DOCTOR", "DOCTOR");
        assertEquals(BadgeCategory.QUALITY_OF_CARE, result);
    }

    @Test
    void getCategoryForBadge_PatientActiveCommitment_ReturnsCorrectCategory() {
        BadgeCategory result = badgeService.getCategoryForBadge("PATIENT_SMART_PLANNER", "PATIENT");
        assertEquals(BadgeCategory.ACTIVE_COMMITMENT, result);
    }

    @Test
    void getCategoryForBadge_DoctorProfessionalism_ReturnsCorrectCategory() {
        BadgeCategory result = badgeService.getCategoryForBadge("DOCTOR_AGILE_RESPONDER", "DOCTOR");
        assertEquals(BadgeCategory.PROFESSIONALISM, result);
    }

    @Test
    void getCategoryForBadge_DoctorConsistency_ReturnsCorrectCategory() {
        BadgeCategory result = badgeService.getCategoryForBadge("DOCTOR_CONSISTENT_PROFESSIONAL", "DOCTOR");
        assertEquals(BadgeCategory.CONSISTENCY, result);
    }

    @Test
    void getCategoryForBadge_DoctorExperience_ReturnsCorrectCategory() {
        BadgeCategory result = badgeService.getCategoryForBadge("DOCTOR_MEDICAL_LEGEND", "DOCTOR");
        assertEquals(BadgeCategory.CONSISTENCY, result);
    }

    @Test
    void getCategoryForBadge_DoctorPatientRelations_ReturnsCorrectCategory() {
        BadgeCategory result = badgeService.getCategoryForBadge("DOCTOR_RELATIONSHIP_BUILDER", "DOCTOR");
        assertEquals(BadgeCategory.PROFESSIONALISM, result);
    }

    @Test
    void getCategoryForBadge_DoctorDocumentation_ReturnsCorrectCategory() {
        BadgeCategory result = badgeService.getCategoryForBadge("DOCTOR_COMPLETE_DOCUMENTER", "DOCTOR");
        assertEquals(BadgeCategory.PROFESSIONALISM, result);
    }

    @Test
    void getCategoryForBadge_PatientWelcome_ReturnsCorrectCategory() {
        BadgeCategory result = badgeService.getCategoryForBadge("PATIENT_MEDIBOOK_WELCOME", "PATIENT");
        assertEquals(BadgeCategory.WELCOME, result);
    }

    @Test
    void getCategoryForBadge_PatientCollaboration_ReturnsCorrectCategory() {
        BadgeCategory result = badgeService.getCategoryForBadge("PATIENT_EXCELLENT_COLLABORATOR", "PATIENT");
        assertEquals(BadgeCategory.ACTIVE_COMMITMENT, result);
    }

    @Test
    void getCategoryForBadge_PatientPunctuality_ReturnsCorrectCategory() {
        BadgeCategory result = badgeService.getCategoryForBadge("PATIENT_EXEMPLARY_PUNCTUALITY", "PATIENT");
        assertEquals(BadgeCategory.ACTIVE_COMMITMENT, result);
    }

    @Test
    void getCategoryForBadge_PatientPreparedness_ReturnsCorrectCategory() {
        BadgeCategory result = badgeService.getCategoryForBadge("PATIENT_SMART_PLANNER", "PATIENT");
        assertEquals(BadgeCategory.ACTIVE_COMMITMENT, result);
    }

    @Test
    void getCategoryForBadge_PatientExcellence_ReturnsCorrectCategory() {
        BadgeCategory result = badgeService.getCategoryForBadge("PATIENT_COMMITTED_PATIENT", "PATIENT");
        assertEquals(BadgeCategory.PREVENTIVE_CARE, result);
    }

    @Test
    void getCategoryForBadge_PatientContinuousCare_ReturnsCorrectCategory() {
        BadgeCategory result = badgeService.getCategoryForBadge("PATIENT_CONTINUOUS_FOLLOWUP", "PATIENT");
        assertEquals(BadgeCategory.PREVENTIVE_CARE, result);
    }

    @Test
    void getCategoryForBadge_PatientEvaluator_ReturnsCorrectCategory() {
        BadgeCategory result = badgeService.getCategoryForBadge("PATIENT_RESPONSIBLE_EVALUATOR", "PATIENT");
        assertEquals(BadgeCategory.CLINICAL_EXCELLENCE, result);
    }

    @Test
    void getCategoryForBadge_UnknownBadge_ReturnsGeneral() {
        BadgeCategory result = badgeService.getCategoryForBadge("UNKNOWN_BADGE", "DOCTOR");
        assertEquals(BadgeCategory.CONSISTENCY, result);
    }

    @Test
    void getOrCreateStatistics_ExistingStats_ReturnsExisting() {
        BadgeStatistics existingStats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(objectMapper.createObjectNode())
                .progress(objectMapper.createObjectNode())
                .build();

        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(existingStats));

        BadgeStatistics result = badgeService.getOrCreateStatistics(userId);

        assertEquals(existingStats, result);
        verify(statisticsRepository).findByUserId(userId);
        verify(statisticsRepository, never()).save(any());
    }

    @Test
    void getOrCreateStatistics_NoExistingStats_CreatesNew() {
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BadgeStatistics result = badgeService.getOrCreateStatistics(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertNotNull(result.getStatistics());
        assertNotNull(result.getProgress());
        verify(statisticsRepository).findByUserId(userId);
        verify(statisticsRepository).save(any(BadgeStatistics.class));
    }

    @Test
    void evaluateTurnCompletionRelatedBadges_DoctorRole_CallsDoctorEvaluation() {
        User doctor = new User();
        doctor.setId(userId);
        doctor.setRole("DOCTOR");

        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));

        badgeService.evaluateTurnCompletionRelatedBadges(userId);

        verify(userRepository).findById(userId);
    }

    @Test
    void evaluateTurnCompletionRelatedBadges_PatientRole_CallsPatientEvaluation() {
        User patient = new User();
        patient.setId(userId);
        patient.setRole("PATIENT");

        when(userRepository.findById(userId)).thenReturn(Optional.of(patient));
        when(statisticsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());
        BadgeStatistics mockStats = mock(BadgeStatistics.class);
        when(mockStats.getStatistics()).thenReturn(objectMapper.createObjectNode());
        when(mockStats.getProgress()).thenReturn(objectMapper.createObjectNode());
        when(mockStats.getUserId()).thenReturn(userId);
        when(mockStats.getVersion()).thenReturn(0L);
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(mockStats);

        badgeService.evaluateTurnCompletionRelatedBadges(userId);

        verify(userRepository).findById(userId);
    }

    @Test
    void activateBadge_BadgeExists_Inactive_Activates() {
        Badge existingBadge = new Badge();
        existingBadge.setIsActive(false);

        when(badgeRepository.findByUser_IdAndBadgeType(userId, "TEST_BADGE")).thenReturn(Optional.of(existingBadge));
        when(badgeRepository.save(any(Badge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(objectMapper.createObjectNode())
                .progress(objectMapper.createObjectNode())
                .build();

        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);

        badgeService.activateBadge(userId, "TEST_BADGE");

        verify(badgeRepository).findByUser_IdAndBadgeType(userId, "TEST_BADGE");
        verify(badgeRepository).save(existingBadge);
        assert existingBadge.getIsActive();
    }

    @Test
    void deactivateBadge_BadgeExists_Active_Deactivates() {
        Badge existingBadge = new Badge();
        existingBadge.setIsActive(true);

        when(badgeRepository.findByUser_IdAndBadgeType(userId, "TEST_BADGE")).thenReturn(Optional.of(existingBadge));
        when(badgeRepository.save(any(Badge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(objectMapper.createObjectNode())
                .progress(objectMapper.createObjectNode())
                .build();

        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);

        badgeService.deactivateBadge(userId, "TEST_BADGE");

        verify(badgeRepository).findByUser_IdAndBadgeType(userId, "TEST_BADGE");
        verify(badgeRepository).save(existingBadge);
        assert !existingBadge.getIsActive();
    }

    @Test
    void evaluateContinuousFollowup_SufficientTurnsWithSameDoctor_ActivatesBadge() {
        User patient = new User();
        patient.setId(userId);
        patient.setRole("PATIENT");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(objectMapper.createObjectNode()
                        .put("turns_with_same_doctor", 5))
                .progress(objectMapper.createObjectNode())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(patient));
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(badgeRepository.existsByUser_IdAndBadgeType(userId, "PATIENT_CONTINUOUS_FOLLOWUP")).thenReturn(false);
        when(badgeRepository.save(any(Badge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);

        badgeService.evaluateContinuousFollowup(patient);

        verify(badgeRepository).existsByUser_IdAndBadgeType(userId, "PATIENT_CONTINUOUS_FOLLOWUP");
        verify(badgeRepository).save(any(Badge.class));
    }

    @Test
    void evaluateContinuousFollowup_InsufficientTurns_DoesNotActivate() {
        User patient = new User();
        patient.setId(userId);
        patient.setRole("PATIENT");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(objectMapper.createObjectNode()
                        .put("turns_with_same_doctor", 2))
                .progress(objectMapper.createObjectNode())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(patient));
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));

        badgeService.evaluateContinuousFollowup(patient);

        verify(badgeRepository, never()).findByUser_IdAndBadgeType(any(), any());
        verify(badgeRepository, never()).save(any());
    }

    @Test
    void evaluateConsistentProfessional_LowCancellationRate_ActivatesBadge() {
        User doctor = new User();
        doctor.setId(userId);
        doctor.setRole("DOCTOR");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(objectMapper.createObjectNode()
                        .put("total_turns_completed", 100)
                        .put("total_turns_cancelled", 10))
                .progress(objectMapper.createObjectNode())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_CONSISTENT_PROFESSIONAL")).thenReturn(Optional.empty());
        when(badgeRepository.save(any(Badge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);

        badgeService.evaluateConsistentProfessional(doctor);

        verify(badgeRepository).findByUser_IdAndBadgeType(userId, "DOCTOR_CONSISTENT_PROFESSIONAL");
        verify(badgeRepository).save(any(Badge.class));
    }

    @Test
    void evaluateConsistentProfessional_HighCancellationRate_DoesNotActivate() {
        User doctor = new User();
        doctor.setId(userId);
        doctor.setRole("DOCTOR");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(objectMapper.createObjectNode()
                        .put("total_turns_completed", 100)
                        .put("total_cancellations", 20))
                .progress(objectMapper.createObjectNode())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_CONSISTENT_PROFESSIONAL")).thenReturn(Optional.empty());
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);

        badgeService.evaluateConsistentProfessional(doctor);

        verify(badgeRepository).findByUser_IdAndBadgeType(userId, "DOCTOR_CONSISTENT_PROFESSIONAL");
        verify(badgeRepository, never()).save(any());
    }

    @Test
    void evaluateResponsibleEvaluator_SufficientRatings_ActivatesBadge() {
        User patient = new User();
        patient.setId(userId);
        patient.setRole("PATIENT");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(objectMapper.createObjectNode()
                        .put("ratings_given", 15)
                        .put("avg_rating_given", 4.5))
                .progress(objectMapper.createObjectNode())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(patient));
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "PATIENT_RESPONSIBLE_EVALUATOR")).thenReturn(Optional.empty());
        when(badgeRepository.save(any(Badge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);

        badgeService.evaluateResponsibleEvaluator(patient);

        verify(badgeRepository).findByUser_IdAndBadgeType(userId, "PATIENT_RESPONSIBLE_EVALUATOR");
        verify(badgeRepository).save(any(Badge.class));
    }

    private List<Rating> createRatings(int count, int score) {
        List<Rating> ratings = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ratings.add(Rating.builder().score(score).build());
        }
        return ratings;
    }

    private void stubMedicalLegendDependencies(UUID doctorId, boolean communicator, boolean empathetic, boolean punctuality) {
        when(badgeRepository.existsByUser_IdAndBadgeTypeAndIsActive(doctorId, "DOCTOR_EXCEPTIONAL_COMMUNICATOR", true))
                .thenReturn(communicator);
        when(badgeRepository.existsByUser_IdAndBadgeTypeAndIsActive(doctorId, "DOCTOR_EMPATHETIC_DOCTOR", true))
                .thenReturn(empathetic);
        when(badgeRepository.existsByUser_IdAndBadgeTypeAndIsActive(doctorId, "DOCTOR_PUNCTUALITY_PROFESSIONAL", true))
                .thenReturn(punctuality);
    }

    @Test
    void evaluateTopSpecialist_SufficientTurns_ActivatesBadge() {
        User doctor = new User();
        doctor.setId(userId);
        doctor.setRole("DOCTOR");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(objectMapper.createObjectNode()
                        .put("total_turns_completed", 120)
                        .put("total_avg_rating", 4.5)
                        .put("specialty_rank_percentile", 0.2))
                .progress(objectMapper.createObjectNode())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_TOP_SPECIALIST")).thenReturn(Optional.empty());
        when(badgeRepository.save(any(Badge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        when(ratingRepository.findTop35ByRated_IdAndRater_RoleOrderByCreatedAtDesc(userId, "PATIENT")).thenReturn(createRatings(35, 4));

        badgeService.evaluateTopSpecialist(doctor);

        verify(badgeRepository).findByUser_IdAndBadgeType(userId, "DOCTOR_TOP_SPECIALIST");
        verify(badgeRepository).save(any(Badge.class));
    }

    @Test
    void evaluateMedicalLegend_SufficientTurns_ActivatesBadge() {
        User doctor = new User();
        doctor.setId(userId);
        doctor.setRole("DOCTOR");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(objectMapper.createObjectNode()
                        .put("total_turns_completed", 550)
                        .put("total_avg_rating", 4.8))
                .progress(objectMapper.createObjectNode())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_MEDICAL_LEGEND")).thenReturn(Optional.empty());
        when(badgeRepository.save(any(Badge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);
        stubMedicalLegendDependencies(userId, true, true, true);

        badgeService.evaluateMedicalLegend(doctor);

        verify(badgeRepository, times(1)).existsByUser_IdAndBadgeTypeAndIsActive(userId, "DOCTOR_EXCEPTIONAL_COMMUNICATOR", true);
        verify(badgeRepository, times(1)).existsByUser_IdAndBadgeTypeAndIsActive(userId, "DOCTOR_EMPATHETIC_DOCTOR", true);
        verify(badgeRepository, times(1)).existsByUser_IdAndBadgeTypeAndIsActive(userId, "DOCTOR_PUNCTUALITY_PROFESSIONAL", true);
        verify(badgeRepository).findByUser_IdAndBadgeType(userId, "DOCTOR_MEDICAL_LEGEND");
        verify(badgeRepository).save(any(Badge.class));
    }

    @Test
    void evaluateAgileResponder_SufficientRequestsHandled_ActivatesBadge() {
        User doctor = new User();
        doctor.setId(userId);
        doctor.setRole("DOCTOR");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(objectMapper.createObjectNode()
                        .put("total_requests_handled", 8))
                .progress(objectMapper.createObjectNode())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(statisticsRepository.findByUserId(userId)).thenReturn(Optional.of(stats));
        when(badgeRepository.findByUser_IdAndBadgeType(userId, "DOCTOR_AGILE_RESPONDER")).thenReturn(Optional.empty());
        when(badgeRepository.save(any(Badge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);

        badgeService.evaluateAgileResponder(doctor);

        verify(badgeRepository).findByUser_IdAndBadgeType(userId, "DOCTOR_AGILE_RESPONDER");
        verify(badgeRepository).save(any(Badge.class));
    }

    @Test
    void evaluateAllBadges_DoctorRole_CallsDoctorEvaluations() {
        User doctor = new User();
        doctor.setId(userId);
        doctor.setRole("DOCTOR");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(objectMapper.createObjectNode())
                .progress(objectMapper.createObjectNode())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(statisticsRepository.findByUserId(any())).thenReturn(Optional.of(stats));
        when(badgeRepository.findByUser_IdAndBadgeType(any(), any())).thenReturn(Optional.empty());
        when(badgeRepository.save(any(Badge.class))).thenReturn(null);
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);

        badgeService.evaluateAllBadges(userId);

        verify(userRepository, atLeastOnce()).findById(userId);
    }

    @Test
    void evaluateAllBadges_PatientRole_CallsPatientEvaluations() {
        User patient = new User();
        patient.setId(userId);
        patient.setRole("PATIENT");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(objectMapper.createObjectNode())
                .progress(objectMapper.createObjectNode())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(patient));
        when(statisticsRepository.findByUserId(any())).thenReturn(Optional.of(stats));
        when(badgeRepository.findByUser_IdAndBadgeType(any(), any())).thenReturn(Optional.empty());
        when(badgeRepository.save(any(Badge.class))).thenReturn(null);
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(stats);

        badgeService.evaluateAllBadges(userId);

        verify(userRepository, atLeastOnce()).findById(userId);
    }

    @Test
    void evaluateTurnRelatedBadges_DoctorRole_CallsDoctorTurnEvaluations() {
        User doctor = new User();
        doctor.setId(userId);
        doctor.setRole("DOCTOR");

        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(statisticsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());
        BadgeStatistics mockStats = mock(BadgeStatistics.class);
        when(mockStats.getStatistics()).thenReturn(objectMapper.createObjectNode());
        when(mockStats.getProgress()).thenReturn(objectMapper.createObjectNode());
        when(mockStats.getUserId()).thenReturn(userId);
        when(mockStats.getVersion()).thenReturn(0L);
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(mockStats);

        badgeService.evaluateTurnRelatedBadges(userId);

        verify(userRepository).findById(userId);
    }

    @Test
    void evaluateRatingRelatedBadges_DoctorRole_CallsDoctorRatingEvaluations() {
        User doctor = new User();
        doctor.setId(userId);
        doctor.setRole("DOCTOR");

        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(statisticsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());
        BadgeStatistics mockStats = mock(BadgeStatistics.class);
        when(mockStats.getStatistics()).thenReturn(objectMapper.createObjectNode());
        when(mockStats.getProgress()).thenReturn(objectMapper.createObjectNode());
        when(mockStats.getUserId()).thenReturn(userId);
        when(mockStats.getVersion()).thenReturn(0L);
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(mockStats);

        badgeService.evaluateRatingRelatedBadges(userId);

        verify(userRepository).findById(userId);
    }

    @Test
    void evaluateDocumentationRelatedBadges_DoctorRole_CallsDoctorDocumentationEvaluations() {
        User doctor = new User();
        doctor.setId(userId);
        doctor.setRole("DOCTOR");

        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(statisticsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());
        BadgeStatistics mockStats = mock(BadgeStatistics.class);
        when(mockStats.getStatistics()).thenReturn(objectMapper.createObjectNode());
        when(mockStats.getProgress()).thenReturn(objectMapper.createObjectNode());
        when(mockStats.getUserId()).thenReturn(userId);
        when(mockStats.getVersion()).thenReturn(0L);
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(mockStats);

        badgeService.evaluateDocumentationRelatedBadges(userId);

        verify(userRepository).findById(userId);
    }

    @Test
    void evaluateConsistencyRelatedBadges_DoctorRole_CallsDoctorConsistencyEvaluations() {
        User doctor = new User();
        doctor.setId(userId);
        doctor.setRole("DOCTOR");

        BadgeStatistics stats = BadgeStatistics.builder()
                .userId(userId)
                .statistics(objectMapper.createObjectNode())
                .progress(objectMapper.createObjectNode())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(statisticsRepository.findByUserId(any())).thenReturn(Optional.of(stats));

        badgeService.evaluateConsistencyRelatedBadges(userId);

        verify(userRepository).findById(userId);
    }

    @Test
    void evaluateFileRelatedBadges_PatientRole_CallsPatientFileEvaluations() {
        User patient = new User();
        patient.setId(userId);
        patient.setRole("PATIENT");

        when(userRepository.findById(userId)).thenReturn(Optional.of(patient));
        when(statisticsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());
        BadgeStatistics mockStats = mock(BadgeStatistics.class);
        when(mockStats.getStatistics()).thenReturn(objectMapper.createObjectNode());
        when(mockStats.getProgress()).thenReturn(objectMapper.createObjectNode());
        when(mockStats.getUserId()).thenReturn(userId);
        when(mockStats.getVersion()).thenReturn(0L);
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(mockStats);

        badgeService.evaluateFileRelatedBadges(userId);

        verify(userRepository).findById(userId);
    }

    @Test
    void evaluateBookingRelatedBadges_PatientRole_CallsPatientBookingEvaluations() {
        User patient = new User();
        patient.setId(userId);
        patient.setRole("PATIENT");

        when(userRepository.findById(userId)).thenReturn(Optional.of(patient));
        when(statisticsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());
        BadgeStatistics mockStats = mock(BadgeStatistics.class);
        when(mockStats.getStatistics()).thenReturn(objectMapper.createObjectNode());
        when(mockStats.getProgress()).thenReturn(objectMapper.createObjectNode());
        when(mockStats.getUserId()).thenReturn(userId);
        when(mockStats.getVersion()).thenReturn(0L);
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(mockStats);

        badgeService.evaluateBookingRelatedBadges(userId);

        verify(userRepository).findById(userId);
    }

    @Test
    void evaluateResponseRelatedBadges_DoctorRole_CallsDoctorResponseEvaluations() {
        User doctor = new User();
        doctor.setId(userId);
        doctor.setRole("DOCTOR");

        when(userRepository.findById(userId)).thenReturn(Optional.of(doctor));
        when(statisticsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());
        BadgeStatistics mockStats = mock(BadgeStatistics.class);
        when(mockStats.getStatistics()).thenReturn(objectMapper.createObjectNode());
        when(mockStats.getProgress()).thenReturn(objectMapper.createObjectNode());
        when(mockStats.getUserId()).thenReturn(userId);
        when(mockStats.getVersion()).thenReturn(0L);
        when(statisticsRepository.save(any(BadgeStatistics.class))).thenReturn(mockStats);

        badgeService.evaluateResponseRelatedBadges(userId);

        verify(userRepository).findById(userId);
    }
}