package com.medibook.api.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
