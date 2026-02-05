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
        "BONUS"
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

        PaymentRegister payment = paymentRepo.findByTurnId(turnId)
            .orElseThrow(() -> new RuntimeException("Payment register not found for this turn"));

        String targetStatus = payment.getPaymentStatus();
        if (request.getPaymentStatus() != null) {
            targetStatus = normalizeStatus(request.getPaymentStatus());
            payment.setPaymentStatus(targetStatus);
        }
        if (request.getPaymentAmount() != null) {
            payment.setPaymentAmount(request.getPaymentAmount());
        }
        if (request.getMethod() != null) {
            payment.setMethod(normalizeMethod(request.getMethod()));
        }

        if (request.getPayedAt() != null) {
            payment.setLastUpdateStatus(request.getPayedAt().toInstant());
        } else {
            payment.setLastUpdateStatus(Instant.now());
        }

        if ("HEALTH INSURANCE".equals(targetStatus)) {
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
            .lastUpdateStatus(Instant.now())
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
}
