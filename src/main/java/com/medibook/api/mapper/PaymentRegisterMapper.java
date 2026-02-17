package com.medibook.api.mapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

import com.medibook.api.dto.Payment.PaymentRegisterResponseDTO;
import com.medibook.api.entity.PaymentRegister;

@Component
public class PaymentRegisterMapper {

    public PaymentRegisterResponseDTO toDTO(PaymentRegister payment) {
        PaymentRegisterResponseDTO dto = new PaymentRegisterResponseDTO();
        dto.setId(payment.getId());
        dto.setTurnId(payment.getTurnId());
        dto.setPaymentStatus(payment.getPaymentStatus());
        dto.setPaymentAmount(payment.getPaymentAmount());
        dto.setCopaymentAmount(payment.getCopaymentAmount());
        dto.setMethod(payment.getMethod());
        if (payment.getPaidAt() != null) {
            OffsetDateTime paidAt = OffsetDateTime.ofInstant(payment.getPaidAt(), ZoneOffset.UTC);
            dto.setPaidAt(paidAt);
        }
        return dto;
    }
}
