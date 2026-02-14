package com.medibook.api.service;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medibook.api.dto.Payment.PaymentRegisterRequestDTO;
import com.medibook.api.dto.Payment.PaymentRegisterResponseDTO;
import com.medibook.api.entity.PaymentRegister;
import com.medibook.api.entity.TurnAssigned;
import com.medibook.api.mapper.PaymentRegisterMapper;
import com.medibook.api.repository.PaymentRegisterRepository;
import com.medibook.api.repository.TurnAssignedRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentRegisterService {
   
    private static final java.util.Set<String> VALID_STATUSES = java.util.Set.of(
        "PENDING",
        "PAID",
        "HEALTH INSURANCE",
        "BONUS",
        "CANCELED"
    );

    private static final java.util.Set<String> VALID_METHODS = java.util.Set.of(
        "CASH",
        "CREDIT CARD",
        "DEBIT CARD",
        "ONLINE PAYMENT",
        "TRANSFER",
        "BONUS",
        "HEALTH INSURANCE"
    );
    private final PaymentRegisterRepository paymentRepo;
    private final TurnAssignedRepository turnRepo;
    private final PaymentRegisterMapper mapper;

    public PaymentRegisterResponseDTO createPaymentRegister(UUID turnId) {
        TurnAssigned turn = turnRepo.findById(turnId)
            .orElseThrow(() -> new RuntimeException("Turn not found"));

        if (paymentRepo.existsByTurnId(turnId) || turn.getPaymentRegister() != null) {
            throw new RuntimeException("Payment register already exists for this turn");
        }

        PaymentRegister saved = createPaymentRegisterForTurn(turn);
        return mapper.toDTO(saved);
    }

    public PaymentRegisterResponseDTO getPaymentRegister(UUID turnId) {
        PaymentRegister payment = paymentRepo.findByTurnId(turnId)
            .orElseThrow(() -> new RuntimeException("Payment register not found for this turn"));
        return mapper.toDTO(payment);
    }

    public PaymentRegisterResponseDTO updatePaymentRegister(UUID turnId,
            PaymentRegisterRequestDTO request,
            UUID actorId,
            String actorRole) {

        if (request == null) {
            throw new RuntimeException("Update payload is required");
        }

        TurnAssigned turn = turnRepo.findById(turnId)
            .orElseThrow(() -> new RuntimeException("Turn not found"));

        boolean isAdmin = "ADMIN".equals(actorRole);
        boolean isDoctorOwner = "DOCTOR".equals(actorRole)
            && turn.getDoctor() != null
            && actorId != null
            && actorId.equals(turn.getDoctor().getId());

        if (!isAdmin && !isDoctorOwner) {
            throw new RuntimeException("You are not allowed to update this payment register");
        }

        if (turn.getStatus() == null || !"COMPLETED".equals(turn.getStatus())) {
            throw new RuntimeException("Payment register can only be updated for completed turns");
        }
        
        PaymentRegister payment = paymentRepo.findByTurnId(turnId)
            .orElseThrow(() -> new RuntimeException("Payment register not found for this turn"));

        String requestedStatus = null;
        if (request.getPaymentStatus() != null) {
            requestedStatus = normalizeStatus(request.getPaymentStatus());
            if ("CANCELED".equals(requestedStatus)) {
                validateCancelRequestWithoutExtraFields(request);
                validateCancelableStatus(payment.getPaymentStatus());

                payment.setPaymentStatus("CANCELED");
                PaymentRegister saved = paymentRepo.save(payment);
                turn.setPaymentRegister(saved);

                return mapper.toDTO(saved);
            }
        }

        String targetStatus = payment.getPaymentStatus();
        if (request.getPaymentStatus() != null) {
            targetStatus = requestedStatus;
            payment.setPaymentStatus(targetStatus);
        }
        if (request.getPaymentAmount() != null) {
            payment.setPaymentAmount(request.getPaymentAmount());
        }
        String targetMethod = payment.getMethod();
        if (request.getMethod() != null) {
            targetMethod = normalizeMethod(request.getMethod());
        }

        if ("HEALTH INSURANCE".equals(targetStatus)) {
            if (request.getMethod() != null && !"HEALTH INSURANCE".equals(targetMethod)) {
                throw new RuntimeException("Payment method must be HEALTH INSURANCE when payment status is HEALTH INSURANCE");
            }
            targetMethod = "HEALTH INSURANCE";
        }

        if ("BONUS".equals(targetStatus)) {
            if (request.getMethod() != null && !"BONUS".equals(targetMethod)) {
                throw new RuntimeException("Payment method must be BONUS when payment status is BONUS");
            }
            targetMethod = "BONUS";
        }

        if ("HEALTH INSURANCE".equals(targetMethod) && !"HEALTH INSURANCE".equals(targetStatus)) {
            throw new RuntimeException("Payment status must be HEALTH INSURANCE when payment method is HEALTH INSURANCE");
        }

        if ("BONUS".equals(targetMethod) && !"BONUS".equals(targetStatus)) {
            throw new RuntimeException("Payment status must be BONUS when payment method is BONUS");
        }

        payment.setMethod(targetMethod);

        if (request.getPaidAt() != null) {
            payment.setPaidAt(request.getPaidAt().toInstant());
        } else {
            payment.setPaidAt(Instant.now());
        }
        

        if ("HEALTH INSURANCE".equals(targetStatus)) {
            Double targetPaymentAmount = request.getPaymentAmount() != null
                    ? request.getPaymentAmount()
                    : payment.getPaymentAmount();
            Double targetCopaymentAmount = request.getCopaymentAmount() != null
                    ? request.getCopaymentAmount()
                    : payment.getCopaymentAmount();

            validateCopaymentLessThanAmount(targetPaymentAmount, targetCopaymentAmount);

            if (request.getCopaymentAmount() != null) {
                payment.setCopaymentAmount(request.getCopaymentAmount());
            }
        } else {
            if (request.getCopaymentAmount() != null) {
                throw new RuntimeException("Copayment amount can only be set when payment status is HEALTH INSURANCE");
            }
            payment.setCopaymentAmount(null);
        }

        PaymentRegister saved = paymentRepo.save(payment);
        turn.setPaymentRegister(saved);

        return mapper.toDTO(saved);
    }

    public PaymentRegisterResponseDTO cancelPaymentRegister(UUID turnId,
            UUID actorId,
            String actorRole) {

        TurnAssigned turn = turnRepo.findById(turnId)
            .orElseThrow(() -> new RuntimeException("Turn not found"));

        boolean isAdmin = "ADMIN".equals(actorRole);
        boolean isDoctorOwner = "DOCTOR".equals(actorRole)
            && turn.getDoctor() != null
            && actorId != null
            && actorId.equals(turn.getDoctor().getId());

        if (!isAdmin && !isDoctorOwner) {
            throw new RuntimeException("You are not allowed to cancel this payment register");
        }

        if (turn.getStatus() == null || !"COMPLETED".equals(turn.getStatus())) {
            throw new RuntimeException("Payment register can only be canceled for completed turns");
        }

        PaymentRegister payment = paymentRepo.findByTurnId(turnId)
            .orElseThrow(() -> new RuntimeException("Payment register not found for this turn"));

        validateCancelableStatus(payment.getPaymentStatus());

        payment.setPaymentStatus("CANCELED");

        PaymentRegister saved = paymentRepo.save(payment);
        turn.setPaymentRegister(saved);

        return mapper.toDTO(saved);
    }

    public PaymentRegister createPaymentRegisterForTurn(TurnAssigned turn) {
        if (turn == null || turn.getId() == null) {
            throw new RuntimeException("Turn information is required to create a payment register");
        }

        UUID turnId = turn.getId();

        if (paymentRepo.existsByTurnId(turnId) || turn.getPaymentRegister() != null) {
            throw new RuntimeException("Payment register already exists for this turn");
        }

        PaymentRegister payment = PaymentRegister.builder()
            .turnId(turnId)
            .paymentStatus("PENDING")
            .paidAt(Instant.now())
            .build();

        PaymentRegister saved = paymentRepo.save(payment);
        saved.setTurn(turn);
        turn.setPaymentRegister(saved);

        return saved;
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            throw new RuntimeException("Payment status cannot be null");
        }

        String normalized = status.trim();
        if (normalized.isEmpty()) {
            throw new RuntimeException("Payment status cannot be empty");
        }

        String upperCaseStatus = normalized.toUpperCase(Locale.ROOT);
        if (!VALID_STATUSES.contains(upperCaseStatus)) {
            throw new RuntimeException("Invalid payment status");
        }

        return upperCaseStatus;
    }

    private String normalizeMethod(String method) {
        if (method == null) {
            throw new RuntimeException("Payment method cannot be null");
        }

        String normalized = method.trim();
        if (normalized.isEmpty()) {
            throw new RuntimeException("Payment method cannot be empty");
        }

        String upperCaseMethod = normalized.toUpperCase(Locale.ROOT);
        if (!VALID_METHODS.contains(upperCaseMethod)) {
            throw new RuntimeException("Invalid payment method");
        }

        return upperCaseMethod;
    }

    private void validateCancelableStatus(String currentStatus) {
        if (currentStatus == null || "PENDING".equals(currentStatus)) {
            throw new RuntimeException("Payment with status PENDING cannot be canceled");
        }
    }

    private void validateCancelRequestWithoutExtraFields(PaymentRegisterRequestDTO request) {
        if (request.getMethod() != null
                || request.getCopaymentAmount() != null
                || request.getPaymentAmount() != null
                || request.getPaidAt() != null) {
            throw new RuntimeException("When payment status is CANCELED, method/copayment/amount/paidAt cannot be sent");
        }
    }

    private void validateCopaymentLessThanAmount(Double paymentAmount, Double copaymentAmount) {
        if (copaymentAmount == null) {
            return;
        }

        if (paymentAmount == null) {
            throw new RuntimeException("Payment amount is required when copayment amount is set");
        }

        if (copaymentAmount >= paymentAmount) {
            throw new RuntimeException("Copayment amount must be less than payment amount");
        }
    }
}
