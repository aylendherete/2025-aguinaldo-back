package com.medibook.api.dto.Turn;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.util.UUID;

import com.medibook.api.dto.Payment.PaymentRegisterResponseDTO;


@Data
@Builder
public class TurnResponseDTO {
    private UUID id;
    private UUID doctorId;
    private String doctorName;
    private String doctorSpecialty;
    private UUID patientId;     
    private String patientName;
    private Double patientScore;
    private OffsetDateTime scheduledAt;
    private String status;
    private Boolean needsPatientRating;
    private Boolean needsDoctorRating;
    private String motive;
    private String fileUrl;
    private String fileName;
    private Instant uploadedAt;
    private PaymentRegisterResponseDTO paymentRegister;
}
