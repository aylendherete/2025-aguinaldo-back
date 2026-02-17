package com.medibook.api.dto.Payment;


import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRegisterResponseDTO {
    private UUID id;
    private UUID turnId;
    private OffsetDateTime paidAt;
    private String paymentStatus;
    private Double paymentAmount;
    private String method;
    private Double copaymentAmount;
    
}
