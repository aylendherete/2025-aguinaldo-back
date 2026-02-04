package com.medibook.api.service;

import com.medibook.api.dto.Payment.PaymentRegisterResponseDTO;
import com.medibook.api.entity.PaymentRegister;
import com.medibook.api.entity.TurnAssigned;
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

    @BeforeEach
    void setUp() {
        turnId = UUID.randomUUID();
        turn = TurnAssigned.builder()
                .id(turnId)
                .status("SCHEDULED")
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
}
