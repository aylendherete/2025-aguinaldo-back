package com.medibook.api.dto.Auth;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record RegisterRequestDTO(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 254, message = "Email must be less than 254 characters")
    String email,

    @NotNull(message = "DNI is required")
    @Min(value = 1000000L, message = "DNI must be at least 7 digits")
    @Max(value = 999999999L, message = "DNI must be at most 9 digits")
    Long dni,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
             message = "Password must contain at least one lowercase letter, one uppercase letter, and one digit")
    String password,

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "Name can only contain letters and spaces")
    String name,

    @NotBlank(message = "Surname is required")
    @Size(min = 2, max = 50, message = "Surname must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "Surname can only contain letters and spaces")
    String surname,

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Phone must contain 8-15 digits, optionally starting with +")
    String phone,
    
    @NotNull(message = "Birthdate is required")
    @Past(message = "Birthdate must be in the past")
    LocalDate birthdate,
    
    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "^(MALE|FEMALE)$", message = "Gender must be MALE or FEMALE")
    String gender,

    @Size(max = 50, message = "Medical license must be less than 50 characters")
    String medicalLicense,
    
    @Size(max = 100, message = "Specialty must be less than 100 characters")
    String specialty,

    @Min(value = 15, message = "Slot duration must be at least 15 minutes")
    @Max(value = 180, message = "Slot duration must be at most 180 minutes")
    Integer slotDurationMin,

    @Size(max = 50, message = "Health insurance must be less than 50 characters")
    String healthInsurance,

    @Size(max = 50, message = "Health plan must be less than 50 characters")
    String healthPlan
) {}