package com.medibook.api.service;

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

        PaymentRegister payment = PaymentRegister.builder()
            .turnId(turnId)
            .paymentStatus("PENDING")
            .lastUpdateStatus(new java.util.Date().toInstant())
            .build();

        PaymentRegister saved = paymentRepo.save(payment);
        turn.setPaymentRegister(saved);
        turnRepo.save(turn);

        return mapper.toDTO(saved);
    }
}
