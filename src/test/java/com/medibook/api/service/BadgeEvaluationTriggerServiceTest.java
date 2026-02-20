package com.medibook.api.service;

import com.medibook.api.entity.User;
import com.medibook.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BadgeEvaluationTriggerServiceTest {

    @Mock
    private BadgeService badgeService;

    @Mock
    private BadgeStatisticsUpdateService statisticsUpdateService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BadgeEvaluationTriggerService triggerService;

    private UUID doctorId;
    private UUID patientId;
    private User doctor;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        doctor = new User();
        doctor.setId(doctorId);
        doctor.setName("Dr. Test");
        doctor.setSurname("Doctor");
        doctor.setRole("DOCTOR");
        doctor.setStatus("ACTIVE");
    }

    @Test
    void evaluateAfterRating_ValidDoctor_UpdatesStatisticsAndEvaluatesBadges() {
        Integer communicationScore = 4;
        Integer empathyScore = 5;
        Integer punctualityScore = 4;

        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        triggerService.evaluateAfterRating(doctorId, communicationScore, empathyScore, punctualityScore);

        verify(userRepository).findById(doctorId);
        verify(statisticsUpdateService).updateAfterRatingAddedSync(doctorId, communicationScore, empathyScore, punctualityScore);
        verify(statisticsUpdateService).updateProgressAfterRatingSync(doctorId);
        verify(badgeService).evaluateRatingRelatedBadges(doctorId);
        verify(badgeService).evaluateTopSpecialist(doctor);
    }

    @Test
    void evaluateAfterRating_InvalidDoctorRole_HandlesGracefully() {
        User patient = new User();
        patient.setId(doctorId);
        patient.setRole("PATIENT");

        when(userRepository.findById(doctorId)).thenReturn(Optional.of(patient));

        assertDoesNotThrow(() ->
            triggerService.evaluateAfterRating(doctorId, 4, 5, 4));

        verify(userRepository).findById(doctorId);
        verifyNoInteractions(statisticsUpdateService);
        verifyNoInteractions(badgeService);
    }

    @Test
    void evaluateAfterRating_DoctorNotFound_HandlesGracefully() {
        when(userRepository.findById(doctorId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() ->
            triggerService.evaluateAfterRating(doctorId, 4, 5, 4));

        verify(userRepository).findById(doctorId);
        verifyNoInteractions(statisticsUpdateService);
        verifyNoInteractions(badgeService);
    }

    @Test
    void evaluateAfterRating_ExceptionInStatisticsUpdate_HandlesGracefully() {
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        doThrow(new RuntimeException("Statistics update failed"))
            .when(statisticsUpdateService).updateAfterRatingAddedSync(any(), any(), any(), any());

        assertDoesNotThrow(() ->
            triggerService.evaluateAfterRating(doctorId, 4, 5, 4));

        verify(userRepository).findById(doctorId);
        verify(statisticsUpdateService).updateAfterRatingAddedSync(doctorId, 4, 5, 4);
        verify(statisticsUpdateService, never()).updateProgressAfterRatingSync(any());
        verify(badgeService, never()).evaluateRatingRelatedBadges(any());
        verify(badgeService, never()).evaluateTopSpecialist(any());
    }

    @Test
    void evaluateAfterTurnCompletion_ValidDoctor_UpdatesStatisticsAndEvaluatesBadges() {
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        triggerService.evaluateAfterTurnCompletion(doctorId, patientId);

        verify(userRepository).findById(doctorId);
        verify(statisticsUpdateService).updateAfterTurnCompletedSync(doctorId, patientId);
        verify(statisticsUpdateService).updateProgressAfterTurnCompletionSync(doctorId);
        verify(badgeService).evaluateTurnCompletionRelatedBadges(doctorId);
    }

    @Test
    void evaluateAfterTurnCompletion_Success() {
        User patient = new User();
        patient.setId(doctorId);
        patient.setRole("PATIENT");

        when(userRepository.findById(doctorId)).thenReturn(Optional.of(patient));

        assertDoesNotThrow(() ->
            triggerService.evaluateAfterTurnCompletion(doctorId, patientId));

        verify(userRepository).findById(doctorId);
        verify(statisticsUpdateService).updateAfterTurnCompletedSync(doctorId, patientId);
        verify(statisticsUpdateService).updateProgressAfterTurnCompletionSync(doctorId);
        verify(badgeService).evaluateTurnCompletionRelatedBadges(doctorId);
    }

    @Test
    void evaluateAfterMedicalHistoryDocumented_ValidDoctor_UpdatesStatisticsAndEvaluatesBadges() {
        String content = "Patient shows symptoms of diabetes";

        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        triggerService.evaluateAfterMedicalHistoryDocumented(doctorId, content);

        verify(userRepository).findById(doctorId);
        verify(statisticsUpdateService).updateAfterMedicalHistoryDocumentedSync(doctorId, content);
        verify(statisticsUpdateService).updateProgressAfterMedicalHistorySync(doctorId);
        verify(badgeService).evaluateDocumentationRelatedBadges(doctorId);
    }

    @Test
    void evaluateAfterMedicalHistoryDocumented_NullContent_HandlesGracefully() {
        String content = null;

        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        triggerService.evaluateAfterMedicalHistoryDocumented(doctorId, content);

        verify(userRepository).findById(doctorId);
        verify(statisticsUpdateService).updateAfterMedicalHistoryDocumentedSync(doctorId, content);
        verify(statisticsUpdateService).updateProgressAfterMedicalHistorySync(doctorId);
        verify(badgeService).evaluateDocumentationRelatedBadges(doctorId);
    }

    @Test
    void evaluateAfterModifyRequestHandled_ValidDoctor_UpdatesStatisticsAndEvaluatesBadges() {
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        triggerService.evaluateAfterModifyRequestHandled(doctorId);

        verify(userRepository).findById(doctorId);
        verify(statisticsUpdateService).updateAfterModifyRequestHandledSync(doctorId);
        verify(statisticsUpdateService).updateProgressAfterModifyRequestSync(doctorId);
        verify(badgeService).evaluateResponseRelatedBadges(doctorId);
    }

    @Test
    void evaluateAfterTurnCancellation_ValidDoctor_UpdatesStatisticsAndEvaluatesBadges() {
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        triggerService.evaluateAfterTurnCancellation(doctorId);

        verify(userRepository).findById(doctorId);
        verify(statisticsUpdateService).updateAfterTurnCancelledSync(doctorId);
        verify(statisticsUpdateService).updateProgressAfterCancellationSync(doctorId);
        verify(badgeService).evaluateConsistencyRelatedBadges(doctorId);
    }

    @Test
    void evaluateAllBadges_ValidDoctor_UpdatesAllProgressAndEvaluatesBadges() {
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        triggerService.evaluateAllBadges(doctorId);

        verify(userRepository).findById(doctorId);
        verify(statisticsUpdateService).updateAllBadgeProgress(doctorId);
        verify(badgeService).evaluateAllBadges(doctorId);
    }

    @Test
    void evaluateAllBadges_Success() {
        User patient = new User();
        patient.setId(doctorId);
        patient.setRole("PATIENT");

        when(userRepository.findById(doctorId)).thenReturn(Optional.of(patient));

        assertDoesNotThrow(() -> triggerService.evaluateAllBadges(doctorId));

        verify(userRepository).findById(doctorId);
        verify(statisticsUpdateService).updateAllBadgeProgress(doctorId);
        verify(badgeService).evaluateAllBadges(doctorId);
    }

    @Test
    void evaluateAllBadges_ExceptionInProgressUpdate_HandlesGracefully() {
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        doThrow(new RuntimeException("Progress update failed"))
            .when(statisticsUpdateService).updateAllBadgeProgress(doctorId);

        assertDoesNotThrow(() -> triggerService.evaluateAllBadges(doctorId));

        verify(userRepository).findById(doctorId);
        verify(statisticsUpdateService).updateAllBadgeProgress(doctorId);
        verify(badgeService, never()).evaluateAllBadges(any());
    }

    @Test
    void evaluateAfterTurnNoShow_ValidDoctor_UpdatesStatisticsAndEvaluatesBadges() {
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        triggerService.evaluateAfterTurnNoShow(doctorId);

        verify(userRepository).findById(doctorId);
        verify(statisticsUpdateService).updateAfterTurnNoShowSync(doctorId);
        verify(statisticsUpdateService).updateProgressAfterCancellationSync(doctorId);
        verify(badgeService).evaluateConsistencyRelatedBadges(doctorId);
    }

    @Test
    void evaluateAfterTurnNoShow_InvalidDoctorRole_HandlesGracefully() {
        User patient = new User();
        patient.setId(doctorId);
        patient.setRole("PATIENT");

        when(userRepository.findById(doctorId)).thenReturn(Optional.of(patient));

        assertDoesNotThrow(() -> triggerService.evaluateAfterTurnNoShow(doctorId));

        verify(userRepository).findById(doctorId);
        verifyNoInteractions(statisticsUpdateService);
        verifyNoInteractions(badgeService);
    }

    @Test
    void evaluateAfterAdvanceBooking_ValidPatient_UpdatesStatisticsAndEvaluatesBadges() {
        User patient = new User();
        patient.setId(patientId);
        patient.setRole("PATIENT");

        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));

        triggerService.evaluateAfterAdvanceBooking(patientId);

        verify(userRepository).findById(patientId);
        verify(statisticsUpdateService).updateAfterAdvanceBookingSync(patientId);
        verify(statisticsUpdateService).updateProgressAfterBookingSync(patientId);
        verify(badgeService).evaluateBookingRelatedBadges(patientId);
    }

    @Test
    void evaluateAfterAdvanceBooking_InvalidPatientRole_HandlesGracefully() {
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        assertDoesNotThrow(() -> triggerService.evaluateAfterAdvanceBooking(doctorId));

        verify(userRepository).findById(doctorId);
        verifyNoInteractions(statisticsUpdateService);
        verifyNoInteractions(badgeService);
    }

    @Test
    void evaluateAfterRatingGiven_ValidPatient_UpdatesStatisticsAndEvaluatesBadges() {
        User patient = new User();
        patient.setId(patientId);
        patient.setRole("PATIENT");

        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));

        triggerService.evaluateAfterRatingGiven(patientId);

        verify(userRepository).findById(patientId);
        verify(statisticsUpdateService).updateAfterRatingGivenSync(patientId);
        verify(statisticsUpdateService).updateProgressAfterRatingSync(patientId);
        verify(badgeService).evaluateRatingRelatedBadges(patientId);
    }

    @Test
    void evaluateAfterRatingGiven_InvalidPatientRole_HandlesGracefully() {
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        assertDoesNotThrow(() -> triggerService.evaluateAfterRatingGiven(doctorId));

        verify(userRepository).findById(doctorId);
        verifyNoInteractions(statisticsUpdateService);
        verifyNoInteractions(badgeService);
    }

    @Test
    void evaluateAfterRatingReceived_ValidUser_UpdatesStatisticsAndEvaluatesBadges() {
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        triggerService.evaluateAfterRatingReceived(doctorId);

        verify(userRepository).findById(doctorId);
        verify(statisticsUpdateService).updateAfterRatingReceivedSync(doctorId);
        verify(statisticsUpdateService).updateProgressAfterRatingSync(doctorId);
        verify(badgeService).evaluateRatingRelatedBadges(doctorId);
    }

    @Test
    void evaluateAfterFileUploaded_ValidPatient_UpdatesStatisticsAndEvaluatesBadges() {
        User patient = new User();
        patient.setId(patientId);
        patient.setRole("PATIENT");

        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));

        triggerService.evaluateAfterFileUploaded(patientId);

        verify(userRepository).findById(patientId);
        verify(statisticsUpdateService).updateAfterFileUploadedSync(patientId);
        verify(statisticsUpdateService).updateProgressAfterFileUploadSync(patientId);
        verify(badgeService).evaluateFileRelatedBadges(patientId);
    }

    @Test
    void evaluateAfterFileUploaded_InvalidPatientRole_HandlesGracefully() {
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        assertDoesNotThrow(() -> triggerService.evaluateAfterFileUploaded(doctorId));

        verify(userRepository).findById(doctorId);
        verifyNoInteractions(statisticsUpdateService);
        verifyNoInteractions(badgeService);
    }

    @Test
    void evaluateAfterAvailabilityConfigured_ValidDoctor_EvaluatesAlwaysAvailableBadge() {
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        triggerService.evaluateAfterAvailabilityConfigured(doctorId);

        verify(userRepository).findById(doctorId);
        verify(badgeService).evaluateAlwaysAvailable(doctor);
    }

    @Test
    void evaluateAfterAvailabilityConfigured_InvalidDoctorRole_HandlesGracefully() {
        User patient = new User();
        patient.setId(doctorId);
        patient.setRole("PATIENT");

        when(userRepository.findById(doctorId)).thenReturn(Optional.of(patient));

        assertDoesNotThrow(() -> triggerService.evaluateAfterAvailabilityConfigured(doctorId));

        verify(userRepository).findById(doctorId);
        verifyNoInteractions(badgeService);
    }
}