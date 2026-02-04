package com.medibook.api.controller;

import java.util.Locale;
import java.util.UUID;

import com.medibook.api.dto.Payment.PaymentRegisterRequestDTO;
import com.medibook.api.dto.Payment.PaymentRegisterResponseDTO;
import com.medibook.api.entity.TurnAssigned;
import com.medibook.api.entity.User;
import com.medibook.api.repository.TurnAssignedRepository;
import com.medibook.api.service.PaymentRegisterService;
import com.medibook.api.util.AuthorizationUtil;
import com.medibook.api.util.ErrorResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentRegisterController {

    private final PaymentRegisterService paymentRegisterService;
    private final TurnAssignedRepository turnAssignedRepository;

    @GetMapping("/turn/{turnId}")
    public ResponseEntity<?> getPaymentRegister(
            @PathVariable UUID turnId,
            HttpServletRequest request) {

        User authenticatedUser = (User) request.getAttribute("authenticatedUser");

        var optionalTurn = turnAssignedRepository.findById(turnId);
        if (optionalTurn.isEmpty()) {
            log.warn("Payment lookup failed, turn {} not found", turnId);
            var error = ErrorResponseUtil.createNotFoundResponse("Turn not found", request.getRequestURI());
            return new ResponseEntity<>(error.getBody(), error.getStatusCode());
        }

        TurnAssigned turn = optionalTurn.get();
        boolean isAdmin = AuthorizationUtil.isAdmin(authenticatedUser);
        boolean isDoctorOwner = AuthorizationUtil.isDoctor(authenticatedUser)
                && turn.getDoctor() != null
                && turn.getDoctor().getId().equals(authenticatedUser.getId());
        boolean isPatientOwner = AuthorizationUtil.isPatient(authenticatedUser)
                && turn.getPatient() != null
                && turn.getPatient().getId().equals(authenticatedUser.getId());

        if (!isAdmin && !isDoctorOwner && !isPatientOwner) {
            log.warn("User {} attempted to access payment for turn {} without permission", authenticatedUser != null ? authenticatedUser.getId() : null, turnId);
            return AuthorizationUtil.createOwnershipAccessDeniedResponse(
                    "You do not have access to this payment information");
        }

        try {
            PaymentRegisterResponseDTO response = paymentRegisterService.getPaymentRegister(turnId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            log.warn("Payment register for turn {} not found: {}", turnId, ex.getMessage());
            var error = ErrorResponseUtil.createNotFoundResponse(ex.getMessage(), request.getRequestURI());
            return new ResponseEntity<>(error.getBody(), error.getStatusCode());
        }
    }

    @PatchMapping("/turn/{turnId}")
    public ResponseEntity<?> updatePaymentRegister(
            @PathVariable UUID turnId,
            @RequestBody PaymentRegisterRequestDTO paymentRequest,
            HttpServletRequest request) {

        User authenticatedUser = (User) request.getAttribute("authenticatedUser");

        if (authenticatedUser == null) {
            log.warn("Payment update rejected for turn {} due to missing authentication", turnId);
            var error = ErrorResponseUtil.createErrorResponse(
                    "UNAUTHORIZED",
                    "Authentication is required",
                    HttpStatus.UNAUTHORIZED,
                    request.getRequestURI());
            return new ResponseEntity<>(error.getBody(), error.getStatusCode());
        }

        boolean isAdmin = AuthorizationUtil.isAdmin(authenticatedUser);
        boolean isDoctor = AuthorizationUtil.isDoctor(authenticatedUser);

        if (!isAdmin && !isDoctor) {
            log.warn("User {} attempted to update payment for turn {} without doctor/admin role", authenticatedUser.getId(), turnId);
            return AuthorizationUtil.createDoctorAccessDeniedResponse(request.getRequestURI());
        }

        try {
            PaymentRegisterResponseDTO response = paymentRegisterService.updatePaymentRegister(
                    turnId,
                    paymentRequest,
                    authenticatedUser.getId(),
                    authenticatedUser.getRole());
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "Unable to update payment register";
            log.warn("Failed to update payment register for turn {}: {}", turnId, message);

            String lowerMessage = message.toLowerCase(Locale.ROOT);
            if (lowerMessage.contains("not found")) {
                var error = ErrorResponseUtil.createNotFoundResponse(message, request.getRequestURI());
                return new ResponseEntity<>(error.getBody(), error.getStatusCode());
            }
            if (lowerMessage.contains("not allowed")) {
                return AuthorizationUtil.createOwnershipAccessDeniedResponse(message);
            }
            var error = ErrorResponseUtil.createBadRequestResponse(message, request.getRequestURI());
            return new ResponseEntity<>(error.getBody(), error.getStatusCode());
        }
    }
}
