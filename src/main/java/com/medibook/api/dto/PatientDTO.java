package com.medibook.api.dto;

import com.medibook.api.dto.Rating.SubcategoryCountDTO;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PatientDTO {
    private UUID id;
    private String name;
    private String surname;
    private String email;
    private Long dni;
    private String phone;
    private LocalDate birthdate;
    private String gender;
    private String status;
    private List<MedicalHistoryDTO> medicalHistories;
    private String medicalHistory;
    private Double score;
    private List<SubcategoryCountDTO> ratingSubcategories;
    private String healthInsurance;
    private String healthPlan;
}