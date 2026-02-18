package com.medibook.api.dto;

import com.medibook.api.dto.Badge.BadgeDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class DoctorDTO {
    private UUID id;
    private String name;
    private String surname;
    private String gender;
    private String email;
    private String medicalLicense;
    private String specialty;
    private int slotDurationMin;
    private Double score;
    private List<BadgeDTO> activeBadges;
    private Integer totalActiveBadges;
}
