package com.medibook.api.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ProfileUpdateRequestDTO(
    @Email(message = "Invalid email format")
    @Size(max = 254, message = "Email must be less than 254 characters")
    String email,

    @Size(min = 1, max = 50, message = "Name must be between 1 and 50 characters")
    String name,

    @Size(min = 1, max = 50, message = "Surname must be between 1 and 50 characters") 
    String surname,

    @Size(max = 20, message = "Phone must be less than 20 characters")
    String phone,
    
    LocalDate birthdate,
    
    @Size(max = 10, message = "Gender must be less than 10 characters")
    String gender,

    @Size(max = 50, message = "Medical license must be less than 50 characters")
    String medicalLicense,
    
    @Size(max = 100, message = "Specialty must be less than 100 characters")
    String specialty,

    @Min(value = 1, message = "Slot duration must be positive")
    @Max(value = 180, message = "Slot duration must be at most 180 minutes")
    Integer slotDurationMin,


    @Size(max = 50, message = "Health insurance must be less than 50 characters")
    String healthInsurance,

    @Size(max = 50, message = "Health plan must be less than 50 characters")
    String healthPlan
) {}