package com.medibook.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibook.api.dto.Availability.*;
import com.medibook.api.entity.DoctorProfile;
import com.medibook.api.entity.User;
import com.medibook.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorAvailabilityService {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final BadgeEvaluationTriggerService badgeEvaluationTriggerService;

    @Transactional
    public void saveAvailability(UUID doctorId, DoctorAvailabilityRequestDTO request) {
        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        if (doctor.getDoctorProfile() == null) {
            throw new RuntimeException("Doctor profile not found");
        }

        DoctorProfile profile = doctor.getDoctorProfile();
    int slotDurationMin = (profile.getSlotDurationMin() != null && profile.getSlotDurationMin() > 0)
        ? profile.getSlotDurationMin()
        : 30;

    validateAvailabilityRanges(request, slotDurationMin);
        
        
        try {
            String scheduleJson = objectMapper.writeValueAsString(request.getWeeklyAvailability());
            profile.setAvailabilitySchedule(scheduleJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing availability schedule", e);
        }

        userRepository.save(doctor);
        
        badgeEvaluationTriggerService.evaluateAfterAvailabilityConfigured(doctorId);
    }

    private void validateAvailabilityRanges(DoctorAvailabilityRequestDTO request, int slotDurationMin) {
        if (request == null || request.getWeeklyAvailability() == null) {
            return;
        }

        for (DayAvailabilityDTO dayAvailability : request.getWeeklyAvailability()) {
            if (dayAvailability == null || !Boolean.TRUE.equals(dayAvailability.getEnabled()) || dayAvailability.getRanges() == null) {
                continue;
            }

            List<TimeRangeDTO> ranges = new ArrayList<>(dayAvailability.getRanges());
            ranges.sort((a, b) -> LocalTime.parse(a.getStart()).compareTo(LocalTime.parse(b.getStart())));

            LocalTime previousEnd = null;
            for (TimeRangeDTO range : ranges) {
                LocalTime start = LocalTime.parse(range.getStart());
                LocalTime end = LocalTime.parse(range.getEnd());

                if (!start.isBefore(end)) {
                    throw new IllegalArgumentException("Invalid time range for " + dayAvailability.getDay() + ": start must be before end");
                }

                int startMinutes = start.getHour() * 60 + start.getMinute();
                int endMinutes = end.getHour() * 60 + end.getMinute();
                int duration = endMinutes - startMinutes;

                if (startMinutes % slotDurationMin != 0 || endMinutes % slotDurationMin != 0 || duration % slotDurationMin != 0) {
                    throw new IllegalArgumentException("Time ranges must be multiples of slot duration (" + slotDurationMin + " min) for " + dayAvailability.getDay());
                }

                if (previousEnd != null && start.isBefore(previousEnd)) {
                    throw new IllegalArgumentException("Overlapping time ranges are not allowed for " + dayAvailability.getDay());
                }

                previousEnd = end;
            }
        }
    }

    @Transactional(readOnly = true)
    public DoctorAvailabilityResponseDTO getAvailability(UUID doctorId) {
        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        if (doctor.getDoctorProfile() == null) {
            throw new RuntimeException("Doctor profile not found");
        }

        DoctorProfile profile = doctor.getDoctorProfile();
        
        DoctorAvailabilityResponseDTO response = new DoctorAvailabilityResponseDTO();
        response.setSlotDurationMin(profile.getSlotDurationMin());

        
        if (profile.getAvailabilitySchedule() != null) {
            try {
                List<DayAvailabilityDTO> weeklyAvailability = objectMapper.readValue(
                        profile.getAvailabilitySchedule(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, DayAvailabilityDTO.class)
                );
                response.setWeeklyAvailability(weeklyAvailability);
            } catch (JsonProcessingException e) {
                log.error("Error deserializing availability schedule for doctor: {}", doctorId, e);
                response.setWeeklyAvailability(new ArrayList<>());
            }
        } else {
            response.setWeeklyAvailability(new ArrayList<>());
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<AvailableSlotDTO> getAvailableSlots(UUID doctorId, LocalDate fromDate, LocalDate toDate) {
        DoctorAvailabilityResponseDTO availability = getAvailability(doctorId);
        
        List<AvailableSlotDTO> slots = new ArrayList<>();

        if (availability.getWeeklyAvailability() == null || availability.getWeeklyAvailability().isEmpty()) {
            return slots; 
        }

        
        for (LocalDate currentDate = fromDate; !currentDate.isAfter(toDate); currentDate = currentDate.plusDays(1)) {
            final LocalDate finalCurrentDate = currentDate;
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            String dayName = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase();

            
            availability.getWeeklyAvailability().stream()
                    .filter(day -> day.getDay().equals(dayName) && day.getEnabled())
                    .forEach(day -> {
                        if (day.getRanges() != null) {
                            day.getRanges().forEach(range -> {
                                List<AvailableSlotDTO> dailySlots = generateSlotsForRange(
                                        finalCurrentDate, range, availability.getSlotDurationMin()
                                );
                                slots.addAll(dailySlots);
                            });
                        }
                    });
        }

        return slots;
    }

    private List<AvailableSlotDTO> generateSlotsForRange(LocalDate date, TimeRangeDTO range, int slotDurationMin) {
        List<AvailableSlotDTO> slots = new ArrayList<>();
        
        LocalTime startTime = LocalTime.parse(range.getStart());
        LocalTime endTime = LocalTime.parse(range.getEnd());
        
        LocalTime currentSlotStart = startTime;
        while (currentSlotStart.plusMinutes(slotDurationMin).isBefore(endTime) || 
               currentSlotStart.plusMinutes(slotDurationMin).equals(endTime)) {
            
            LocalTime currentSlotEnd = currentSlotStart.plusMinutes(slotDurationMin);
            
            AvailableSlotDTO slot = new AvailableSlotDTO();
            slot.setDate(date);
            slot.setStartTime(currentSlotStart);
            slot.setEndTime(currentSlotEnd);
            slot.setDayOfWeek(date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
            
            slots.add(slot);
            currentSlotStart = currentSlotEnd;
        }
        
        return slots;
    }
}