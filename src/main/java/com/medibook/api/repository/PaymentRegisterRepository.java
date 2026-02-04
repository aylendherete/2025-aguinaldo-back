package com.medibook.api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medibook.api.entity.PaymentRegister;

public interface PaymentRegisterRepository extends JpaRepository<PaymentRegister, UUID> {
    boolean existsByTurnId(UUID turnId);
}
