package com.medibook.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medibook.api.dto.Badge.*;
import com.medibook.api.entity.*;
import com.medibook.api.entity.BadgeType.BadgeCategory;
import com.medibook.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unused")
public class BadgeService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MEDIBOOK_WELCOME_MIN_TURNS = 1;
    private static final int CONSTANT_PATIENT_MIN_TURNS_TOTAL = 15;
    private static final double CONSTANT_PATIENT_ATTENDANCE_RATE = 0.75;
    private static final int CONTINUOUS_FOLLOWUP_MIN_TURNS_SAME_DOCTOR = 3;
    private static final int EXEMPLARY_PUNCTUALITY_MIN_POSITIVE_RATINGS = 10;
    private static final int EXEMPLARY_PUNCTUALITY_MIN_TURNS = 8;
    private static final int ALWAYS_PREPARED_MIN_DOCUMENTS = 10;
    private static final int RESPONSIBLE_EVALUATOR_MIN_RATINGS = 10;
    private static final int RESPONSIBLE_EVALUATOR_MIN_SUBCATEGORIES = 2;
    private static final double RESPONSIBLE_EVALUATOR_MIN_AVG_RATING = 3.0;
    private static final double RESPONSIBLE_EVALUATOR_MAX_AVG_RATING = 5.0;
    private static final int EXCELLENCE_MODEL_MIN_OTHER_BADGES = 4;
    private static final int EXCELLENCE_MODEL_MIN_TURNS = 25;

    private static final int EXCEPTIONAL_COMMUNICATOR_THRESHOLD = 25;
    private static final int EMPATHETIC_DOCTOR_THRESHOLD = 25;
    private static final int PUNCTUALITY_PROFESSIONAL_THRESHOLD = 20;
    private static final double PUNCTUALITY_CANCELLATION_MAX = 0.15;
    private static final int COMPLETE_DOCUMENTER_THRESHOLD = 35;
    private static final double DETAILED_HISTORIAN_THRESHOLD = 0.9;
    private static final double DETAILED_HISTORIAN_MIN_WORDS = 150.0;
    private static final int RELATIONSHIP_BUILDER_MIN_PATIENTS = 25;
    private static final int RELATIONSHIP_BUILDER_RETURNING_MIN = 10;
        private static final int CONSISTENT_PROFESSIONAL_MIN_TURNS = 80;
        private static final double CONSISTENT_PROFESSIONAL_CANCELLATION_MAX = 0.15;
        private static final int TOP_SPECIALIST_REQUIRED_RATINGS = 35;
        static final List<String> MEDICAL_LEGEND_REQUIRED_BADGES = List.of(
            "DOCTOR_EXCEPTIONAL_COMMUNICATOR",
            "DOCTOR_EMPATHETIC_DOCTOR",
            "DOCTOR_PUNCTUALITY_PROFESSIONAL"
        );

    private final BadgeRepository badgeRepository;
    private final UserRepository userRepository;
    private final BadgeStatisticsRepository statisticsRepository;
    private final RatingRepository ratingRepository;
    private final TurnAssignedRepository turnAssignedRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final BadgeProgressService badgeProgressService;

    @Transactional(readOnly = true)
    public List<BadgeProgressSummaryDTO> getUserBadgeProgress(UUID userId) {
        return badgeProgressService.getBadgeProgress(userId);
    }

    @Transactional(readOnly = true)
    public BadgesResponseDTO getUserBadges(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Badge> allBadges = badgeRepository.findByUser_IdOrderByEarnedAtDesc(userId);

        Map<BadgeCategory, List<BadgeDTO>> badgesByCategory = allBadges.stream()
                .map(badge -> toBadgeDTO(badge, user.getRole()))
                .collect(Collectors.groupingBy(BadgeDTO::getCategory));

        long activeBadges = allBadges.stream().filter(Badge::getIsActive).count();

        return BadgesResponseDTO.builder()
                .userId(userId)
                .userName(user.getName() + " " + user.getSurname())
                .role(user.getRole())
                .totalActiveBadges((int) activeBadges)
                .badgesByCategory(badgesByCategory)
                .build();
    }

    @Transactional
    public void evaluateTurnRelatedBadges(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("PATIENT".equals(user.getRole())) {
            evaluateMediBookWelcome(user);
            evaluateCommittedPatient(user);
            evaluateContinuousFollowup(user);
            evaluateConstantPatient(user);
        } else if ("DOCTOR".equals(user.getRole())) {
            evaluateRelationshipBuilder(user);
            evaluateTopSpecialist(user);
            evaluateMedicalLegend(user);
        }
    }

    @Transactional
    public void evaluateRatingRelatedBadges(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("PATIENT".equals(user.getRole())) {
            evaluateExemplaryPunctuality(user);
            evaluateResponsibleEvaluator(user);
            evaluateExcellentCollaborator(user);
            evaluateExcellenceModel(user);
        } else if ("DOCTOR".equals(user.getRole())) {
            evaluateExceptionalCommunicator(user);
            evaluateEmpatheticDoctor(user);
            evaluatePunctualityProfessional(user);
        }
    }

    @Transactional
    public void evaluateFileRelatedBadges(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("PATIENT".equals(user.getRole())) {
            evaluateAlwaysPrepared(user);
        }
    }

    @Transactional
    public void evaluateBookingRelatedBadges(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("PATIENT".equals(user.getRole())) {
            evaluateSmartPlanner(user);
        }
    }

    @Transactional
    public void evaluateDocumentationRelatedBadges(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("DOCTOR".equals(user.getRole())) {
            evaluateCompleteDocumenter(user);
            evaluateDetailedDiagnostician(user);
        }
    }

    @Transactional
    public void evaluateConsistencyRelatedBadges(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("DOCTOR".equals(user.getRole())) {
            evaluateConsistentProfessional(user);
            evaluateAlwaysAvailable(user);
        }
    }

    @Transactional
    public void evaluateResponseRelatedBadges(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("DOCTOR".equals(user.getRole())) {
            evaluateAgileResponder(user);
        }
    }

    @Transactional
    public void evaluateTurnCompletionRelatedBadges(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("DOCTOR".equals(user.getRole())) {
            evaluateRelationshipBuilder(user);
            evaluateTopSpecialist(user);
            evaluateMedicalLegend(user);
            evaluateConsistentProfessional(user);
        } else if ("PATIENT".equals(user.getRole())) {
            evaluateMediBookWelcome(user);
            evaluateCommittedPatient(user);
            evaluateContinuousFollowup(user);
            evaluateConstantPatient(user);
        }
    }

    @Transactional
    public void evaluateAllBadges(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("PATIENT".equals(user.getRole())) {
            evaluateTurnRelatedBadges(userId);
            evaluateRatingRelatedBadges(userId);
            evaluateFileRelatedBadges(userId);
            evaluateBookingRelatedBadges(userId);
        } else if ("DOCTOR".equals(user.getRole())) {
            evaluateTurnRelatedBadges(userId);
            evaluateRatingRelatedBadges(userId);
            evaluateDocumentationRelatedBadges(userId);
            evaluateConsistencyRelatedBadges(userId);
            evaluateResponseRelatedBadges(userId);
            evaluateAlwaysAvailable(user);
        }
    }

    private void evaluateMediBookWelcome(User patient) {
        UUID patientId = patient.getId();
        try {
            BadgeStatistics stats = getOrCreateStatistics(patientId);
            JsonNode statistics = stats.getStatistics();

            int turnsCompleted = statistics.path("total_turns_completed").asInt(0);
            double progress = turnsCompleted >= MEDIBOOK_WELCOME_MIN_TURNS ? 100.0 : ((double) turnsCompleted / MEDIBOOK_WELCOME_MIN_TURNS) * 100;

            updateProgress(patientId, "PATIENT_MEDIBOOK_WELCOME", progress);

            if (turnsCompleted >= MEDIBOOK_WELCOME_MIN_TURNS) {
                activateBadge(patientId, "PATIENT_MEDIBOOK_WELCOME");
            } else {
                deactivateBadge(patientId, "PATIENT_MEDIBOOK_WELCOME");
            }
        } catch (Exception e) {
            log.error("Error evaluating MEDIBOOK_WELCOME for patient {}", patientId, e);
        }
    }

    private void evaluateCommittedPatient(User patient) {
        UUID patientId = patient.getId();
        try {
            BadgeStatistics stats = getOrCreateStatistics(patientId);
            JsonNode statistics = stats.getStatistics();

            int totalTurnsCompleted = statistics.path("total_turns_completed").asInt(0);
            log.debug("Evaluating COMMITTED_PATIENT for patient {}: totalTurnsCompleted={}", patientId, totalTurnsCompleted);
            double progress = totalTurnsCompleted >= 5 ? 100.0 : ((double) totalTurnsCompleted / 5) * 100;
            log.debug("Calculated progress for COMMITTED_PATIENT: {}%", progress);

            updateProgress(patientId, "PATIENT_COMMITTED_PATIENT", progress);

            if (totalTurnsCompleted >= 5) {
                activateBadge(patientId, "PATIENT_COMMITTED_PATIENT");
            } else {
                deactivateBadge(patientId, "PATIENT_COMMITTED_PATIENT");
            }
        } catch (Exception e) {
            log.error("Error evaluating COMMITTED_PATIENT for patient {}", patientId, e);
        }
    }

    void evaluateContinuousFollowup(User patient) {
        UUID patientId = patient.getId();
        try {
            BadgeStatistics stats = getOrCreateStatistics(patientId);
            JsonNode statistics = stats.getStatistics();

            int turnsSameDoctor = statistics.path("turns_with_same_doctor").asInt(0);
            double progress = Math.min(((double) turnsSameDoctor / CONTINUOUS_FOLLOWUP_MIN_TURNS_SAME_DOCTOR) * 100, 100.0);

            updateProgress(patientId, "PATIENT_CONTINUOUS_FOLLOWUP", progress);

            if (progress >= 100.0 && !badgeRepository.existsByUser_IdAndBadgeType(patientId, "PATIENT_CONTINUOUS_FOLLOWUP")) {
                createBadge(patientId, "PATIENT_CONTINUOUS_FOLLOWUP");
            }
        } catch (Exception e) {
            log.error("Error evaluating CONTINUOUS_FOLLOWUP for patient {}", patientId, e);
        }
    }

    private void evaluateConstantPatient(User patient) {
        UUID patientId = patient.getId();
        try {
            BadgeStatistics stats = getOrCreateStatistics(patientId);
            JsonNode statistics = stats.getStatistics();

            int totalTurnsCompleted = statistics.path("total_turns_completed").asInt(0);

            if (totalTurnsCompleted < CONSTANT_PATIENT_MIN_TURNS_TOTAL) {
                deactivateBadge(patientId, "PATIENT_CONSTANT_PATIENT");
                updateProgress(patientId, "PATIENT_CONSTANT_PATIENT", ((double) totalTurnsCompleted / CONSTANT_PATIENT_MIN_TURNS_TOTAL) * 100);
                return;
            }

            activateBadge(patientId, "PATIENT_CONSTANT_PATIENT");
            updateProgress(patientId, "PATIENT_CONSTANT_PATIENT", 100.0);
        } catch (Exception e) {
            log.error("Error evaluating CONSTANT_PATIENT for patient {}", patientId, e);
        }
    }

    private void evaluateExemplaryPunctuality(User patient) {
        UUID patientId = patient.getId();
        try {
            BadgeStatistics stats = getOrCreateStatistics(patientId);
            JsonNode statistics = stats.getStatistics();

            int totalTurnsCompleted = statistics.path("total_turns_completed").asInt(0);
            int totalPunctualityCount = statistics.path("doctor_punctuality_mentions").asInt(0);

            if (totalPunctualityCount < EXEMPLARY_PUNCTUALITY_MIN_POSITIVE_RATINGS) {
                deactivateBadge(patientId, "PATIENT_EXEMPLARY_PUNCTUALITY");
                updateProgress(patientId, "PATIENT_EXEMPLARY_PUNCTUALITY", ((double) totalPunctualityCount / EXEMPLARY_PUNCTUALITY_MIN_POSITIVE_RATINGS) * 100);
                return;
            }

            updateProgress(patientId, "PATIENT_EXEMPLARY_PUNCTUALITY", 100.0);
            activateBadge(patientId, "PATIENT_EXEMPLARY_PUNCTUALITY");
        } catch (Exception e) {
            log.error("Error evaluating EXEMPLARY_PUNCTUALITY for patient {}", patientId, e);
        }
    }

    private void evaluateSmartPlanner(User patient) {
        UUID patientId = patient.getId();
        try {
            BadgeStatistics stats = getOrCreateStatistics(patientId);
            JsonNode statistics = stats.getStatistics();

            int totalAdvanceBookingCount = statistics.path("advance_bookings").asInt(0);

            if (totalAdvanceBookingCount < 10) {
                deactivateBadge(patientId, "PATIENT_SMART_PLANNER");
                updateProgress(patientId, "PATIENT_SMART_PLANNER", ((double) totalAdvanceBookingCount / 10) * 100);
                return;
            }

            updateProgress(patientId, "PATIENT_SMART_PLANNER", 100.0);
            activateBadge(patientId, "PATIENT_SMART_PLANNER");
        } catch (Exception e) {
            log.error("Error evaluating SMART_PLANNER for patient {}", patientId, e);
        }
    }

    private void evaluateExcellentCollaborator(User patient) {
        UUID patientId = patient.getId();
        try {
            BadgeStatistics stats = getOrCreateStatistics(patientId);
            JsonNode statistics = stats.getStatistics();

            int totalCollaborationCount = statistics.path("doctor_collaboration_mentions").asInt(0);

            if (totalCollaborationCount < 10) {
                deactivateBadge(patientId, "PATIENT_EXCELLENT_COLLABORATOR");
                updateProgress(patientId, "PATIENT_EXCELLENT_COLLABORATOR", ((double) totalCollaborationCount / 10) * 100);
                return;
            }

            updateProgress(patientId, "PATIENT_EXCELLENT_COLLABORATOR", 100.0);
            activateBadge(patientId, "PATIENT_EXCELLENT_COLLABORATOR");
        } catch (Exception e) {
            log.error("Error evaluating EXCELLENT_COLLABORATOR for patient {}", patientId, e);
        }
    }

    private void evaluateAlwaysPrepared(User patient) {
        UUID patientId = patient.getId();
        try {
            BadgeStatistics stats = getOrCreateStatistics(patientId);
            JsonNode statistics = stats.getStatistics();

            int totalFilesUploadedCount = statistics.path("files_uploaded").asInt(0);

            if (totalFilesUploadedCount < ALWAYS_PREPARED_MIN_DOCUMENTS) {
                deactivateBadge(patientId, "PATIENT_ALWAYS_PREPARED");
                updateProgress(patientId, "PATIENT_ALWAYS_PREPARED", ((double) totalFilesUploadedCount / ALWAYS_PREPARED_MIN_DOCUMENTS) * 100);
                return;
            }

            updateProgress(patientId, "PATIENT_ALWAYS_PREPARED", 100.0);
            activateBadge(patientId, "PATIENT_ALWAYS_PREPARED");
        } catch (Exception e) {
            log.error("Error evaluating ALWAYS_PREPARED for patient {}", patientId, e);
        }
    }

    void evaluateResponsibleEvaluator(User patient) {
        UUID patientId = patient.getId();
        try {
            BadgeStatistics stats = getOrCreateStatistics(patientId);
            JsonNode statistics = stats.getStatistics();

            int totalRatingsGiven = statistics.path("ratings_given").asInt(0);
            Double avgRatingGiven = statistics.path("avg_rating_given").asDouble();

            if (totalRatingsGiven < RESPONSIBLE_EVALUATOR_MIN_RATINGS) {
                deactivateBadge(patientId, "PATIENT_RESPONSIBLE_EVALUATOR");
                updateProgress(patientId, "PATIENT_RESPONSIBLE_EVALUATOR", ((double) totalRatingsGiven / RESPONSIBLE_EVALUATOR_MIN_RATINGS) * 100);
                return;
            }

            boolean hasConstructiveRatingRange = avgRatingGiven != null &&
                    avgRatingGiven >= RESPONSIBLE_EVALUATOR_MIN_AVG_RATING &&
                    avgRatingGiven <= RESPONSIBLE_EVALUATOR_MAX_AVG_RATING;

            double progress = Math.min(((double) totalRatingsGiven / RESPONSIBLE_EVALUATOR_MIN_RATINGS) * 100, 100.0);

            updateProgress(patientId, "PATIENT_RESPONSIBLE_EVALUATOR", progress);

            if (hasConstructiveRatingRange) {
                activateBadge(patientId, "PATIENT_RESPONSIBLE_EVALUATOR");
            } else {
                deactivateBadge(patientId, "PATIENT_RESPONSIBLE_EVALUATOR");
            }
        } catch (Exception e) {
            log.error("Error evaluating RESPONSIBLE_EVALUATOR for patient {}", patientId, e);
        }
    }

    private void evaluateExcellenceModel(User patient) {
        UUID patientId = patient.getId();
        try {
            BadgeStatistics stats = getOrCreateStatistics(patientId);
            JsonNode statistics = stats.getStatistics();

            int totalTurnsCompleted = statistics.path("total_turns_completed").asInt(0);
            long otherActiveBadges = badgeRepository.countActiveBadgesByUserIdExcludingType(patientId, "PATIENT_EXCELLENCE_MODEL");
            boolean hasRequiredOtherBadges = otherActiveBadges >= EXCELLENCE_MODEL_MIN_OTHER_BADGES;

            if (totalTurnsCompleted < EXCELLENCE_MODEL_MIN_TURNS) {
                deactivateBadge(patientId, "PATIENT_EXCELLENCE_MODEL");
                updateProgress(patientId, "PATIENT_EXCELLENCE_MODEL", ((double) totalTurnsCompleted / EXCELLENCE_MODEL_MIN_TURNS) * 100);
                return;
            }

            double badgeProgress = Math.min(((double) otherActiveBadges / EXCELLENCE_MODEL_MIN_OTHER_BADGES) * 100, 100);
            double turnProgress = Math.min(((double) totalTurnsCompleted / EXCELLENCE_MODEL_MIN_TURNS) * 100, 100);
            double progress = Math.min((badgeProgress + turnProgress) / 2, 100.0);

            updateProgress(patientId, "PATIENT_EXCELLENCE_MODEL", progress);

            if (hasRequiredOtherBadges) {
                activateBadge(patientId, "PATIENT_EXCELLENCE_MODEL");
            } else {
                deactivateBadge(patientId, "PATIENT_EXCELLENCE_MODEL");
            }
        } catch (Exception e) {
            log.error("Error evaluating EXCELLENCE_MODEL for patient {}", patientId, e);
        }
    }

    private void evaluateExceptionalCommunicator(User doctor) {
        UUID doctorId = doctor.getId();
        try {
            BadgeStatistics stats = getOrCreateStatistics(doctorId);
            JsonNode statistics = stats.getStatistics();

            int communicationCount = statistics.path("total_communication_count").asInt(0);
            double progress = Math.min(((double) communicationCount / EXCEPTIONAL_COMMUNICATOR_THRESHOLD) * 100, 100.0);

            updateProgress(doctorId, "DOCTOR_EXCEPTIONAL_COMMUNICATOR", progress);

            if (communicationCount >= EXCEPTIONAL_COMMUNICATOR_THRESHOLD) {
                activateBadge(doctorId, "DOCTOR_EXCEPTIONAL_COMMUNICATOR");
            } else {
                deactivateBadge(doctorId, "DOCTOR_EXCEPTIONAL_COMMUNICATOR");
            }
        } catch (Exception e) {
            log.error("Error evaluating EXCEPTIONAL_COMMUNICATOR for doctor {}", doctorId, e);
        }
    }

    private void evaluateEmpatheticDoctor(User doctor) {
        UUID doctorId = doctor.getId();
        try {
            BadgeStatistics stats = getOrCreateStatistics(doctorId);
            JsonNode statistics = stats.getStatistics();

            int empathyCount = statistics.path("total_empathy_count").asInt(0);
            double progress = Math.min(((double) empathyCount / EMPATHETIC_DOCTOR_THRESHOLD) * 100, 100.0);

            updateProgress(doctorId, "DOCTOR_EMPATHETIC_DOCTOR", progress);

            if (empathyCount >= EMPATHETIC_DOCTOR_THRESHOLD) {
                activateBadge(doctorId, "DOCTOR_EMPATHETIC_DOCTOR");
            } else {
                deactivateBadge(doctorId, "DOCTOR_EMPATHETIC_DOCTOR");
            }
        } catch (Exception e) {
            log.error("Error evaluating EMPATHETIC_DOCTOR for doctor {}", doctorId, e);
        }
    }

    private void evaluatePunctualityProfessional(User doctor) {
        UUID doctorId = doctor.getId();
        try {
            BadgeStatistics stats = getOrCreateStatistics(doctorId);
            JsonNode statistics = stats.getStatistics();

            int punctualityCount = statistics.path("total_punctuality_count").asInt(0);
            double progress = Math.min(((double) punctualityCount / PUNCTUALITY_PROFESSIONAL_THRESHOLD) * 100, 100.0);

            updateProgress(doctorId, "DOCTOR_PUNCTUALITY_PROFESSIONAL", progress);

            if (punctualityCount >= PUNCTUALITY_PROFESSIONAL_THRESHOLD) {
                activateBadge(doctorId, "DOCTOR_PUNCTUALITY_PROFESSIONAL");
            } else {
                deactivateBadge(doctorId, "DOCTOR_PUNCTUALITY_PROFESSIONAL");
            }
        } catch (Exception e) {
            log.error("Error evaluating PUNCTUALITY_PROFESSIONAL for doctor {}", doctorId, e);
        }
    }

    private void evaluateCompleteDocumenter(User doctor) {
        UUID doctorId = doctor.getId();
        try {
            BadgeStatistics stats = getOrCreateStatistics(doctorId);
            JsonNode statistics = stats.getStatistics();

            int totalDocumentedCount = statistics.path("documentation_count").asInt(0);

            double progress = Math.min(((double) totalDocumentedCount / COMPLETE_DOCUMENTER_THRESHOLD) * 100, 100.0);

            updateProgress(doctorId, "DOCTOR_COMPLETE_DOCUMENTER", progress);

            if (totalDocumentedCount >= COMPLETE_DOCUMENTER_THRESHOLD) {
                activateBadge(doctorId, "DOCTOR_COMPLETE_DOCUMENTER");
            } else {
                deactivateBadge(doctorId, "DOCTOR_COMPLETE_DOCUMENTER");
            }
        } catch (Exception e) {
            log.error("Error evaluating COMPLETE_DOCUMENTER for doctor {}", doctorId, e);
        }
    }

    void evaluateDetailedDiagnostician(User doctor) {
        UUID doctorId = doctor.getId();
        try {
            BadgeStatistics stats = getOrCreateStatistics(doctorId);
            JsonNode statistics = stats.getStatistics();

            int documentationCount = statistics.path("documentation_count").asInt(0);

            if (documentationCount < 60) {
                updateProgress(doctorId, "DOCTOR_DETAILED_DIAGNOSTICIAN", (double) documentationCount / 60 * 100);
                deactivateBadge(doctorId, "DOCTOR_DETAILED_DIAGNOSTICIAN");
            } else {
                updateProgress(doctorId, "DOCTOR_DETAILED_DIAGNOSTICIAN", 100.0);
                activateBadge(doctorId, "DOCTOR_DETAILED_DIAGNOSTICIAN");
            }
        } catch (Exception e) {
            log.error("Error evaluating DETAILED_DIAGNOSTICIAN for doctor {}", doctorId, e);
        }
    }

    void evaluateAgileResponder(User doctor) {
        UUID doctorId = doctor.getId();
        try {
            BadgeStatistics stats = getOrCreateStatistics(doctorId);
            JsonNode statistics = stats.getStatistics();

            int totalRequestsHandled = statistics.path("total_requests_handled").asInt(0);

            if (totalRequestsHandled < 8) {
                deactivateBadge(doctorId, "DOCTOR_AGILE_RESPONDER");
                updateProgress(doctorId, "DOCTOR_AGILE_RESPONDER", ((double) totalRequestsHandled / 8) * 100);
                return;
            }

            double progress = Math.min(((double) totalRequestsHandled / 8) * 100, 100.0);

            updateProgress(doctorId, "DOCTOR_AGILE_RESPONDER", progress);

            activateBadge(doctorId, "DOCTOR_AGILE_RESPONDER");
        } catch (Exception e) {
            log.error("Error evaluating AGILE_RESPONDER for doctor {}", doctorId, e);
        }
    }

    void evaluateRelationshipBuilder(User doctor) {
        UUID doctorId = doctor.getId();
        try {
            int totalUniquePatients = turnAssignedRepository.findDistinctPatientsByDoctorId(doctorId).size();

            boolean hasEnoughPatients = totalUniquePatients >= RELATIONSHIP_BUILDER_MIN_PATIENTS;

            double progress = 0.0;
            if (hasEnoughPatients) {
                progress = 100.0;
            } else {
                progress = ((double) totalUniquePatients / RELATIONSHIP_BUILDER_MIN_PATIENTS) * 100;
            }

            updateProgress(doctorId, "DOCTOR_RELATIONSHIP_BUILDER", progress);

            if (hasEnoughPatients) {
                activateBadge(doctorId, "DOCTOR_RELATIONSHIP_BUILDER");
            } else {
                deactivateBadge(doctorId, "DOCTOR_RELATIONSHIP_BUILDER");
            }
        } catch (Exception e) {
            log.error("Error evaluating RELATIONSHIP_BUILDER for doctor {}", doctorId, e);
        }
    }

    void evaluateConsistentProfessional(User doctor) {
        UUID doctorId = doctor.getId();
        try {
            BadgeStatistics stats = getOrCreateStatistics(doctorId);
            JsonNode statistics = stats.getStatistics();

            int totalTurnsCompleted = statistics.path("total_turns_completed").asInt(0);
            int totalCancellations = statistics.path("total_cancellations").asInt(0);

            if (totalTurnsCompleted < CONSISTENT_PROFESSIONAL_MIN_TURNS) {
                deactivateBadge(doctorId, "DOCTOR_CONSISTENT_PROFESSIONAL");
                updateProgress(doctorId, "DOCTOR_CONSISTENT_PROFESSIONAL", ((double) totalTurnsCompleted / CONSISTENT_PROFESSIONAL_MIN_TURNS) * 100);
                return;
            }

            double cancellationRate = totalTurnsCompleted > 0 ? (double) totalCancellations / totalTurnsCompleted : 0.0;
            double progress = (cancellationRate < CONSISTENT_PROFESSIONAL_CANCELLATION_MAX) ? 100.0 : 0.0;

            updateProgress(doctorId, "DOCTOR_CONSISTENT_PROFESSIONAL", progress);

            if (totalTurnsCompleted >= CONSISTENT_PROFESSIONAL_MIN_TURNS && cancellationRate < CONSISTENT_PROFESSIONAL_CANCELLATION_MAX) {
                activateBadge(doctorId, "DOCTOR_CONSISTENT_PROFESSIONAL");
            } else {
                deactivateBadge(doctorId, "DOCTOR_CONSISTENT_PROFESSIONAL");
            }
        } catch (Exception e) {
            log.error("Error evaluating CONSISTENT_PROFESSIONAL for doctor {}", doctorId, e);
        }
    }

    public void evaluateAlwaysAvailable(User doctor) {
        UUID doctorId = doctor.getId();
        log.debug("Starting evaluation of ALWAYS_AVAILABLE for doctor: {}", doctorId);

        try {
            Optional<DoctorProfile> profileOpt = doctorProfileRepository.findByUserId(doctorId);
            int availableDays = 0;

            if (profileOpt.isPresent() && profileOpt.get().getAvailabilitySchedule() != null && !profileOpt.get().getAvailabilitySchedule().isEmpty()) {
                JsonNode schedule = parseJson(profileOpt.get().getAvailabilitySchedule());
                if (schedule.isArray()) {
                    for (JsonNode dayEntry : schedule) {
                        if (dayEntry.has("enabled") && dayEntry.get("enabled").asBoolean()) {
                            availableDays++;
                        }
                    }
                }
            }

            double progress = Math.min((double) availableDays / 4 * 100, 100.0);

            updateProgress(doctorId, "DOCTOR_ALWAYS_AVAILABLE", progress);

            if (availableDays >= 4) {
                activateBadge(doctorId, "DOCTOR_ALWAYS_AVAILABLE");
            } else {
                deactivateBadge(doctorId, "DOCTOR_ALWAYS_AVAILABLE");
            }
        } catch (Exception e) {
            log.error("Error evaluating ALWAYS_AVAILABLE for doctor {}", doctorId, e);
        }
    }

    void evaluateTopSpecialist(User doctor) {
        UUID doctorId = doctor.getId();
        try {
            List<Rating> recentRatings = ratingRepository.findTop35ByRated_IdAndRater_RoleOrderByCreatedAtDesc(doctorId, "PATIENT");
            if (recentRatings == null) {
                recentRatings = Collections.emptyList();
            }

            long highScoreCount = recentRatings.stream()
                    .filter(rating -> rating.getScore() != null && rating.getScore() >= 4)
                    .count();

            double progress = Math.min((highScoreCount / (double) TOP_SPECIALIST_REQUIRED_RATINGS) * 100.0, 100.0);

            updateProgress(doctorId, "DOCTOR_TOP_SPECIALIST", progress);

            if (recentRatings.size() == TOP_SPECIALIST_REQUIRED_RATINGS && highScoreCount == TOP_SPECIALIST_REQUIRED_RATINGS) {
                activateBadge(doctorId, "DOCTOR_TOP_SPECIALIST");
            } else {
                deactivateBadge(doctorId, "DOCTOR_TOP_SPECIALIST");
            }
        } catch (Exception e) {
            log.error("Error evaluating TOP_SPECIALIST for doctor {}", doctorId, e);
        }
    }

    void evaluateMedicalLegend(User doctor) {
        UUID doctorId = doctor.getId();
        try {
            BadgeStatistics stats = getOrCreateStatistics(doctorId);

            long activeRequiredBadges = MEDICAL_LEGEND_REQUIRED_BADGES.stream()
                    .filter(badgeType -> badgeRepository.existsByUser_IdAndBadgeTypeAndIsActive(doctorId, badgeType, true))
                    .count();

            double progress = MEDICAL_LEGEND_REQUIRED_BADGES.isEmpty()
                    ? 0.0
                    : Math.min(activeRequiredBadges * 100.0 / MEDICAL_LEGEND_REQUIRED_BADGES.size(), 100.0);

            updateProgress(doctorId, "DOCTOR_MEDICAL_LEGEND", progress);

            if (activeRequiredBadges == MEDICAL_LEGEND_REQUIRED_BADGES.size()) {
                activateBadge(doctorId, "DOCTOR_MEDICAL_LEGEND");
            } else {
                deactivateBadge(doctorId, "DOCTOR_MEDICAL_LEGEND");
            }
        } catch (Exception e) {
            log.error("Error evaluating MEDICAL_LEGEND for doctor {}", doctorId, e);
        }
    }

    BadgeStatistics getOrCreateStatistics(UUID userId) {
        log.debug("Getting or creating statistics for userId: {}", userId);
        return statisticsRepository.findByUserId(userId).orElseGet(() -> {
            log.debug("Creating new statistics for userId: {}", userId);
            BadgeStatistics stats = BadgeStatistics.builder()
                    .userId(userId)
                    .statistics(objectMapper.createObjectNode())
                    .progress(objectMapper.createObjectNode())
                    .build();
            return statisticsRepository.save(stats);
        });
    }

    private void updateProgress(UUID userId, String badgeType, double progress) {
        log.debug("Updating progress for userId: {}, badgeType: {}, progress: {}", userId, badgeType, progress);
        try {
            BadgeStatistics stats = getOrCreateStatistics(userId);
            log.debug("Retrieved statistics for update: userId={}, current progress={}", userId, stats.getProgress());

            ObjectNode progressJson = (ObjectNode) stats.getProgress();
            progressJson.put(badgeType, progress);

            log.debug("Attempting to save updated statistics for userId: {}", userId);
            BadgeStatistics saved = statisticsRepository.save(stats);
            log.debug("Successfully updated progress for userId: {}, badgeType: {}", userId, badgeType);
        } catch (Exception e) {
            log.error("Error updating progress for user {} badge {}: {}", userId, badgeType, e.getMessage(), e);
        }
    }

    void createBadge(UUID userId, String badgeType) {
        Badge badge = Badge.builder()
                .userId(userId)
                .badgeType(badgeType)
                .build();
        badgeRepository.save(badge);
    }

    void activateBadge(UUID userId, String badgeType) {
        Optional<Badge> existing = badgeRepository.findByUser_IdAndBadgeType(userId, badgeType);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (existing.isPresent()) {
            Badge badge = existing.get();

            if (!badge.getIsActive()) {
                badge.setIsActive(true);
                if (badge.getEarnedAt() == null) {
                    badge.setEarnedAt(now);
                }
                badge.setLastEvaluatedAt(now);
                badgeRepository.save(badge);
            } else {
                badge.setLastEvaluatedAt(now);
                badgeRepository.save(badge);
            }
        } else {
            Badge newBadge = Badge.builder()
                    .userId(userId)
                    .badgeType(badgeType)
                    .earnedAt(now)
                    .isActive(true)
                    .lastEvaluatedAt(now)
                    .build();
            badgeRepository.save(newBadge);
        }
    }

    void deactivateBadge(UUID userId, String badgeType) {
        Optional<Badge> existing = badgeRepository.findByUser_IdAndBadgeType(userId, badgeType);

        if (existing.isPresent()) {
            Badge badge = existing.get();
            if (badge.getIsActive()) {
                badge.setIsActive(false);
                badge.setLastEvaluatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                badgeRepository.save(badge);
            } else {
                badge.setLastEvaluatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                badgeRepository.save(badge);
            }
        }
    }

    JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return objectMapper.createObjectNode();
        }
    }

    public BadgeDTO toBadgeDTO(Badge badge, String role) {
        return BadgeDTO.builder()
                .id(badge.getId())
                .badgeType(badge.getBadgeType())
                .category(getCategoryForBadge(badge.getBadgeType(), role))
                .earnedAt(badge.getEarnedAt())
                .isActive(badge.getIsActive())
                .lastEvaluatedAt(badge.getLastEvaluatedAt())
                .build();
    }

    BadgeCategory getCategoryForBadge(String badgeType, String role) {
        if ("PATIENT".equals(role)) {
            if (badgeType.contains("MEDIBOOK_WELCOME")) return BadgeCategory.WELCOME;
            if (badgeType.contains("COMMITTED_PATIENT") ||
                badgeType.contains("CONTINUOUS_FOLLOWUP") || badgeType.contains("CONSTANT_PATIENT")) return BadgeCategory.PREVENTIVE_CARE;
            if (badgeType.contains("EXEMPLARY_PUNCTUALITY") || badgeType.contains("SMART_PLANNER") ||
                badgeType.contains("EXCELLENT_COLLABORATOR")) return BadgeCategory.ACTIVE_COMMITMENT;
            return BadgeCategory.CLINICAL_EXCELLENCE;
        } else {
            if (badgeType.contains("EXCEPTIONAL_COMMUNICATOR") || badgeType.contains("EMPATHETIC_DOCTOR") ||
                badgeType.contains("PUNCTUALITY_PROFESSIONAL")) return BadgeCategory.QUALITY_OF_CARE;
            if (badgeType.contains("COMPLETE_DOCUMENTER") || badgeType.contains("DETAILED_DIAGNOSTICIAN") ||
                badgeType.contains("AGILE_RESPONDER") || badgeType.contains("RELATIONSHIP_BUILDER")) return BadgeCategory.PROFESSIONALISM;
            return BadgeCategory.CONSISTENCY;
        }
    }
}
