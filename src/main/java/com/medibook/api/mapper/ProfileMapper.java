package com.medibook.api.mapper;

import com.medibook.api.dto.ProfileResponseDTO;
import com.medibook.api.dto.ProfileUpdateRequestDTO;
import com.medibook.api.entity.User;
import com.medibook.api.entity.DoctorProfile;
import org.springframework.stereotype.Component;

@Component
public class ProfileMapper {

    public ProfileResponseDTO toProfileResponse(User user) {
        ProfileResponseDTO dto = new ProfileResponseDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setSurname(user.getSurname());
        dto.setDni(user.getDni());
        dto.setPhone(user.getPhone());
        dto.setBirthdate(user.getBirthdate());
        dto.setGender(user.getGender());
        dto.setStatus(user.getStatus());
    dto.setScore(user.getScore());

        if ("DOCTOR".equalsIgnoreCase(user.getRole()) && user.getDoctorProfile() != null) {
            DoctorProfile profile = user.getDoctorProfile();
            dto.setMedicalLicense(profile.getMedicalLicense());
            dto.setSpecialty(profile.getSpecialty());
            dto.setSlotDurationMin(profile.getSlotDurationMin());
        }

        dto.setHealthInsurance(user.getHealthInsurance());
        dto.setHealthPlan(user.getHealthPlan());

        return dto;
    }

    public void updateUserFromRequest(User user, ProfileUpdateRequestDTO request) {
        if (request.email() != null && !request.email().isBlank()) {
            user.setEmail(request.email());
        }
        
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name());
        }
        
        if (request.surname() != null && !request.surname().isBlank()) {
            user.setSurname(request.surname());
        }
        
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        
        if (request.birthdate() != null) {
            user.setBirthdate(request.birthdate());
        }
        
        if (request.gender() != null) {
            user.setGender(request.gender());
        }

        if ("DOCTOR".equalsIgnoreCase(user.getRole()) && user.getDoctorProfile() != null) {
            DoctorProfile profile = user.getDoctorProfile();
            
            if (request.medicalLicense() != null && !request.medicalLicense().isBlank()) {
                profile.setMedicalLicense(request.medicalLicense());
            }
            
            if (request.specialty() != null && !request.specialty().isBlank()) {
                profile.setSpecialty(request.specialty());
            }
            
            if (request.slotDurationMin() != null) {
                profile.setSlotDurationMin(request.slotDurationMin());
            }
        }

        if (request.healthInsurance() != null && !request.healthInsurance().isBlank() && request.healthPlan() != null && !request.healthPlan().isBlank()) {
            user.setHealthInsurance(request.healthInsurance());
            user.setHealthPlan(request.healthPlan());
        }    
    }
}