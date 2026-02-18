package com.medibook.api.mapper;
import com.medibook.api.dto.Badge.BadgeDTO;
import com.medibook.api.service.BadgeService;
import com.medibook.api.dto.DoctorDTO;
import com.medibook.api.entity.Badge;
import com.medibook.api.entity.User;
import com.medibook.api.repository.BadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DoctorMapper {

    private final BadgeRepository badgeRepository;
    private final BadgeService badgeService;

    public DoctorDTO toDTO(User user) {
        if (user == null || user.getDoctorProfile() == null) {
            return null;
        }

        List<Badge> activeBadges = badgeRepository.findByUser_IdAndIsActiveTrue(user.getId());
        
        List<BadgeDTO> badgeDTOs = activeBadges.stream()
                .map(badge -> badgeService.toBadgeDTO(badge, "DOCTOR"))
                .collect(Collectors.toList());

        return DoctorDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .gender(user.getGender())
                .email(user.getEmail())
                .medicalLicense(user.getDoctorProfile().getMedicalLicense())
                .specialty(user.getDoctorProfile().getSpecialty())
                .slotDurationMin(user.getDoctorProfile().getSlotDurationMin())
                .score(user.getScore())
                .activeBadges(badgeDTOs)
                .totalActiveBadges(badgeDTOs.size())
                .build();
    }
}