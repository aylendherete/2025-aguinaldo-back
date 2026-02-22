package com.medibook.api.dto.Turn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;
@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TurnModifyRequestResponseDTO {
    
    private UUID id;
    private UUID turnId;
    private UUID patientId;
    private UUID doctorId;
    private OffsetDateTime currentScheduledAt;
    private OffsetDateTime requestedScheduledAt;
    private String status;
}