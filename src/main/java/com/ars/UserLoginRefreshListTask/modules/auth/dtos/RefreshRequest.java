package com.ars.UserLoginRefreshListTask.modules.auth.dtos;

import jakarta.validation.constraints.NotBlank;

/**
 * Dados de entrada da renovacao do token.
 */
public record RefreshRequest(
        @NotBlank(message = "Refresh token e obrigatorio")
        String refreshToken
) {
}
