package com.medibook.api.service;

import com.medibook.api.entity.User;
import com.medibook.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BadgeEvaluationTriggerService {

    private final BadgeService badgeService;
    private final BadgeStatisticsUpdateService statisticsUpdateService;
    private final UserRepository userRepository;

    @Async("badgeEvaluationTaskExecutor")
    public void evaluateAfterRating(UUID userId, Integer communicationScore, Integer empathyScore, Integer punctualityScore) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            
            if (user == null) {
                log.warn("Attempted badge evaluation for non-existent user: {}", userId);
                return;
            }
            
            if (!"DOCTOR".equals(user.getRole())) {
                log.warn("Attempted badge evaluation for non-doctor user: {} (role: {})", userId, user.getRole());
                return;
            }
            
            statisticsUpdateService.updateAfterRatingAddedSync(userId, communicationScore, empathyScore, punctualityScore);
            statisticsUpdateService.updateProgressAfterRatingSync(userId);
            
            badgeService.evaluateRatingRelatedBadges(userId);
            badgeService.evaluateTopSpecialist(user);

        } catch (Exception e) {
            log.error("[TRIGGER] Unexpected error evaluating rating-related badges for user {} after rating: {}", userId, e.getMessage(), e);
        }
    }

    @Async("badgeEvaluationTaskExecutor")
    public void evaluateAfterTurnCompletion(UUID userId, UUID otherUserId) {
        try {
            userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            
            statisticsUpdateService.updateAfterTurnCompletedSync(userId, otherUserId);
            statisticsUpdateService.updateProgressAfterTurnCompletionSync(userId);
            
            badgeService.evaluateTurnCompletionRelatedBadges(userId);

        } catch (Exception e) {
            log.error("[TRIGGER] Unexpected error evaluating turn completion badges for user {} after turn completion: {}", userId, e.getMessage(), e);
        }
    }

    @Async("badgeEvaluationTaskExecutor")
    public void evaluateAfterMedicalHistoryDocumented(UUID userId, String content) {
        try {
            User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            
            if (!"DOCTOR".equals(user.getRole())) {
                log.warn("Attempted badge evaluation for non-doctor user: {} (role: {})", userId, user.getRole());
                return;
            }
            
            statisticsUpdateService.updateAfterMedicalHistoryDocumentedSync(userId, content);
            statisticsUpdateService.updateProgressAfterMedicalHistorySync(userId);
            
            badgeService.evaluateDocumentationRelatedBadges(userId);

        } catch (Exception e) {
            log.error("[TRIGGER] Unexpected error evaluating documentation badges for user {} after medical history: {}", userId, e.getMessage(), e);
        }
    }

    @Async("badgeEvaluationTaskExecutor")
    public void evaluateAfterModifyRequestHandled(UUID userId) {
        try {
            User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            
            if (!"DOCTOR".equals(user.getRole())) {
                log.warn("Attempted badge evaluation for non-doctor user: {} (role: {})", userId, user.getRole());
                return;
            }
            
            statisticsUpdateService.updateAfterModifyRequestHandledSync(userId);
            statisticsUpdateService.updateProgressAfterModifyRequestSync(userId);
            
            badgeService.evaluateResponseRelatedBadges(userId);

        } catch (Exception e) {
            log.error("[TRIGGER] Unexpected error evaluating response badges for user {} after modify request: {}", userId, e.getMessage(), e);
        }
    }

    @Async("badgeEvaluationTaskExecutor")
    public void evaluateAfterTurnCancellation(UUID userId) {
        try {
            User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            
            if (!"DOCTOR".equals(user.getRole())) {
                log.warn("Attempted badge evaluation for non-doctor user: {} (role: {})", userId, user.getRole());
                return;
            }
            
            statisticsUpdateService.updateAfterTurnCancelledSync(userId);
            statisticsUpdateService.updateProgressAfterCancellationSync(userId);
            
            badgeService.evaluateConsistencyRelatedBadges(userId);

        } catch (Exception e) {
            log.error("[TRIGGER] Unexpected error evaluating consistency badges for user {} after cancellation: {}", userId, e.getMessage(), e);
        }
    }

    @Async("badgeEvaluationTaskExecutor")
    public void evaluateAfterTurnNoShow(UUID userId) {
        try {
            User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            
            if (!"DOCTOR".equals(user.getRole())) {
                log.warn("Attempted badge evaluation for non-doctor user: {} (role: {})", userId, user.getRole());
                return;
            }
            
            statisticsUpdateService.updateAfterTurnNoShowSync(userId);
            statisticsUpdateService.updateProgressAfterCancellationSync(userId);
            
            badgeService.evaluateConsistencyRelatedBadges(userId);

        } catch (Exception e) {
            log.error("[TRIGGER] Unexpected error evaluating consistency badges for user {} after no-show: {}", userId, e.getMessage(), e);
        }
    }

    @Async("badgeEvaluationTaskExecutor")
    public void evaluateAfterAdvanceBooking(UUID userId) {
        try {
            User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            
            if (!"PATIENT".equals(user.getRole())) {
                log.warn("Attempted badge evaluation for non-patient user: {} (role: {})", userId, user.getRole());
                return;
            }
            
            statisticsUpdateService.updateAfterAdvanceBookingSync(userId);
            statisticsUpdateService.updateProgressAfterBookingSync(userId);
            badgeService.evaluateBookingRelatedBadges(userId);

        } catch (Exception e) {
            log.error("[TRIGGER] Unexpected error evaluating advance booking badges for user {} after booking: {}", userId, e.getMessage(), e);
        }
    }

    @Async("badgeEvaluationTaskExecutor")
    public void evaluateAfterRatingGiven(UUID userId) {
        int maxRetries = 3;
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                
                if (!"PATIENT".equals(user.getRole())) {
                    log.warn("Attempted badge evaluation for non-patient user: {} (role: {})", userId, user.getRole());
                    return;
                }
                
                statisticsUpdateService.updateAfterRatingGivenSync(userId);
                statisticsUpdateService.updateProgressAfterRatingSync(userId);
                badgeService.evaluateRatingRelatedBadges(userId);
                return; // Success, exit
            } catch (ObjectOptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    log.error("[TRIGGER] Failed to evaluate rating given badges for user {} after {} attempts due to optimistic locking: {}", userId, maxRetries, e.getMessage());
                } else {
                    log.warn("[TRIGGER] Optimistic locking failure for user {} on attempt {}, retrying...", userId, attempt);
                    try {
                        Thread.sleep(100 * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("[TRIGGER] Interrupted while retrying for user {}", userId);
                        return;
                    }
                }
            } catch (Exception e) {
                log.error("[TRIGGER] Unexpected error evaluating rating given badges for user {} after rating given: {}", userId, e.getMessage(), e);
                return;
            }
        }
    }

    @Async("badgeEvaluationTaskExecutor")
    public void evaluateAfterRatingReceived(UUID userId) {
        int maxRetries = 3;
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                
                statisticsUpdateService.updateAfterRatingReceivedSync(userId);
                statisticsUpdateService.updateProgressAfterRatingSync(userId);
                badgeService.evaluateRatingRelatedBadges(userId);
                return; // Success, exit
            } catch (ObjectOptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    log.error("[TRIGGER] Failed to evaluate rating received badges for user {} after {} attempts due to optimistic locking: {}", userId, maxRetries, e.getMessage());
                } else {
                    log.warn("[TRIGGER] Optimistic locking failure for user {} on attempt {}, retrying...", userId, attempt);
                    try {
                        Thread.sleep(100 * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("[TRIGGER] Interrupted while retrying for user {}", userId);
                        return;
                    }
                }
            } catch (Exception e) {
                log.error("[TRIGGER] Unexpected error evaluating rating received badges for user {} after rating received: {}", userId, e.getMessage(), e);
                return;
            }
        }
    }

    @Async("badgeEvaluationTaskExecutor")
    public void evaluateAfterFileUploaded(UUID userId) {
        try {
            User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            
            if (!"PATIENT".equals(user.getRole())) {
                log.warn("Attempted badge evaluation for non-patient user: {} (role: {})", userId, user.getRole());
                return;
            }
            
            statisticsUpdateService.updateAfterFileUploadedSync(userId);
            statisticsUpdateService.updateProgressAfterFileUploadSync(userId);
            badgeService.evaluateFileRelatedBadges(userId);

        } catch (Exception e) {
            log.error("[TRIGGER] Unexpected error evaluating file upload badges for user {} after file upload: {}", userId, e.getMessage(), e);
        }
    }

    @Async("badgeEvaluationTaskExecutor")
    public void evaluateAfterAvailabilityConfigured(UUID doctorId) {
        try {
            User user = userRepository.findById(doctorId).orElseThrow(() -> new IllegalArgumentException("User not found: " + doctorId));
            
            if (!"DOCTOR".equals(user.getRole())) {
                log.warn("Attempted badge evaluation for non-doctor user: {} (role: {})", doctorId, user.getRole());
                return;
            }
            
            badgeService.evaluateAlwaysAvailable(user);

        } catch (Exception e) {
            log.error("[TRIGGER] Unexpected error evaluating ALWAYS_AVAILABLE badge for doctor {} after availability configuration: {}", doctorId, e.getMessage(), e);
        }
    }

    @Async("badgeEvaluationTaskExecutor")
    public void evaluateAllBadges(UUID userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            
            if (user == null) {
                log.warn("Attempted badge evaluation for non-existent user: {}", userId);
                return;
            }
            
            statisticsUpdateService.updateAllBadgeProgress(userId);
            badgeService.evaluateAllBadges(userId);
            
        } catch (Exception e) {
            log.error("[TRIGGER] Unexpected error evaluating all badges for user {}: {}", userId, e.getMessage(), e);
        }
    }
}
