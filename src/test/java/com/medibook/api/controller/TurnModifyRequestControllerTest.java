package com.medibook.api.controller;

import com.medibook.api.dto.Turn.TurnModifyRequestDTO;
import com.medibook.api.dto.Turn.TurnModifyRequestResponseDTO;
import com.medibook.api.entity.User;
import com.medibook.api.service.TurnModifyRequestService;
import com.medibook.api.util.AuthorizationUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurnModifyRequestControllerTest {

    @Mock
    private TurnModifyRequestService turnModifyRequestService;
    
    @Mock
    private HttpServletRequest request;
    
    @InjectMocks
    private TurnModifyRequestController controller;
    
    private User patientUser;
    private User doctorUser;
    private TurnModifyRequestDTO requestDTO;
    private TurnModifyRequestResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        patientUser = new User();
        patientUser.setId(UUID.randomUUID());
        patientUser.setRole("PATIENT");
        
        doctorUser = new User();
        doctorUser.setId(UUID.randomUUID());
        doctorUser.setRole("DOCTOR");
        
        requestDTO = new TurnModifyRequestDTO();
        requestDTO.setTurnId(UUID.randomUUID());
        requestDTO.setNewScheduledAt(OffsetDateTime.now().plusDays(2));
        
        responseDTO = new TurnModifyRequestResponseDTO();
        responseDTO.setId(UUID.randomUUID());
        responseDTO.setStatus("PENDING");
        responseDTO.setCurrentScheduledAt(OffsetDateTime.now().plusDays(1));
    }

    @Test
    void createModifyRequest_WithPatientUser_ShouldCreateSuccessfully() {
        when(request.getAttribute("authenticatedUser")).thenReturn(patientUser);
        when(turnModifyRequestService.createModifyRequest(requestDTO, patientUser)).thenReturn(responseDTO);
        
        try (MockedStatic<AuthorizationUtil> authUtil = mockStatic(AuthorizationUtil.class)) {
            authUtil.when(() -> AuthorizationUtil.isPatient(patientUser)).thenReturn(true);
            
            ResponseEntity<Object> result = controller.createModifyRequest(requestDTO, request);
            
            assertEquals(HttpStatus.CREATED, result.getStatusCode());
            assertEquals(responseDTO, result.getBody());
            verify(turnModifyRequestService).createModifyRequest(requestDTO, patientUser);
        }
    }

    @Test
    void createModifyRequest_WithDoctorUser_ShouldReturnForbidden() {
        when(request.getAttribute("authenticatedUser")).thenReturn(doctorUser);
        
        try (MockedStatic<AuthorizationUtil> authUtil = mockStatic(AuthorizationUtil.class)) {
            authUtil.when(() -> AuthorizationUtil.isPatient(doctorUser)).thenReturn(false);
            
            ResponseEntity<Object> result = controller.createModifyRequest(requestDTO, request);
            
            assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) result.getBody();
            assertNotNull(body);
            assertEquals("Forbidden", body.get("error"));
            assertEquals("Only patients can request turn modifications", body.get("message"));
            verifyNoInteractions(turnModifyRequestService);
        }
    }

    @Test
    void createModifyRequest_WithIllegalArgumentException_ShouldReturnBadRequest() {
        when(request.getAttribute("authenticatedUser")).thenReturn(patientUser);
        when(turnModifyRequestService.createModifyRequest(requestDTO, patientUser))
                .thenThrow(new IllegalArgumentException("Turn not found"));
        
        try (MockedStatic<AuthorizationUtil> authUtil = mockStatic(AuthorizationUtil.class)) {
            authUtil.when(() -> AuthorizationUtil.isPatient(patientUser)).thenReturn(true);
            
            ResponseEntity<Object> result = controller.createModifyRequest(requestDTO, request);
            
            assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) result.getBody();
            assertNotNull(body);
            assertEquals("Bad Request", body.get("error"));
            assertEquals("Turn not found", body.get("message"));
        }
    }

    @Test
    void createModifyRequest_WithUnexpectedException_ShouldReturnInternalServerError() {
        when(request.getAttribute("authenticatedUser")).thenReturn(patientUser);
        when(turnModifyRequestService.createModifyRequest(requestDTO, patientUser))
                .thenThrow(new RuntimeException("Unexpected error"));
        
        try (MockedStatic<AuthorizationUtil> authUtil = mockStatic(AuthorizationUtil.class)) {
            authUtil.when(() -> AuthorizationUtil.isPatient(patientUser)).thenReturn(true);
            
            ResponseEntity<Object> result = controller.createModifyRequest(requestDTO, request);
            
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) result.getBody();
            assertNotNull(body);
            assertEquals("Internal Server Error", body.get("error"));
            assertEquals("An unexpected error occurred", body.get("message"));
        }
    }

    @Test
    void getMyRequests_WithPatientUser_ShouldReturnRequests() {
        List<TurnModifyRequestResponseDTO> requests = Arrays.asList(responseDTO);
        when(request.getAttribute("authenticatedUser")).thenReturn(patientUser);
        when(turnModifyRequestService.getPatientRequests(patientUser.getId())).thenReturn(requests);
        
        try (MockedStatic<AuthorizationUtil> authUtil = mockStatic(AuthorizationUtil.class)) {
            authUtil.when(() -> AuthorizationUtil.isPatient(patientUser)).thenReturn(true);
            
            ResponseEntity<List<TurnModifyRequestResponseDTO>> result = controller.getMyRequests(request);
            
            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals(requests, result.getBody());
            verify(turnModifyRequestService).getPatientRequests(patientUser.getId());
        }
    }

    @Test
    void getMyRequests_WithDoctorUser_ShouldReturnForbidden() {
        when(request.getAttribute("authenticatedUser")).thenReturn(doctorUser);
        
        try (MockedStatic<AuthorizationUtil> authUtil = mockStatic(AuthorizationUtil.class)) {
            authUtil.when(() -> AuthorizationUtil.isPatient(doctorUser)).thenReturn(false);
            
            ResponseEntity<List<TurnModifyRequestResponseDTO>> result = controller.getMyRequests(request);
            
            assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
            verifyNoInteractions(turnModifyRequestService);
        }
    }

    @Test
    void getPendingRequests_WithDoctorUser_ShouldReturnPendingRequests() {
  
        List<TurnModifyRequestResponseDTO> requests = Arrays.asList(responseDTO);
        when(request.getAttribute("authenticatedUser")).thenReturn(doctorUser);
        when(turnModifyRequestService.getDoctorPendingRequests(doctorUser.getId())).thenReturn(requests);
        
        try (MockedStatic<AuthorizationUtil> authUtil = mockStatic(AuthorizationUtil.class)) {
            authUtil.when(() -> AuthorizationUtil.isDoctor(doctorUser)).thenReturn(true);
            
            ResponseEntity<List<TurnModifyRequestResponseDTO>> result = controller.getPendingRequests(request);
            
            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals(requests, result.getBody());
            verify(turnModifyRequestService).getDoctorPendingRequests(doctorUser.getId());
        }
    }

    @Test
    void getPendingRequests_WithPatientUser_ShouldReturnForbidden() {
        when(request.getAttribute("authenticatedUser")).thenReturn(patientUser);
        
        try (MockedStatic<AuthorizationUtil> authUtil = mockStatic(AuthorizationUtil.class)) {
            authUtil.when(() -> AuthorizationUtil.isDoctor(patientUser)).thenReturn(false);
            
            ResponseEntity<List<TurnModifyRequestResponseDTO>> result = controller.getPendingRequests(request);
            
            assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
            verifyNoInteractions(turnModifyRequestService);
        }
    }

    @Test
    void approveRequest_ForPastTurn_ShouldReturnBadRequest() {
        UUID requestId = UUID.randomUUID();
        responseDTO.setCurrentScheduledAt(OffsetDateTime.now().minusDays(1)); // Simulate past turn
        when(request.getAttribute("authenticatedUser")).thenReturn(doctorUser);
        when(turnModifyRequestService.getRequestById(requestId)).thenReturn(responseDTO);
        
        try (MockedStatic<AuthorizationUtil> authUtil = mockStatic(AuthorizationUtil.class)) {
            authUtil.when(() -> AuthorizationUtil.isDoctor(doctorUser)).thenReturn(true);
            
            ResponseEntity<Object> result = controller.approveModifyRequest(requestId, request);
            
            assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) result.getBody();
            assertNotNull(body);
            assertEquals("Bad Request", body.get("error"));
            assertEquals("Cannot approve modification requests for past turns", body.get("message"));
        }
    }

    @Test
    void rejectRequest_ForPastTurn_ShouldReturnBadRequest() {
        UUID requestId = UUID.randomUUID();
        responseDTO.setCurrentScheduledAt(OffsetDateTime.now().minusDays(1)); // Simulate past turn
        when(request.getAttribute("authenticatedUser")).thenReturn(doctorUser);
        when(turnModifyRequestService.getRequestById(requestId)).thenReturn(responseDTO);
        
        try (MockedStatic<AuthorizationUtil> authUtil = mockStatic(AuthorizationUtil.class)) {
            authUtil.when(() -> AuthorizationUtil.isDoctor(doctorUser)).thenReturn(true);
            
            ResponseEntity<Object> result = controller.rejectModifyRequest(requestId, request);
            
            assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) result.getBody();
            assertNotNull(body);
            assertEquals("Bad Request", body.get("error"));
            assertEquals("Cannot reject modification requests for past turns", body.get("message"));
        }
    }


    @Test 
    void getModifyRequest_Filtered_WithoutPastTurns() {
        TurnModifyRequestResponseDTO pastRequest = new TurnModifyRequestResponseDTO();
        pastRequest.setId(UUID.randomUUID());
        pastRequest.setStatus("PENDING");
        pastRequest.setCurrentScheduledAt(OffsetDateTime.now().minusDays(1)); // Past turn
        
        List<TurnModifyRequestResponseDTO> requests = Arrays.asList(responseDTO, pastRequest);
        when(request.getAttribute("authenticatedUser")).thenReturn(doctorUser);
        when(turnModifyRequestService.getDoctorPendingRequests(doctorUser.getId())).thenReturn(requests);
        
        try (MockedStatic<AuthorizationUtil> authUtil = mockStatic(AuthorizationUtil.class)) {
            authUtil.when(() -> AuthorizationUtil.isDoctor(doctorUser)).thenReturn(true);
            
            ResponseEntity<List<TurnModifyRequestResponseDTO>> result = controller.getPendingRequests(request);
            
            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals(1, result.getBody().size());
            assertEquals(responseDTO, result.getBody().get(0));
            verify(turnModifyRequestService).getDoctorPendingRequests(doctorUser.getId());
        }
    }
       
}