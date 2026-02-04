package com.medibook.api.mapper;

import com.medibook.api.dto.Turn.TurnCreateRequestDTO;
import com.medibook.api.dto.Turn.TurnResponseDTO;
import com.medibook.api.entity.TurnAssigned;
import com.medibook.api.entity.TurnFile;
import com.medibook.api.entity.User;
import com.medibook.api.dto.Payment.PaymentRegisterResponseDTO;
import com.medibook.api.entity.PaymentRegister;
import com.medibook.api.repository.RatingRepository;
import com.medibook.api.service.TurnFileService;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TurnAssignedMapper {

    private final RatingRepository ratingRepository;
    private final TurnFileService turnFileService;
    private final PaymentRegisterMapper paymentRegisterMapper;

    public TurnAssigned toEntity(TurnCreateRequestDTO dto, User doctor) {
        return TurnAssigned.builder()
                .doctor(doctor)
                .motive(dto.getMotive())
                .scheduledAt(dto.getScheduledAt())
                .status("AVAILABLE")
                .build();
    }

    public TurnResponseDTO toDTO(TurnAssigned turn) {
        boolean isCompleted = "COMPLETED".equals(turn.getStatus());
        
        boolean needsPatientRating = false;
        boolean needsDoctorRating = false;
        
        if (turn.getPatient() != null && turn.getDoctor() != null) {
            
            if (isCompleted) {
                needsPatientRating = !ratingRepository.existsByTurnAssigned_IdAndRater_Id(
                    turn.getId(), turn.getPatient().getId());
            }
            
            if (isCompleted) {
                needsDoctorRating = !ratingRepository.existsByTurnAssigned_IdAndRater_Id(
                    turn.getId(), turn.getDoctor().getId());
            }
        }
        
        Optional<TurnFile> turnFile = turnFileService.getTurnFileInfo(turn.getId());
        PaymentRegister paymentRegister = turn.getPaymentRegister();
        PaymentRegisterResponseDTO paymentRegisterDTO = null;
        String paymentStatus = null;

        if (paymentRegister != null) {
            paymentRegisterDTO = paymentRegisterMapper.toDTO(paymentRegister);
            paymentStatus = paymentRegister.getPaymentStatus();
        }
        
        return TurnResponseDTO.builder()
                .id(turn.getId())
                .doctorId(turn.getDoctor().getId())
                .doctorName(turn.getDoctor().getName() + " " + turn.getDoctor().getSurname())
                .doctorSpecialty(turn.getDoctor().getDoctorProfile() != null ? 
                    turn.getDoctor().getDoctorProfile().getSpecialty() : null)
                .patientId(turn.getPatient() != null ? turn.getPatient().getId() : null)
                .patientName(turn.getPatient() != null ? turn.getPatient().getName() + " " + turn.getPatient().getSurname() : null)
                .patientScore(turn.getPatient() != null ? turn.getPatient().getScore() : null)
                .scheduledAt(turn.getScheduledAt())
                .motive(turn.getMotive())
                .status(turn.getStatus())
                .needsPatientRating(needsPatientRating)
                .needsDoctorRating(needsDoctorRating)
                .fileUrl(turnFile.map(TurnFile::getFileUrl).orElse(null))
                .fileName(turnFile.map(TurnFile::getFileName).orElse(null))
                .uploadedAt(turnFile.map(TurnFile::getUploadedAt).orElse(null))
                .paymentStatus(paymentStatus)
                .paymentRegister(paymentRegisterDTO)
                .build();
    }
}
