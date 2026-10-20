package com.ars.UserLoginRefreshListTask.modules.auth;

import com.ars.UserLoginRefreshListTask.modules.auth.dtos.LoginRequest;
import com.ars.UserLoginRefreshListTask.modules.auth.dtos.LoginResponse;
import com.ars.UserLoginRefreshListTask.modules.auth.dtos.RefreshRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de autenticacao.
 * POST /auth/login   -> 200 LoginResponse (name, email, role, accessToken, refreshToken)
 * POST /auth/refresh -> 200 LoginResponse (novos tokens a partir de um refresh token valido)
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
}
