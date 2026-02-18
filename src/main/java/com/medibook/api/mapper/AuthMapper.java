package com.medibook.api.mapper;

import com.medibook.api.dto.Auth.SignInResponseDTO;
import com.medibook.api.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public SignInResponseDTO toSignInResponse(User user, String accessToken, String refreshToken) {
        return new SignInResponseDTO(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getSurname(),
            user.getRole(),
            user.getStatus(),
            user.getGender(),
            accessToken,
            refreshToken
        );
    }

    public SignInResponseDTO toSignInResponse(User user) {
        return toSignInResponse(user, null, null);
    }
}