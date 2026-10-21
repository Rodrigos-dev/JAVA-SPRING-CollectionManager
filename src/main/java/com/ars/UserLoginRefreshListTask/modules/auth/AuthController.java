package com.ars.UserLoginRefreshListTask.modules.auth;

import com.ars.UserLoginRefreshListTask.core.GlobalExceptionHandler;
import com.ars.UserLoginRefreshListTask.modules.auth.dtos.LoginRequest;
import com.ars.UserLoginRefreshListTask.modules.auth.dtos.LoginResponse;
import com.ars.UserLoginRefreshListTask.modules.auth.dtos.RefreshRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Auth", description = "Autenticacao JWT - login e renovacao de tokens")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login", description = "Autentica por email + senha e retorna access token (1h) e refresh token (30h)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado - tokens emitidos"),
            @ApiResponse(responseCode = "400", description = "Corpo invalido ou campos de validacao falhando",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais invalidas",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Refresh token", description = "Emite novos tokens a partir de um refresh token valido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Novos tokens emitidos"),
            @ApiResponse(responseCode = "400", description = "Corpo invalido ou campos de validacao falhando",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token invalido ou expirado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
}
