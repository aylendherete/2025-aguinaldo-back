package com.medibook.api.service;

import com.medibook.api.entity.User;
import com.medibook.api.repository.UserRepository;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticatedUserService authenticatedUserService;

    private User activeUser;
    private User inactiveUser;
    private UUID userId;
    private String validToken; 


    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        validToken = "valid.jwt.token";
        
        activeUser = new User();
        activeUser.setId(userId);
        activeUser.setEmail("test@example.com");
        activeUser.setRole("PATIENT");
        activeUser.setStatus("ACTIVE");

        inactiveUser = new User();
        inactiveUser.setId(userId);
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setRole("PATIENT");
        inactiveUser.setStatus("DISABLED");
    }

     @Test
    void validateAccessToken_ValidToken_ReturnsUser() {
        when(jwtService.extractUserId(validToken)).thenReturn(userId.toString());
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Optional<User> result = authenticatedUserService.validateAccessToken(validToken);

        assertTrue(result.isPresent());
        assertEquals(activeUser, result.get());
    }

    @Test
    void validateAccessToken_InvalidTokenFormat_ThrowsException() {        
        doThrow(new MalformedJwtException("Token invalido")).when(jwtService).validateTokenThrows("invalid-token");

        assertThrows(MalformedJwtException.class, () -> 
            authenticatedUserService.validateAccessToken("invalid-token")
        );
    }

    @Test
    void validateAccessToken_UserNotFound_ReturnsEmpty() {
        when(jwtService.extractUserId(validToken)).thenReturn(userId.toString());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Optional<User> result = authenticatedUserService.validateAccessToken(validToken);

        assertTrue(result.isEmpty());
    }

    @Test
    void getUserFromAuthorizationHeader_ValidHeader_ReturnsUser() {
        String authHeader = "Bearer " + validToken;
        
        when(jwtService.extractUserId(validToken)).thenReturn(userId.toString());
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Optional<User> result = authenticatedUserService.getUserFromAuthorizationHeader(authHeader);

        assertTrue(result.isPresent());
        assertEquals(activeUser, result.get());
    }

    @Test
    void getUserFromAuthorizationHeader_NoBearer_ReturnsEmpty() {
        Optional<User> result = authenticatedUserService.getUserFromAuthorizationHeader(validToken);
        assertTrue(result.isEmpty());
    }
}