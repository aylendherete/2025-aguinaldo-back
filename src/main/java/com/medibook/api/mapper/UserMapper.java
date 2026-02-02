package com.medibook.api.mapper;

import com.medibook.api.dto.Auth.RegisterRequestDTO;
import com.medibook.api.dto.Auth.RegisterResponseDTO;
import com.medibook.api.entity.User;
import com.medibook.api.entity.DoctorProfile;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toUser(RegisterRequestDTO dto, String role, String passwordHash) {
        User user = new User();
        user.setEmail(dto.email());
        user.setDni(dto.dni());
        user.setPasswordHash(passwordHash);
        user.setName(dto.name());
        user.setSurname(dto.surname());
        user.setPhone(dto.phone());
        user.setBirthdate(dto.birthdate());
        user.setGender(dto.gender());
        user.setRole(role);
        user.setStatus("ACTIVE");

        if ("DOCTOR".equals(role)) {
            if (dto.slotDurationMin() == null) {
                throw new IllegalArgumentException("slotDurationMin is required for doctor registration");
            }
            DoctorProfile profile = new DoctorProfile();
            profile.setMedicalLicense(dto.medicalLicense());
            profile.setSpecialty(dto.specialty());
            profile.setSlotDurationMin(dto.slotDurationMin());
            user.setDoctorProfile(profile);
        }
        user.setHealthInsurance(dto.healthInsurance());
        user.setHealthPlan(dto.healthPlan());
        return user;
    }

    public RegisterResponseDTO toRegisterResponse(User user) {
        return new RegisterResponseDTO(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getSurname(),
            user.getRole(),
            user.getStatus()
        );
    }
}