package com.medibook.api.dto.Payment;


import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRegisterRequestDTO {
    private UUID turnId;
    private OffsetDateTime payedAt;
    private String paymentStatus;
    private Double paymentAmount;
    private String method;
    private Double copaymentAmount;
    
}
