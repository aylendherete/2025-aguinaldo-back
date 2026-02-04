package com.medibook.api.service;

import com.medibook.api.dto.Payment.PaymentRegisterRequestDTO;
import com.medibook.api.dto.Payment.PaymentRegisterResponseDTO;
import com.medibook.api.entity.PaymentRegister;
import com.medibook.api.entity.TurnAssigned;
import com.medibook.api.entity.User;
import com.medibook.api.mapper.PaymentRegisterMapper;
import com.medibook.api.repository.PaymentRegisterRepository;
import com.medibook.api.repository.TurnAssignedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRegisterServiceTest {

    @Mock
    private PaymentRegisterRepository paymentRepo;

    @Mock
    private TurnAssignedRepository turnRepo;

    @Mock
    private PaymentRegisterMapper mapper;

    @InjectMocks
    private PaymentRegisterService paymentRegisterService;

    private UUID turnId;
    private TurnAssigned turn;
    private PaymentRegister savedPayment;
    private PaymentRegisterResponseDTO responseDTO;
    private User doctor;

    @BeforeEach
    void setUp() {
        turnId = UUID.randomUUID();
        doctor = new User();
        doctor.setId(UUID.randomUUID());
        doctor.setRole("DOCTOR");

        turn = TurnAssigned.builder()
                .id(turnId)
                .status("SCHEDULED")
                .doctor(doctor)
                .build();

        savedPayment = PaymentRegister.builder()
                .id(UUID.randomUUID())
                .turnId(turnId)
                .paymentStatus("PENDING")
                .lastUpdateStatus(Instant.now())
                .build();

        responseDTO = new PaymentRegisterResponseDTO();
        responseDTO.setId(savedPayment.getId());
        responseDTO.setTurnId(turnId);
        responseDTO.setPaymentStatus("PENDING");
    }

    @Test
    void createPaymentRegister_whenTurnExistsAndNoPayment_createsRegister() {
        when(turnRepo.findById(turnId)).thenReturn(Optional.of(turn));
        when(paymentRepo.existsByTurnId(turnId)).thenReturn(false);
        when(paymentRepo.save(any(PaymentRegister.class))).thenAnswer(invocation -> {
            PaymentRegister payment = invocation.getArgument(0);
            payment.setId(savedPayment.getId());
            payment.setLastUpdateStatus(savedPayment.getLastUpdateStatus());
            return payment;
        });
        when(mapper.toDTO(any(PaymentRegister.class))).thenReturn(responseDTO);

        PaymentRegisterResponseDTO result = paymentRegisterService.createPaymentRegister(turnId);

        assertNotNull(result);
        assertEquals(savedPayment.getId(), result.getId());
        assertEquals("PENDING", result.getPaymentStatus());
        assertEquals(turnId, result.getTurnId());
        assertNotNull(turn.getPaymentRegister());
        assertEquals(savedPayment.getId(), turn.getPaymentRegister().getId());

        ArgumentCaptor<PaymentRegister> paymentCaptor = ArgumentCaptor.forClass(PaymentRegister.class);
        verify(paymentRepo).save(paymentCaptor.capture());
        PaymentRegister capturedPayment = paymentCaptor.getValue();
        assertEquals(turnId, capturedPayment.getTurnId());
        assertEquals("PENDING", capturedPayment.getPaymentStatus());
        assertNotNull(capturedPayment.getLastUpdateStatus());

        ArgumentCaptor<PaymentRegister> mapperCaptor = ArgumentCaptor.forClass(PaymentRegister.class);
        verify(mapper).toDTO(mapperCaptor.capture());
        assertEquals(turnId, mapperCaptor.getValue().getTurnId());
    }

    @Test
    void createPaymentRegister_whenTurnNotFound_throwsException() {
        when(turnRepo.findById(turnId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                paymentRegisterService.createPaymentRegister(turnId));

        assertEquals("Turn not found", exception.getMessage());
        verify(paymentRepo, never()).save(any());
    }

    @Test
    void createPaymentRegister_whenRegisterAlreadyExists_throwsException() {
        when(turnRepo.findById(turnId)).thenReturn(Optional.of(turn));
        when(paymentRepo.existsByTurnId(turnId)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                paymentRegisterService.createPaymentRegister(turnId));

        assertEquals("Payment register already exists for this turn", exception.getMessage());
        verify(paymentRepo, never()).save(any());
    }

    @Test
    void createPaymentRegister_whenTurnAlreadyLinked_throwsException() {
        turn.setPaymentRegister(savedPayment);
        when(turnRepo.findById(turnId)).thenReturn(Optional.of(turn));
        when(paymentRepo.existsByTurnId(turnId)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                paymentRegisterService.createPaymentRegister(turnId));

        assertEquals("Payment register already exists for this turn", exception.getMessage());
        verify(paymentRepo, never()).save(any());
    }

    @Test
    void getPaymentRegister_whenExists_returnsDTO() {
        when(paymentRepo.findByTurnId(turnId)).thenReturn(Optional.of(savedPayment));
        when(mapper.toDTO(savedPayment)).thenReturn(responseDTO);

        PaymentRegisterResponseDTO result = paymentRegisterService.getPaymentRegister(turnId);

        assertNotNull(result);
        assertEquals(savedPayment.getId(), result.getId());
        verify(mapper).toDTO(savedPayment);
    }

    @Test
    void getPaymentRegister_whenMissing_throwsException() {
        when(paymentRepo.findByTurnId(turnId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                paymentRegisterService.getPaymentRegister(turnId));

        assertEquals("Payment register not found for this turn", exception.getMessage());
    }

    @Test
    void updatePaymentRegister_asDoctor_updatesFields() {
        PaymentRegisterRequestDTO requestDTO = new PaymentRegisterRequestDTO();
        requestDTO.setPaymentStatus("PAID");
        requestDTO.setPaymentAmount(150.0);
        requestDTO.setMethod("CREDIT CARD");
        requestDTO.setPayedAt(OffsetDateTime.now());

        when(turnRepo.findById(turnId)).thenReturn(Optional.of(turn));
        when(paymentRepo.findByTurnId(turnId)).thenReturn(Optional.of(savedPayment));
        when(paymentRepo.save(any(PaymentRegister.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDTO(any(PaymentRegister.class))).thenReturn(responseDTO);

        PaymentRegisterResponseDTO result = paymentRegisterService.updatePaymentRegister(
                turnId,
                requestDTO,
                doctor.getId(),
                doctor.getRole());

        assertNotNull(result);

        ArgumentCaptor<PaymentRegister> captor = ArgumentCaptor.forClass(PaymentRegister.class);
        verify(paymentRepo).save(captor.capture());
        PaymentRegister updated = captor.getValue();
        assertEquals("PAID", updated.getPaymentStatus());
        assertEquals(150.0, updated.getPaymentAmount());
        assertEquals("CREDIT CARD", updated.getMethod());
        assertEquals(requestDTO.getPayedAt().toInstant(), updated.getLastUpdateStatus());
        assertSame(updated, turn.getPaymentRegister());
        assertNull(updated.getCopaymentAmount());
    }

    @Test
    void updatePaymentRegister_whenRegisterMissing_throwsException() {
        PaymentRegisterRequestDTO requestDTO = new PaymentRegisterRequestDTO();
        when(turnRepo.findById(turnId)).thenReturn(Optional.of(turn));
        when(paymentRepo.findByTurnId(turnId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                paymentRegisterService.updatePaymentRegister(turnId, requestDTO, doctor.getId(), doctor.getRole()));

        assertEquals("Payment register not found for this turn", exception.getMessage());
        verify(paymentRepo, never()).save(any());
    }

    @Test
    void updatePaymentRegister_whenDoctorNotOwner_throwsException() {
        PaymentRegisterRequestDTO requestDTO = new PaymentRegisterRequestDTO();
        User anotherDoctor = new User();
        anotherDoctor.setId(UUID.randomUUID());
        anotherDoctor.setRole("DOCTOR");

        turn.setDoctor(anotherDoctor);

        when(turnRepo.findById(turnId)).thenReturn(Optional.of(turn));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                paymentRegisterService.updatePaymentRegister(turnId, requestDTO, doctor.getId(), doctor.getRole()));

        assertEquals("You are not allowed to update this payment register", exception.getMessage());
        verify(paymentRepo, never()).save(any());
    }

    @Test
    void updatePaymentRegister_withInvalidStatus_throwsException() {
        PaymentRegisterRequestDTO requestDTO = new PaymentRegisterRequestDTO();
        requestDTO.setPaymentStatus("INVALID");

        when(turnRepo.findById(turnId)).thenReturn(Optional.of(turn));
        when(paymentRepo.findByTurnId(turnId)).thenReturn(Optional.of(savedPayment));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                paymentRegisterService.updatePaymentRegister(turnId, requestDTO, doctor.getId(), doctor.getRole()));

        assertEquals("Invalid payment status", exception.getMessage());
        verify(paymentRepo, never()).save(any());
    }

    @Test
    void updatePaymentRegister_healthInsuranceAllowsCopayment() {
        PaymentRegisterRequestDTO requestDTO = new PaymentRegisterRequestDTO();
        requestDTO.setPaymentStatus("health insurance");
        requestDTO.setCopaymentAmount(45.0);

        when(turnRepo.findById(turnId)).thenReturn(Optional.of(turn));
        when(paymentRepo.findByTurnId(turnId)).thenReturn(Optional.of(savedPayment));
        when(paymentRepo.save(any(PaymentRegister.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDTO(any(PaymentRegister.class))).thenReturn(responseDTO);

        PaymentRegisterResponseDTO result = paymentRegisterService.updatePaymentRegister(
                turnId,
                requestDTO,
                doctor.getId(),
                doctor.getRole());

        assertNotNull(result);

        ArgumentCaptor<PaymentRegister> captor = ArgumentCaptor.forClass(PaymentRegister.class);
        verify(paymentRepo).save(captor.capture());
        PaymentRegister updated = captor.getValue();
        assertEquals("HEALTH INSURANCE", updated.getPaymentStatus());
        assertEquals(45.0, updated.getCopaymentAmount());
        assertSame(updated, turn.getPaymentRegister());
    }

    @Test
    void updatePaymentRegister_copaymentWithoutHealthInsurance_throwsException() {
        PaymentRegisterRequestDTO requestDTO = new PaymentRegisterRequestDTO();
        requestDTO.setCopaymentAmount(30.0);

        when(turnRepo.findById(turnId)).thenReturn(Optional.of(turn));
        when(paymentRepo.findByTurnId(turnId)).thenReturn(Optional.of(savedPayment));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                paymentRegisterService.updatePaymentRegister(turnId, requestDTO, doctor.getId(), doctor.getRole()));

        assertEquals("Copayment amount can only be set when payment status is HEALTH INSURANCE", exception.getMessage());
        verify(paymentRepo, never()).save(any());
    }

    @Test
    void updatePaymentRegister_withLowercaseMethod_normalizesAndPersists() {
        PaymentRegisterRequestDTO requestDTO = new PaymentRegisterRequestDTO();
        requestDTO.setMethod("online payment");

        when(turnRepo.findById(turnId)).thenReturn(Optional.of(turn));
        when(paymentRepo.findByTurnId(turnId)).thenReturn(Optional.of(savedPayment));
        when(paymentRepo.save(any(PaymentRegister.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDTO(any(PaymentRegister.class))).thenReturn(responseDTO);

        PaymentRegisterResponseDTO result = paymentRegisterService.updatePaymentRegister(
                turnId,
                requestDTO,
                doctor.getId(),
                doctor.getRole());

        assertNotNull(result);

        ArgumentCaptor<PaymentRegister> captor = ArgumentCaptor.forClass(PaymentRegister.class);
        verify(paymentRepo).save(captor.capture());
        assertEquals("ONLINE PAYMENT", captor.getValue().getMethod());
    }

    @Test
    void updatePaymentRegister_withInvalidMethod_throwsException() {
        PaymentRegisterRequestDTO requestDTO = new PaymentRegisterRequestDTO();
        requestDTO.setMethod("BITCOIN");

        when(turnRepo.findById(turnId)).thenReturn(Optional.of(turn));
        when(paymentRepo.findByTurnId(turnId)).thenReturn(Optional.of(savedPayment));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                paymentRegisterService.updatePaymentRegister(turnId, requestDTO, doctor.getId(), doctor.getRole()));

        assertEquals("Invalid payment method", exception.getMessage());
        verify(paymentRepo, never()).save(any());
    }
}
