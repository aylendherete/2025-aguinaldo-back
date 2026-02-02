package com.medibook.api.service;

import com.medibook.api.dto.Auth.RegisterRequestDTO;
import com.medibook.api.dto.Auth.RegisterResponseDTO;
import com.medibook.api.entity.User;
import com.medibook.api.mapper.AuthMapper;
import com.medibook.api.mapper.UserMapper;
import com.medibook.api.repository.RefreshTokenRepository;
import com.medibook.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRegistrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuthMapper authMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void whenRegisterAdmin_thenSuccess() {
        RegisterRequestDTO request = new RegisterRequestDTO(
            "admin@example.com",
            87654321L,
            "password123",
            "Admin",
            "User",
            "+1234567890",
            LocalDate.of(1985, 1, 15),
            "MALE",
            null,
            null,
            null, null, null
        );

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(request.email());
        user.setName(request.name());
        user.setRole("ADMIN");

        RegisterResponseDTO expectedResponse = new RegisterResponseDTO(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getSurname(),
            user.getRole(),
            "ACTIVE"
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByDni(request.dni())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed_password");
        when(userMapper.toUser(request, "ADMIN", "hashed_password")).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toRegisterResponse(user)).thenReturn(expectedResponse);

        RegisterResponseDTO response = authService.registerAdmin(request);

        assertEquals(expectedResponse.id(), response.id());
        assertEquals(expectedResponse.email(), response.email());
        assertEquals(expectedResponse.name(), response.name());
        assertEquals(expectedResponse.surname(), response.surname());
        assertEquals(expectedResponse.role(), response.role());
        assertEquals("ADMIN", response.role());
    }
}