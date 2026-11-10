package com.ars.collectionmanager.modules.auth;

import com.ars.collectionmanager.modules.auth.dtos.LoginRequest;
import com.ars.collectionmanager.modules.auth.dtos.LoginResponse;
import com.ars.collectionmanager.modules.auth.dtos.RefreshRequest;
import com.ars.collectionmanager.modules.user.UserRepository;
import com.ars.collectionmanager.modules.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Servico de autenticacao: login + refresh token.
 * Credenciais invalidas -> 401 generica (nao revela se o email existe).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginResponse login(LoginRequest dto) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(dto.email())
                .orElseThrow(this::unauthorized);

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw unauthorized();
        }

        return buildResponse(user);
    }

    public LoginResponse refresh(RefreshRequest dto) {
        String token = dto.refreshToken();

        if (!jwtService.isValid(token) || !jwtService.isRefreshToken(token)) {
            throw unauthorized();
        }

        String email = jwtService.extractEmail(token);
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(this::unauthorized);

        // Emite novos access + refresh (rotacao de refresh token)
        return buildResponse(user);
    }

    // ========== HELPERS ==========

    private LoginResponse buildResponse(User user) {
        return new LoginResponse(
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user)
        );
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas");
    }
}
